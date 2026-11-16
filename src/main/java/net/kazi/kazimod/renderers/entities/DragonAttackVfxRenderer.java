package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Arrays;
import net.kazi.kazimod.entities.DragonAttackVfxEntity;
import net.kazi.kazimod.models.abilities.DragonAttackMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Animated attack silhouettes and emitted light, independent of the dragons' collision boxes. */
public final class DragonAttackVfxRenderer extends EntityRenderer<DragonAttackVfxEntity> {
    private static final ResourceLocation UNUSED = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    private final VertexBatch surface = new VertexBatch(2048);
    private final VertexBatch glow = new VertexBatch(4096);

    public DragonAttackVfxRenderer(EntityRendererManager manager) {
        super(manager);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(DragonAttackVfxEntity entity) { return UNUSED; }

    @Override public boolean shouldRender(DragonAttackVfxEntity entity, ClippingHelper frustum,
                                          double x, double y, double z) {
        if (!entity.isReady()) return false;
        float radius = DragonAttackMesh.radius(entity.getKind(), entity.getPhase(), entity.getLength());
        return Float.isFinite(radius) && radius > 0 && VfxDetail.visible(entity, frustum, x, y, z, radius);
    }

    @Override public void render(DragonAttackVfxEntity entity, float yaw, float partial,
                                 MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        if (!entity.isReady()) return;
        float age = entity.getVisualAge(partial);
        float life = entity.getLifetime();
        float length = entity.getLength();
        if (!Float.isFinite(age) || !Float.isFinite(life) || !Float.isFinite(length)
                || age < 0 || life <= 0 || age >= life || length < 0) return;
        float radius = DragonAttackMesh.radius(entity.getKind(), entity.getPhase(), length);
        if (!Float.isFinite(radius) || radius <= 0) return;
        Vector3d direction = entity.getVisualDirection(partial);
        if (!Double.isFinite(direction.x) || !Double.isFinite(direction.y) || !Double.isFinite(direction.z)) return;
        Vector3d forward = direction.lengthSqr() < .000001 ? new Vector3d(0, 0, 1) : direction.normalize();
        Vector3d camera = localCamera(entity, partial, forward);
        int detail = VfxDetail.level(entity, entityRenderDispatcher, radius);
        surface.clear();
        glow.clear();
        stack.pushPose();
        try {
            GaeBolgVfxRenderer.orient(stack, forward);
            Matrix4f pose = stack.last().pose();
            DragonAttackMesh.render(surface, glow, entity.getKind(), entity.getPhase(),
                    detail, age, life, length, entity.getSeed());
            // Submit one material fully before requesting the next builder: requesting a
            // different RenderType can flush the shared buffer underneath its old builder.
            surface.draw(pose, buffers.getBuffer(Materials.SURFACE), camera);
            glow.draw(pose, buffers.getBuffer(Materials.GLOW), camera);
        } finally {
            surface.clear();
            glow.clear();
            stack.popPose();
        }
    }

    /** Match the yaw/pitch frame used by GaeBolgVfxRenderer.orient, including vertical attacks. */
    private Vector3d localCamera(DragonAttackVfxEntity entity, float partial, Vector3d forward) {
        Vector3d origin = new Vector3d(MathHelper.lerp(partial, entity.xOld, entity.getX()),
                MathHelper.lerp(partial, entity.yOld, entity.getY()),
                MathHelper.lerp(partial, entity.zOld, entity.getZ()));
        Vector3d relative = entityRenderDispatcher.camera.getPosition().subtract(origin);
        double yaw = Math.atan2(forward.x, forward.z);
        Vector3d right = new Vector3d(Math.cos(yaw), 0, -Math.sin(yaw));
        Vector3d up = forward.cross(right);
        return new Vector3d(relative.dot(right), relative.dot(up), relative.dot(forward));
    }

    /** Retain storage between events; mesh generation allocates no objects per vertex. */
    private static final class VertexBatch implements DragonAttackMesh.Sink {
        private float[] vertices;
        private int size;

        VertexBatch(int capacity) { vertices = new float[capacity * 5]; }

        @Override public void vertex(float x, float y, float z, int color, float alpha) {
            if (size + 5 > vertices.length) vertices = Arrays.copyOf(vertices, vertices.length * 2);
            vertices[size++] = x;
            vertices[size++] = y;
            vertices[size++] = z;
            // A packed 24-bit RGB integer is exactly representable by a float.
            vertices[size++] = color & 0xFFFFFF;
            vertices[size++] = alpha;
        }

        void draw(Matrix4f pose, IVertexBuilder out, Vector3d camera) {
            for (int quad = 0; quad + 19 < size; quad += 20) {
                if (vertices[quad + 4] + vertices[quad + 9] + vertices[quad + 14] + vertices[quad + 19] < .0001F) continue;
                for (int i = quad; i < quad + 20; i += 5) {
                    float x = vertices[i], y = vertices[i + 1], z = vertices[i + 2];
                    int color = (int) vertices[i + 3];
                    double dx = x - camera.x, dy = y - camera.y, dz = z - camera.z;
                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    float visibility = 1;
                    if (distanceSquared < 2.25) {
                        float p = MathHelper.clamp((float) ((Math.sqrt(distanceSquared) - .3) / 1.2), 0, 1);
                        visibility = p * p * (3 - 2 * p);
                    }
                    // Keep transparent vertices: skipping any one would corrupt the QUAD grouping.
                    out.vertex(pose, x, y, z)
                            .color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F,
                                    MathHelper.clamp(vertices[i + 4], 0, 1) * visibility).endVertex();
                }
            }
        }

        void clear() { size = 0; }
    }

    /** Both layers respect terrain depth and never write depth over the world or other VFX. */
    private static final class Materials extends RenderState {
        private static final RenderType SURFACE = create("kazimod_dragon_attack_surface", TRANSLUCENT_TRANSPARENCY);
        private static final RenderType GLOW = create("kazimod_dragon_attack_glow", LIGHTNING_TRANSPARENCY);

        private Materials() { super(null, null, null); }

        private static RenderType create(String name, TransparencyState blend) {
            return RenderType.create(name, DefaultVertexFormats.POSITION_COLOR, 7, 131072,
                    false, blend == TRANSLUCENT_TRANSPARENCY,
                    RenderType.State.builder().setTransparencyState(blend).setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setShadeModelState(SMOOTH_SHADE).createCompositeState(false));
        }
    }
}
