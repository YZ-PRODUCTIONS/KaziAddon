package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.BipedModel;
import xyz.pixelatedw.mineminenomi.renderers.entities.HumanoidRenderer;

public class ShadowDoppelmanRenderer extends HumanoidRenderer<ShadowDoppelmanEntity, BipedModel<ShadowDoppelmanEntity>> {

    public ShadowDoppelmanRenderer(EntityRendererManager manager) {
        super(manager, new BipedModel<>(0.0F), "mineminenomi:textures/models/doppelman.png");
        this.shadowRadius = 0.5F;
    }

    @Override
    public void scale(ShadowDoppelmanEntity entity, MatrixStack matrixStack, float partialTicks) {
        float shadowsUsed = entity.getShadows();
        float scale = shadowsUsed > 0.0F ? 1.0F + shadowsUsed / 6.0F : 1.0F;
        matrixStack.scale(scale, scale, scale);
    }
}
