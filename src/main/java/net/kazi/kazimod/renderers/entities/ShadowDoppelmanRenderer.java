package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class ShadowDoppelmanRenderer extends BipedRenderer<ShadowDoppelmanEntity, PlayerModel<ShadowDoppelmanEntity>> {
    private static final ResourceLocation DOPPELMAN_TEXTURE =
            new ResourceLocation("mineminenomi", "textures/models/doppelman.png");

    public ShadowDoppelmanRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void scale(ShadowDoppelmanEntity entity, MatrixStack matrixStack, float partialTicks) {
        float shadowsUsed = entity.getShadows();
        float scale = shadowsUsed > 0.0F ? 1.0F + shadowsUsed / 6.0F : 1.0F;
        matrixStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowDoppelmanEntity entity) {
        if (entity.isPlayerIllusion() && entity.getOwner() instanceof AbstractClientPlayerEntity) {
            return ((AbstractClientPlayerEntity) entity.getOwner()).getSkinTextureLocation();
        }
        return DOPPELMAN_TEXTURE;
    }
}
