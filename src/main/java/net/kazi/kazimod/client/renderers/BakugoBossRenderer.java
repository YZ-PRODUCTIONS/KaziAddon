package net.kazi.kazimod.client.renderers;

import net.kazi.kazimod.entities.boss.bakugo.BakugoBossEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class BakugoBossRenderer extends BipedRenderer<BakugoBossEntity, PlayerModel<BakugoBossEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/bakugo_boss.png");

    public BakugoBossRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BakugoBossEntity entity) {
        return TEXTURE;
    }
}
