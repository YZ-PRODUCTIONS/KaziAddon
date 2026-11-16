package net.kazi.kazimod.client.nagi;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

/** Textured, articulated runtime exports of the eight editable Blockbench summon models. */
public final class DragonSummonModels {
    private static final int FULL_BRIGHT = 15728880;
    private static final float MODEL_OPACITY = .55F;
    private static final Map<DragonKind, DragonSummonModels> MODELS = new EnumMap<>(DragonKind.class);
    private static final Map<DragonKind, ResourceLocation> TEXTURES = new EnumMap<>(DragonKind.class);
    private static IResourceManager registeredManager;

    private final CerberusRig rig;
    private final Map<String, CerberusRig.Pose> pose;
    private final CerberusRig.Pose[] animatedBones;
    private final Bone[] roots;
    private final RenderType material;
    private final float biteLength;

    private DragonSummonModels(DragonKind kind, CerberusRig rig) {
        validateRig(rig);
        this.rig = rig;
        pose = rig.pose();
        roots = bake(rig.nodes, false, false);
        animatedBones = pose.values().toArray(new CerberusRig.Pose[0]);
        material = RenderType.entityTranslucent(texture(kind));
        biteLength = rig.clip("bite").length;
    }

    /** Validate everything draw/sample indexes before accepting an external resource override. */
    private static void validateRig(CerberusRig rig) {
        if (rig == null || rig.nodes == null || rig.nodes.length == 0 || rig.animations == null)
            throw new IllegalArgumentException("Incomplete dragon rig");
        Set<String> bones = new HashSet<>();
        validateNodes(rig.nodes, bones, new int[2], 0);
        Set<String> clips = new HashSet<>();
        for (CerberusRig.Clip clip : rig.animations) {
            if (clip == null || clip.name == null || !clips.add(clip.name)
                    || !Float.isFinite(clip.length) || clip.length <= 0 || clip.length > 3600
                    || clip.tracks == null || clip.tracks.length > 2048)
                throw new IllegalArgumentException("Invalid dragon animation");
            for (CerberusRig.Track track : clip.tracks) {
                if (track == null || !bones.contains(track.bone) || track.keys == null
                        || track.keys.length > 4096)
                    throw new IllegalArgumentException("Invalid dragon animation track: " + clip.name);
                for (CerberusRig.Key key : track.keys) {
                    if (key == null || !("rotation".equals(key.channel) || "position".equals(key.channel))
                            || !Float.isFinite(key.time) || key.time < 0 || key.time > clip.length + .0001F)
                        throw new IllegalArgumentException("Invalid dragon animation key: " + clip.name);
                    validateVector(key.value, "rotation".equals(key.channel) ? 360000 : 16384);
                }
            }
        }
        if (!clips.contains("swim") || !clips.contains("bite")
                || !rig.clip("swim").loop || rig.clip("bite").loop)
            throw new IllegalArgumentException("Dragon rig needs looping swim and non-looping bite animations");
    }

    private static void validateNodes(CerberusRig.Node[] nodes, Set<String> bones, int[] budget, int depth) {
        if (depth > 64) throw new IllegalArgumentException("Dragon hierarchy exceeds supported depth");
        for (CerberusRig.Node node : nodes) {
            if (++budget[0] > 50000 || node == null || node.name == null || node.name.isEmpty())
                throw new IllegalArgumentException("Invalid dragon model node");
            if (node.children != null) {
                if (!bones.add(node.name) || node.quads != null)
                    throw new IllegalArgumentException("Ambiguous dragon bone: " + node.name);
                validateVector(node.origin, 16384);
                validateVector(node.rotation, 360000);
                validateNodes(node.children, bones, budget, depth + 1);
            } else {
                if (node.quads == null) throw new IllegalArgumentException("Missing dragon mesh: " + node.name);
                for (CerberusRig.Quad quad : node.quads) {
                    if (++budget[1] > 100000 || quad == null || quad.vertices == null || quad.vertices.length != 4)
                        throw new IllegalArgumentException("Invalid dragon mesh quad: " + node.name);
                    validateVector(quad.normal, 1.001F);
                    float lengthSquared = quad.normal[0] * quad.normal[0] + quad.normal[1] * quad.normal[1]
                            + quad.normal[2] * quad.normal[2];
                    if (Math.abs(lengthSquared - 1) > .02F)
                        throw new IllegalArgumentException("Dragon mesh requires unit normals: " + node.name);
                    for (float[] vertex : quad.vertices) {
                        if (vertex == null || vertex.length != 5)
                            throw new IllegalArgumentException("Invalid dragon vertex: " + node.name);
                        for (int axis = 0; axis < 3; axis++) {
                            if (!Float.isFinite(vertex[axis]) || Math.abs(vertex[axis]) > 16384)
                                throw new IllegalArgumentException("Invalid dragon vertex position: " + node.name);
                        }
                        for (int axis = 3; axis < 5; axis++) {
                            if (!Float.isFinite(vertex[axis]) || vertex[axis] < -.00001F || vertex[axis] > 1.00001F)
                                throw new IllegalArgumentException("Invalid dragon vertex UV: " + node.name);
                        }
                    }
                }
            }
        }
    }

    private static void validateVector(float[] vector, float limit) {
        if (vector == null || vector.length != 3) throw new IllegalArgumentException("Expected a three-axis dragon vector");
        for (float value : vector) {
            if (!Float.isFinite(value) || Math.abs(value) > limit)
                throw new IllegalArgumentException("Non-finite or out-of-bounds dragon vector");
        }
    }

    public static ResourceLocation texture(DragonKind kind) {
        return TEXTURES.computeIfAbsent(kind,
                key -> new ResourceLocation("kazimod", "textures/models/nagi/dragons/" + key.id + ".png"));
    }

    /** Register once on the client, so edited assets and resource packs also respond to F3+T. */
    public static void installReloadListener() {
        IResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager == registeredManager) return;
        registeredManager = manager;
        MODELS.clear();
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).registerReloadListener(
                    (IResourceManagerReloadListener) resources -> MODELS.clear());
        }
    }

    private static DragonSummonModels load(DragonKind kind) {
        ResourceLocation location = new ResourceLocation("kazimod", "models/nagi/dragons/" + kind.id + ".json");
        try (IResource resource = Minecraft.getInstance().getResourceManager().getResource(location);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return new DragonSummonModels(kind, CerberusRig.read(reader));
        } catch (IOException | RuntimeException error) {
            // A malformed resource-pack override must not disconnect a player who sees a summon.
            // The bundled editable export remains the authoritative fallback.
            try (InputStream stream = DragonSummonModels.class.getResourceAsStream(
                    "/assets/kazimod/" + location.getPath())) {
                if (stream == null) throw new IOException("Missing bundled model: " + location);
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    DragonSummonModels fallback = new DragonSummonModels(kind, CerberusRig.read(reader));
                    KaziMod.LOGGER.warn("Using bundled dragon model after resource override failed: {}", location, error);
                    return fallback;
                }
            } catch (IOException | RuntimeException fallbackError) {
                // Store the failed result too: no repeated disk access or log spam every frame.
                KaziMod.LOGGER.error("Unable to load dragon model {}", location, fallbackError);
                return null;
            }
        }
    }

    public static void render(DragonKind kind, MatrixStack stack, IRenderTypeBuffer buffers,
                              int light, float ageTicks, float attack, float phaseSeconds,
                              boolean distant) {
        render(kind, stack, buffers, light, ageTicks, attack, phaseSeconds, distant, 1);
    }

    /** A cinematic summon may materialize gradually without changing the live dragon's material. */
    public static void render(DragonKind kind, MatrixStack stack, IRenderTypeBuffer buffers,
                              int light, float ageTicks, float attack, float phaseSeconds,
                              boolean distant, float opacity) {
        if (!Float.isFinite(opacity) || opacity <= 0) return;
        opacity = MathHelper.clamp(opacity, 0, 1);
        if (!MODELS.containsKey(kind)) MODELS.put(kind, load(kind));
        DragonSummonModels model = MODELS.get(kind);
        if (model == null) return;
        // The render thread draws summons serially. Reuse the small pose arrays instead of
        // allocating dozens of maps/arrays every frame for every summoned dragon.
        for (CerberusRig.Pose value : model.animatedBones) {
            Arrays.fill(value.position, 0);
            Arrays.fill(value.rotation, 0);
        }
        model.rig.apply(model.pose, "swim", ageTicks / 20F + phaseSeconds, 1);
        if (attack > 0) {
            model.rig.apply(model.pose, "bite", (1 - MathHelper.clamp(attack, 0, 1)) * model.biteLength, 1);
        }
        stack.pushPose();
        try {
            // Blockbench export coordinates are +Y up and sixteen model units per block.
            stack.scale(1 / 16F, 1 / 16F, 1 / 16F);
            model.draw(model.roots, stack, buffers.getBuffer(model.material), light, distant, opacity);
        } finally {
            stack.popPose();
        }
    }

    /** Resolve animation poses and detail/emission flags once, not once per face per frame. */
    private Bone[] bake(CerberusRig.Node[] nodes, boolean detail, boolean emissive) {
        Bone[] result = new Bone[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            CerberusRig.Node node = nodes[i];
            boolean isDetail = detail || node.name.startsWith("detail_");
            boolean isEmissive = emissive || node.name.startsWith("glow_");
            CerberusRig.Pose animation = node.children == null ? null
                    : pose.computeIfAbsent(node.name, ignored -> new CerberusRig.Pose());
            result[i] = new Bone(node, animation, isDetail, isEmissive,
                    node.children == null ? null : bake(node.children, isDetail, isEmissive));
        }
        return result;
    }

    private void draw(Bone[] bones, MatrixStack stack, IVertexBuilder buffer, int light, boolean distant, float opacity) {
        for (Bone bone : bones) {
            if (!bone.node.visible || distant && bone.detail) continue;
            if (bone.children != null) {
                stack.pushPose();
                try {
                    transform(bone, stack);
                    draw(bone.children, stack, buffer, light, distant, opacity);
                } finally {
                    stack.popPose();
                }
            } else if (bone.node.quads != null) {
                Matrix4f transform = stack.last().pose();
                Matrix3f normal = stack.last().normal();
                int brightness = bone.emissive ? FULL_BRIGHT : light;
                for (CerberusRig.Quad quad : bone.node.quads) {
                    for (float[] vertex : quad.vertices) {
                        buffer.vertex(transform, vertex[0], vertex[1], vertex[2]).color(1F, 1F, 1F, MODEL_OPACITY * opacity)
                                .uv(vertex[3], vertex[4]).overlayCoords(OverlayTexture.NO_OVERLAY)
                                .uv2(brightness).normal(normal, quad.normal[0], quad.normal[1], quad.normal[2])
                                .endVertex();
                    }
                }
            }
        }
    }

    private static void transform(Bone bone, MatrixStack stack) {
        float[] origin = bone.node.origin, rotation = bone.node.rotation;
        CerberusRig.Pose animated = bone.pose;
        float x = rotation[0] + animated.rotation[0], y = rotation[1] + animated.rotation[1],
                z = rotation[2] + animated.rotation[2];
        if (x == 0 && y == 0 && z == 0) {
            if (animated.position[0] != 0 || animated.position[1] != 0 || animated.position[2] != 0)
                stack.translate(animated.position[0], animated.position[1], animated.position[2]);
            return;
        }
        stack.translate(origin[0] + animated.position[0], origin[1] + animated.position[1],
                origin[2] + animated.position[2]);
        if (z != 0) stack.mulPose(Vector3f.ZP.rotationDegrees(z));
        if (y != 0) stack.mulPose(Vector3f.YP.rotationDegrees(y));
        if (x != 0) stack.mulPose(Vector3f.XP.rotationDegrees(x));
        stack.translate(-origin[0], -origin[1], -origin[2]);
    }

    private static final class Bone {
        final CerberusRig.Node node;
        final CerberusRig.Pose pose;
        final boolean detail, emissive;
        final Bone[] children;
        Bone(CerberusRig.Node node, CerberusRig.Pose pose, boolean detail, boolean emissive, Bone[] children) {
            this.node = node;
            this.pose = pose;
            this.detail = detail;
            this.emissive = emissive;
            this.children = children;
        }
    }
}
