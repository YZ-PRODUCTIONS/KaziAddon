package net.kazi.kazimod.renderers.entities.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.projectiles.TripelTBatProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class TripelTBatProjectileRenderer extends EntityRenderer<TripelTBatProjectile> {

    public TripelTBatProjectileRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public void render(TripelTBatProjectile entity, float entityYaw, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        ItemStack itemStack = entity.getItem();
        if (itemStack.isEmpty()) {
            return;
        }

        stack.pushPose();
        float yaw = MathHelper.lerp(partialTicks, entity.yRotO, entity.yRot) - 90.0F;
        float pitch = MathHelper.lerp(partialTicks, entity.xRotO, entity.xRot);
        stack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        stack.mulPose(Vector3f.ZP.rotationDegrees(pitch + 90.0F));
        stack.mulPose(Vector3f.XP.rotationDegrees((entity.tickCount + partialTicks) * 24.0F));
        stack.scale(1.25F, 1.25F, 1.25F);
        Minecraft.getInstance().getItemRenderer().renderStatic(itemStack, ItemCameraTransforms.TransformType.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, stack, buffer);
        stack.popPose();
        super.render(entity, entityYaw, partialTicks, stack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TripelTBatProjectile entity) {
        return AtlasTexture.LOCATION_BLOCKS;
    }
}
