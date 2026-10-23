package net.kazi.kazimod.client.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.models.morphs.TripelTGodMorphModel;
import net.kazi.kazimod.entities.boss.rebelus.TrueFormRebelusBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.util.ResourceLocation;

public class TrueFormRebelusBossRenderer extends MobRenderer<TrueFormRebelusBossEntity, TripelTGodMorphModel<TrueFormRebelusBossEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/models/tripel_t_god.png");

    public TrueFormRebelusBossRenderer(EntityRendererManager manager) {
        super(manager, new TripelTGodMorphModel<>(), 0.85F);
        this.addLayer(new HeldItemLayer<>(this));
    }

    @Override
    protected void scale(TrueFormRebelusBossEntity entity, MatrixStack matrixStack, float partialTickTime) {
        matrixStack.scale(1.18F, 1.18F, 1.18F);
    }

    @Override
    public ResourceLocation getTextureLocation(TrueFormRebelusBossEntity entity) {
        return TEXTURE;
    }
}
