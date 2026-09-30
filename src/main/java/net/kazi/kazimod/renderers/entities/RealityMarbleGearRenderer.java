package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.RealityMarbleGearEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

/** Centered clockwork gears with variant-colored, counter-rotating energy inlays. */
public class RealityMarbleGearRenderer extends EntityRenderer<RealityMarbleGearEntity> {
    private static final float GEAR_SCALE = 12.0F;
    private static final float ROTATION_TICKS = 400.0F;
    private static final float APPEAR_TICKS = 16.0F;

    public RealityMarbleGearRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(RealityMarbleGearEntity entity) {
        return AtlasTexture.LOCATION_BLOCKS;
    }

    @Override
    public boolean shouldRender(RealityMarbleGearEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, 16);
    }

    @Override
    public void render(RealityMarbleGearEntity entity, float entityYaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        stack.pushPose();
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, 15);
        float age = entity.tickCount + partialTick;
        float yaw = entity.yRotO + (entity.yRot - entity.yRotO) * partialTick;
        float spin = entity.getSpinOffset() + age * 360.0F / ROTATION_TICKS;
        float appear = Math.min(1.0F, Math.max(0.0F, age / APPEAR_TICKS));
        appear = appear * appear * (3.0F - 2.0F * appear);
        stack.translate(0.0D, 0.5D, 0.0D);
        stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));
        stack.mulPose(Vector3f.ZP.rotationDegrees(spin));
        float scale = GEAR_SCALE * (0.88F + 0.12F * appear);
        stack.scale(scale, scale, scale);
        // The mesh is authored around zero: all layers spin around the actual hub.
        RealityMarbleGearModel.renderBody(buffer.getBuffer(appear < 1.0F
                        ? RealityMarbleGearModel.FORMING : RealityMarbleGearModel.METAL),
                stack.last().pose(), entity.isMugenVariant(), appear, detail);
        RealityMarbleGearModel.renderEnergy(buffer.getBuffer(RealityMarbleGearModel.ENERGY),
                stack.last().pose(), entity.isMugenVariant(), age, entity.getSpinOffset(), appear, detail);
        stack.popPose();
    }
}
