package net.kazi.kazimod.client.nagi;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only cinematic doubles and limb masking; never changes the player's equipment or health. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class InnerDragonsCutsceneVisuals {
    private static final Map<PlayerModel<?>, boolean[]> ARMS = new IdentityHashMap<>();
    private static final Batch SOLID = new Batch(), GLOW = new Batch();
    private static CinematicPlayerRenderer defaultProxyRenderer, slimProxyRenderer;
    private InnerDragonsCutsceneVisuals() { }

    public static boolean isArmMissing(Entity entity) {
        if (InnerDragonsArmClient.isTransformed(entity)) return true;
        if (InnerDragonsCutsceneClient.active() && !InnerDragonsCutsceneClient.isNagi()) return false;
        if (!(entity instanceof LivingEntity) || !InnerDragonsCutsceneClient.active()
                || !entity.getUUID().equals(InnerDragonsCutsceneClient.subjectUuid())
                || InnerDragonsCutsceneClient.age(Minecraft.getInstance().getFrameTime()) < InnerDragonsSequence.ARM_BREAK_TICK)
            return false;
        // Keep the cinematic arm consistent until the active scene finishes, even after healing.
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hideArm(RenderPlayerEvent.Pre event) {
        PlayerModel<?> model = event.getRenderer().getModel();
        // PlayerRenderer has just reset this model for this exact player. An earlier cancelled
        // render's snapshot is obsolete and must never overwrite another player's sleeve choice.
        ARMS.remove(model);
        if (!isArmMissing(event.getPlayer())) return;
        ARMS.put(model, new boolean[]{model.rightArm.visible, model.rightSleeve.visible});
        model.rightArm.visible = false;
        model.rightSleeve.visible = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreArm(RenderPlayerEvent.Post event) {
        restore(event.getRenderer().getModel());
    }

    private static void restore(PlayerModel<?> model) {
        boolean[] saved = ARMS.remove(model);
        if (saved != null) {
            model.rightArm.visible = saved[0];
            model.rightSleeve.visible = saved[1];
        }
    }

    @SubscribeEvent
    public static void cleanup(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ARMS.isEmpty()) return;
        for (Map.Entry<PlayerModel<?>, boolean[]> entry : ARMS.entrySet()) {
            entry.getKey().rightArm.visible = entry.getValue()[0];
            entry.getKey().rightSleeve.visible = entry.getValue()[1];
        }
        ARMS.clear();
    }

    @SubscribeEvent
    public static void world(RenderWorldLastEvent event) {
        if (!InnerDragonsCutsceneClient.active()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        float partial = event.getPartialTicks();
        float age = InnerDragonsCutsceneClient.age(partial);
        if (!InnerDragonsCutsceneClient.activeAt(age)) return;
        restoreSceneDepth(client);
        if (InnerDragonsCutsceneClient.isChaosFinale()) {
            net.kazi.kazimod.client.asriel.ChaosBeamFinale.world(event);
            return;
        }
        if (InnerDragonsCutsceneClient.isGoat()) {
            net.kazi.kazimod.client.asriel.HyperGonerVisuals.render(event);
            return;
        }
        if (InnerDragonsCutsceneClient.isGoru()) {
            net.kazi.kazimod.client.goru.EaDrawVisuals.render(event);
            return;
        }
        LivingEntity subject = InnerDragonsCutsceneClient.subject();
        Vector3d origin = InnerDragonsCutsceneClient.origin(partial);
        Vector3d camera = client.gameRenderer.getMainCamera().getPosition();
        float yaw = InnerDragonsCutsceneClient.yaw(partial);
        double angle = Math.toRadians(yaw);
        Vector3d right = new Vector3d(-Math.cos(angle), 0, -Math.sin(angle));
        Vector3d forward = new Vector3d(-Math.sin(angle), 0, Math.cos(angle));
        double shoulderHeight = subject != null && subject.isCrouching() ? 1.12 : 1.40;
        Vector3d shoulder = origin.add(right.scale(.34)).add(0, shoulderHeight, 0);
        MatrixStack stack = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers = client.renderBuffers().bufferSource();
        int detail = VfxDetail.levelForDistance(0);
        if (InnerDragonsCutsceneClient.fallbackSubject() && subject instanceof AbstractClientPlayerEntity) {
            renderProxy((AbstractClientPlayerEntity) subject, origin, camera, yaw, partial, stack, buffers);
        }
        SOLID.clear(); GLOW.clear();
        InnerDragonsEmergenceMesh.render(SOLID, GLOW, age, detail);
        stack.pushPose();
        try {
            stack.translate(shoulder.x - camera.x, shoulder.y - camera.y, shoulder.z - camera.z);
            stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));
            stack.scale(-1, 1, 1);
            SOLID.draw(stack.last().pose(), buffers.getBuffer(Materials.SOLID));
            GLOW.draw(stack.last().pose(), buffers.getBuffer(Materials.GLOW));
        } finally { stack.popPose(); }
        DragonSummonModels.installReloadListener();
        float exitFade = MathHelper.clamp((InnerDragonsSequence.DURATION_TICKS - age) / 8F, 0, 1);
        for (DragonKind kind : DragonKind.values()) {
            float progress = InnerDragonsSequence.emergence(kind.ordinal(), age);
            if (progress <= 0) continue;
            double[] position = InnerDragonsEmergenceMesh.dragonPosition(kind.ordinal(), progress, age);
            Vector3d offset = right.scale(position[0]).add(0, position[1], 0).add(forward.scale(position[2]));
            Vector3d point = shoulder.add(offset);
            Vector3d direction = offset.lengthSqr() > .001 ? offset.normalize() : right;
            float scale = .04F + .96F * progress;
            stack.pushPose();
            try {
                stack.translate(point.x - camera.x, point.y - camera.y, point.z - camera.z);
                orient(stack, direction);
                // A tiny head first becomes visible at the arm, then the full articulated body
                // unfurls while travelling outwards; each dragon has its own stagger and path.
                stack.scale(scale, scale, scale);
                DragonSummonModels.render(kind, stack, buffers, 15728880, age,
                        MathHelper.clamp((progress - .72F) * 1.4F, 0, .35F), kind.ordinal() * .43F,
                        detail > 0, Math.min(1, progress * 4) * exitFade);
            } finally { stack.popPose(); }
        }
        buffers.endBatch();
        SOLID.clear(); GLOW.clear();
    }

    private static void restoreSceneDepth(Minecraft client) {
        if (!Minecraft.useShaderTransparency()) return;
        Framebuffer sceneDepth = client.levelRenderer.getParticlesTarget();
        if (sceneDepth == null) return;
        // Fabulous finishes its transparency chain before RenderWorldLast. Its final blit
        // replaces the main target's world depth with a fullscreen quad, so LEQUAL rejects
        // the cinematic's perspective-space fragments. The particle target retains the
        // world/entity depth copied before that composite; restore it without clearing color
        // or disabling occlusion. copyDepthFrom changes framebuffer bindings, so rebind main.
        Framebuffer main = client.getMainRenderTarget();
        main.copyDepthFrom(sceneDepth);
        main.bindWrite(false);
    }

    private static void renderProxy(AbstractClientPlayerEntity subject, Vector3d point, Vector3d camera,
                                    float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers) {
        if (defaultProxyRenderer == null) {
            EntityRendererManager manager = Minecraft.getInstance().getEntityRenderDispatcher();
            defaultProxyRenderer = new CinematicPlayerRenderer(manager, false);
            slimProxyRenderer = new CinematicPlayerRenderer(manager, true);
        }
        PlayerRenderer renderer = "slim".equals(subject.getModelName()) ? slimProxyRenderer : defaultProxyRenderer;
        stack.pushPose();
        try {
            stack.translate(point.x - camera.x, point.y - camera.y, point.z - camera.z);
            renderer.render(subject, yaw, partial, stack, buffers, 15728880);
        } finally { stack.popPose(); }
    }

    private static void orient(MatrixStack stack, Vector3d direction) {
        Vector3d unit = direction.normalize();
        stack.mulPose(Vector3f.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(unit.x, unit.z))));
        stack.mulPose(Vector3f.XP.rotationDegrees((float) -Math.toDegrees(
                Math.asin(Math.max(-1, Math.min(1, unit.y))))));
    }

    private static final class CinematicPlayerRenderer extends PlayerRenderer {
        CinematicPlayerRenderer(EntityRendererManager manager, boolean slim) {
            super(manager, slim);
            addLayer(new net.kazi.kazimod.events.handlers.InnerDragonsArmLayer(this));
        }
        @Override protected void renderNameTag(AbstractClientPlayerEntity entity, ITextComponent text,
                                               MatrixStack stack, IRenderTypeBuffer buffers, int light) { }
    }

    /** Retain both material streams, then submit them sequentially to avoid mid-quad buffer flushes. */
    private static final class Batch implements InnerDragonsEmergenceMesh.Sink {
        private float[] vertices = new float[4096 * 5];
        private int size;
        @Override public void vertex(double x, double y, double z, int color, float alpha) {
            if (size + 5 > vertices.length) vertices = Arrays.copyOf(vertices, vertices.length * 2);
            vertices[size++] = (float) x; vertices[size++] = (float) y; vertices[size++] = (float) z;
            vertices[size++] = color & 0xFFFFFF; vertices[size++] = alpha;
        }
        void draw(Matrix4f matrix, IVertexBuilder out) {
            for (int i = 0; i < size; i += 5) {
                int color = (int) vertices[i + 3];
                out.vertex(matrix, vertices[i], vertices[i + 1], vertices[i + 2])
                        .color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F,
                                MathHelper.clamp(vertices[i + 4], 0, 1)).endVertex();
            }
        }
        void clear() { size = 0; }
    }

    private static final class Materials extends RenderState {
        static final RenderType SOLID = material("kazimod_inner_dragons_fragments", TRANSLUCENT_TRANSPARENCY);
        static final RenderType GLOW = material("kazimod_inner_dragons_rupture", LIGHTNING_TRANSPARENCY);
        private Materials() { super(null, null, null); }
        private static RenderType material(String name, TransparencyState blend) {
            return RenderType.create(name, DefaultVertexFormats.POSITION_COLOR, 7, 262144, false, false,
                    RenderType.State.builder().setTransparencyState(blend).setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setShadeModelState(SMOOTH_SHADE).createCompositeState(false));
        }
    }
}
