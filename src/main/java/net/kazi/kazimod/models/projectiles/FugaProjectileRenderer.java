package net.kazi.kazimod.models.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.kazi.kazimod.entities.projectiles.FugaProjectile;

public class FugaProjectileRenderer extends EntityRenderer<FugaProjectile> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("kazimod", "textures/entities/projectiles/flame_arrow.png");
    private final FlameArrowProjectileModel model = new FlameArrowProjectileModel();

    public FugaProjectileRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public void render(FugaProjectile entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        matrixStack.pushPose();

        float yaw = entity.yRotO + (entity.yRot - entity.yRotO) * partialTicks;
        float pitch = entity.xRotO + (entity.xRot - entity.xRotO) * partialTicks;

        // Match vanilla arrow-style rotation first.
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(yaw - 90.0F));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(pitch));
        // Then correct for this model's local forward axis.
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));

        RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
        model.renderToBuffer(matrixStack, buffer.getBuffer(renderType), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FugaProjectile entity) {
        return TEXTURE;
    }
}
