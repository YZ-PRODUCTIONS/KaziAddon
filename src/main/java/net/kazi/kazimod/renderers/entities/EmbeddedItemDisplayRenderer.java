package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.EmbeddedItemDisplayEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

/** Renders a fixed weapon model for EmbeddedItemDisplayEntity. */
public class EmbeddedItemDisplayRenderer extends EntityRenderer<EmbeddedItemDisplayEntity> {
    public EmbeddedItemDisplayRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(EmbeddedItemDisplayEntity entity) {
        return AtlasTexture.LOCATION_BLOCKS;
    }

    @Override
    public void render(EmbeddedItemDisplayEntity entity, float entityYaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        ItemStack displayedItem = entity.getDisplayedItem();
        if (displayedItem.isEmpty()) return;

        stack.pushPose();
        float yaw = entity.yRotO + (entity.yRot - entity.yRotO) * partialTick;
        stack.translate(0.0D, 0.35D, 0.0D);
        stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));
        stack.mulPose(Vector3f.XP.rotationDegrees(65.0F));
        stack.scale(1.8F, 1.8F, 1.8F);
        Minecraft.getInstance().getItemRenderer().renderStatic(displayedItem,
                ItemCameraTransforms.TransformType.GUI, packedLight,
                OverlayTexture.NO_OVERLAY, stack, buffer);
        stack.popPose();
    }
}
