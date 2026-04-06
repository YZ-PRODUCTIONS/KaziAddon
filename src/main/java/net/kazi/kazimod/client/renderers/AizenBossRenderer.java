package net.kazi.kazimod.client.renderers;

import net.kazi.kazimod.entities.boss.aizen.AizenBossEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class AizenBossRenderer extends BipedRenderer<AizenBossEntity, PlayerModel<AizenBossEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/aizen_boss.png");

    public AizenBossRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AizenBossEntity entity) {
        return TEXTURE;
    }
}
