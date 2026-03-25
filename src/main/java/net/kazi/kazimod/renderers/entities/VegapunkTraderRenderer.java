package net.kazi.kazimod.renderers.entities;

import net.kazi.kazimod.entities.VegapunkTraderEntity;
import net.kazi.kazimod.models.entities.VegapunkTraderModel;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;

public class VegapunkTraderRenderer extends MobRenderer<VegapunkTraderEntity, VegapunkTraderModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/vegapunk_trader.png");

    public VegapunkTraderRenderer(EntityRendererManager manager) {
        super(manager, new VegapunkTraderModel(), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(VegapunkTraderEntity entity) {
        return TEXTURE;
    }
}