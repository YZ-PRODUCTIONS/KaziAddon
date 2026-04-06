package net.kazi.kazimod.client.renderers;

import net.kazi.kazimod.entities.boss.law.LawBossEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class LawBossRenderer extends BipedRenderer<LawBossEntity, PlayerModel<LawBossEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/law_boss.png");

    public LawBossRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LawBossEntity entity) {
        return TEXTURE;
    }
}
