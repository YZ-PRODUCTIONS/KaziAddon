package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.RealityMarbleWeaponEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

/** Item-model projectile renderer modeled after Mahou Tsukai's weapon renderer. */
public class RealityMarbleWeaponRenderer extends EntityRenderer<RealityMarbleWeaponEntity> {
    public RealityMarbleWeaponRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(RealityMarbleWeaponEntity entity) {
        return AtlasTexture.LOCATION_BLOCKS;
    }

    @Override
    public boolean shouldRender(RealityMarbleWeaponEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, 6);
    }

    @Override
    public void render(RealityMarbleWeaponEntity entity, float entityYaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        ItemStack weapon = entity.getWeapon();
        if (weapon.isEmpty()) return;

        stack.pushPose();
        float xRotation = entity.xRotO + (entity.xRot - entity.xRotO) * partialTick;
        float yRotation = entity.getYRotation();
        float zRotation = entity.getZRotation();
        stack.mulPose(Vector3f.YP.rotationDegrees(yRotation));
        stack.mulPose(Vector3f.XP.rotationDegrees(xRotation));
        stack.mulPose(Vector3f.ZP.rotationDegrees(zRotation));
        stack.translate(-0.59D, -0.59D, 0.0D);
        stack.scale(3.75F, 3.75F, 3.75F);

        Minecraft.getInstance().getItemRenderer().renderStatic(weapon,
                ItemCameraTransforms.TransformType.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, stack, buffer);
        stack.popPose();
        super.render(entity, entityYaw, partialTick, stack, buffer, packedLight);
    }
}
