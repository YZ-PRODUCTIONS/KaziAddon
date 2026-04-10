package net.kazi.kazimod.models.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.models.projectiles.CrowModel;
import net.kazi.kazimod.entities.projectiles.HellfireBirdProjectile;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public class HellfireBirdRenderer extends EntityRenderer<HellfireBirdProjectile> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("cartaddon", "textures/models/projectiles/crow.png");

    private final CrowModel model = new CrowModel();

    public HellfireBirdRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public void render(HellfireBirdProjectile entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        matrixStack.pushPose();

        float yaw = entity.yRotO + (entity.yRot - entity.yRotO) * partialTicks;
        float pitch = entity.xRotO + (entity.xRot - entity.xRotO) * partialTicks;

        matrixStack.mulPose(Vector3f.YP.rotationDegrees(yaw - 90.0F));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(pitch));
        matrixStack.scale(1.25F, 1.25F, 1.25F);

        RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
        this.model.renderToBuffer(matrixStack, buffer.getBuffer(renderType), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 0.65F, 0.65F, 1.0F);

        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HellfireBirdProjectile entity) {
        return TEXTURE;
    }
}
