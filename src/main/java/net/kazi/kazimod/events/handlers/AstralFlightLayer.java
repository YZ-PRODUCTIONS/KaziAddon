package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.abilities.Yagi.AstralFlightAbility;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningMath;
import net.kazi.kazimod.client.asriel.AstralWingsMesh;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/** Colored copies of the actual posed body, with a short flowing rainbow wake. */
public final class AstralFlightLayer<T extends LivingEntity, M extends EntityModel<T>> extends LayerRenderer<T, M> {
    private static final int[] COLORS = {0xFF405C, 0xFF963B, 0xFFE94A, 0x59FF72, 0x40EBFF, 0x5482FF, 0xB15AFF};

    public AstralFlightLayer(IEntityRenderer<T, M> renderer) { super(renderer); }

    @Override
    public void render(MatrixStack stack, IRenderTypeBuffer buffers, int light, T entity,
                       float walk, float amount, float partial, float age, float yaw, float pitch) {
        if (entity.isInvisible() || net.kazi.kazimod.client.nagi.InnerDragonsCutsceneClient.active()
                || !AstralFlightAbility.hasAfterimages(entity)) return;
        boolean flying = AstralFlightAbility.isFlying(entity);
        boolean wings = AstralFlightAbility.hasRainbowWings(entity);
        Minecraft client = Minecraft.getInstance();
        double distance = client.gameRenderer.getMainCamera().getPosition().distanceTo(entity.position());
        if (distance > 80) return;
        int detail = VfxDetail.levelForDistance(distance);
        if (wings) {
            stack.pushPose();
            try {
                if (getParentModel() instanceof BipedModel<?>) {
                    BipedModel<?> model = (BipedModel<?>) getParentModel();
                    if (model.young) { stack.translate(0, .75, 0); stack.scale(.5F, .5F, .5F); }
                    model.body.translateAndRotate(stack);
                }
                // Local Y points up in the wing mesh; model coordinates point down.
                stack.translate(0, .02, .17);
                stack.scale(1, -1, 1);
                IVertexBuilder vertices = buffers.getBuffer(Glow.WINGS);
                AstralWingsMesh.render((x, y, z, rgb, alpha) -> vertices.vertex(stack.last().pose(), x, y, z)
                        .color((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, alpha).endVertex(),
                        entity.tickCount + partial, flying, detail);
            } finally { stack.popPose(); }
        }
        float brightness = YagiAwakeningMath.enhanced(entity.getHealth(), entity.getMaxHealth())
                ? YagiAwakeningMath.LOW_HEALTH_VISUAL_BRIGHTNESS : 1F;
        int stride = detail == 0 ? 1 : detail == 1 ? 2 : 3;
        IVertexBuilder ghost = new SilhouetteVertex(buffers.getBuffer(Glow.TYPE));
        // Convert the world-space wake into the already rotated/scaled model coordinates.
        // Use a full tick's displacement so the trail does not collapse between frames.
        Vector3d movement = new Vector3d(entity.getX() - entity.xOld,
                entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        if (movement.lengthSqr() > 1) movement = movement.normalize();
        float bodyYaw = MathHelper.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot) * ((float) Math.PI / 180F);
        double side = movement.x * Math.cos(bodyYaw) + movement.z * Math.sin(bodyYaw);
        double back = movement.x * Math.sin(bodyYaw) - movement.z * Math.cos(bodyYaw);
        for (int i = COLORS.length - 1; i >= 0; i -= stride) {
            float depth = i + 1;
            float wave = MathHelper.sin(age * .12F - depth * .38F);
            int color = COLORS[i];
            stack.pushPose();
            try {
                stack.translate(-side * depth * .34 + depth * (.055 + wave * .025),
                        movement.y * depth * .34 + wave * depth * .009,
                        -back * depth * .34 + depth * .085);
                getParentModel().renderToBuffer(stack, ghost, 15728880, OverlayTexture.NO_OVERLAY,
                        (color >> 16 & 255) / 255F * brightness,
                        (color >> 8 & 255) / 255F * brightness,
                        (color & 255) / 255F * brightness,
                        .30F - i * .025F);
            } finally {
                stack.popPose();
            }
        }
    }

    private static final class Glow extends RenderState {
        private Glow() { super(null, null, null); }
        static final RenderType WINGS = RenderType.create("kazimod_astral_rainbow_wings", DefaultVertexFormats.POSITION_COLOR,
                7, 32768, false, false, RenderType.State.builder()
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                        .setShadeModelState(SMOOTH_SHADE).createCompositeState(false));
        static final RenderType TYPE = RenderType.create("kazimod_astral_flight", DefaultVertexFormats.POSITION_COLOR,
                7, 32768, false, false, RenderType.State.builder()
                        .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    /** Strip skin UVs so every skin, including dark skins, has the same vivid spectral outline. */
    private static final class SilhouetteVertex implements IVertexBuilder {
        private final IVertexBuilder out;
        SilhouetteVertex(IVertexBuilder out) { this.out = out; }
        @Override public IVertexBuilder vertex(double x, double y, double z) { out.vertex(x, y, z); return this; }
        @Override public IVertexBuilder color(int r, int g, int b, int a) { out.color(r, g, b, a); return this; }
        @Override public IVertexBuilder uv(float u, float v) { return this; }
        @Override public IVertexBuilder overlayCoords(int u, int v) { return this; }
        @Override public IVertexBuilder uv2(int u, int v) { return this; }
        @Override public IVertexBuilder normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { out.endVertex(); }
    }
}
