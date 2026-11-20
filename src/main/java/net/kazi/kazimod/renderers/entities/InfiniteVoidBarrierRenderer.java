package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneClient;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.jjk.ClashSky;
import net.kazi.kazimod.jjk.ClashStage;
import net.kazi.kazimod.models.abilities.InfiniteVoidMesh;
import net.kazi.kazimod.models.abilities.KokuVfxMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class InfiniteVoidBarrierRenderer extends EntityRenderer<InfiniteVoidBarrierEntity> {
    private static final ResourceLocation UNUSED_TEXTURE = new ResourceLocation("kazimod", "textures/entity/empty.png");

    public InfiniteVoidBarrierRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0;
    }

    @Override
    public boolean shouldRender(InfiniteVoidBarrierEntity entity, ClippingHelper frustum, double x, double y, double z) {
        double r = entity.getRadius();
        if (!entity.hasVoidVisual() || r <= 0 || entity.distanceToSqr(x, y, z) > (r + 192) * (r + 192)) return false;
        return frustum.isVisible(new AxisAlignedBB(entity.getX() - r, entity.getY() - r, entity.getZ() - r,
                entity.getX() + r, entity.getY() + r, entity.getZ() + r));
    }

    @Override
    public void render(InfiniteVoidBarrierEntity entity, float entityYaw, float partialTicks,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        if (!entity.hasVoidVisual()) return;
        float radius = entity.getVisualRadius(partialTicks);
        boolean clash = entity.isClashVisual();
        if (clash) {
            ClashStage stage = InnerDragonsCutsceneClient.clashStage();
            if (stage != null && stage.anchor.distanceToSqr(entity.position()) < .01) {
                // An opaque, still-growing wall must not pass between the lens and the cast.
                // Only the cinematic audience sees this backdrop; collision and the duel stay unchanged.
                radius = (float)Math.max(entity.getRadius(), Math.max(
                        entityRenderDispatcher.camera.getPosition().distanceTo(entity.position()),
                        stage.kama.distanceTo(entity.position())) + 2.5);
            }
        }
        if (radius < .1F) return;
        stack.pushPose();
        try {
            float yaw = entity.yRot;
            float age = entity.getVisualAge(partialTicks);
            Vector3d eye = entityRenderDispatcher.camera.getPosition().subtract(entity.position());
            boolean inside = eye.lengthSqr() < radius * radius;
            stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));
            if (clash) {
                ClashSky.shell(sink(stack, buffer, States.SHELL, 0), radius, age);
                ClashSky.boundary(sink(stack, buffer, States.ENERGY, 0), radius * (inside ? .98F : 1.015F), age);
                // The normal Void core forms along local +Z. Face it away from the opposing shrine.
                stack.mulPose(Vector3f.YP.rotationDegrees(180));
                yaw += 180;
            } else {
                InfiniteVoidMesh.shell(sink(stack, buffer, States.SHELL, 0), radius);
            }
            if (!inside) return;
            yaw *= (float) Math.PI / 180;
            float x = (float) (Math.cos(yaw) * eye.x + Math.sin(yaw) * eye.z);
            float y = (float) eye.y;
            float z = (float) (-Math.sin(yaw) * eye.x + Math.cos(yaw) * eye.z);
            int detail = VfxDetail.level(entity, entityRenderDispatcher, radius);
            InfiniteVoidMesh.horizon(sink(stack, buffer, States.HORIZON, detail), radius, age, x, y, z);
            InfiniteVoidMesh.energy(sink(stack, buffer, States.ENERGY, detail), radius, age, x, y, z);
        } finally {
            stack.popPose();
        }
    }

    private static KokuVfxMesh.Sink sink(MatrixStack stack, IRenderTypeBuffer buffer, RenderType type, int detail) {
        IVertexBuilder vertices = buffer.getBuffer(type);
        Matrix4f pose = stack.last().pose();
        return KokuVfxMesh.withDetail((x, y, z, color, alpha) -> vertices.vertex(pose, x, y, z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, MathHelper.clamp((int) (alpha * 255), 0, 255))
                .endVertex(), detail);
    }

    @Override
    public ResourceLocation getTextureLocation(InfiniteVoidBarrierEntity entity) { return UNUSED_TEXTURE; }

    private static final class States extends RenderState {
        private static boolean restoreFog;
        private static final FogState VOID_FOG = new FogState("kazimod_void_clear_fog", () -> {
            restoreFog = GL11.glIsEnabled(GL11.GL_FOG);
            RenderSystem.disableFog();
        }, () -> {
            if (restoreFog) RenderSystem.enableFog();
        });
        private static final RenderType SHELL = create("shell", false, false);
        private static final RenderType HORIZON = create("horizon", false, true);
        private static final RenderType ENERGY = create("energy", true, true);

        private States() { super(null, null, null); }

        private static RenderType create(String name, boolean glow, boolean transparent) {
            return RenderType.create("kazimod_void_" + name, DefaultVertexFormats.POSITION_COLOR, 7, 524288, false, false,
                    RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                            .setTransparencyState(glow ? LIGHTNING_TRANSPARENCY : transparent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                            .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setFogState(VOID_FOG)
                            .setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                            .setWriteMaskState(glow ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
        }
    }
}
