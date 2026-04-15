package net.kazi.kazimod.client.renderers;

import net.kazi.kazimod.entities.boss.sunjinwoo.SunJinWooBossEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class SunJinWooBossRenderer extends BipedRenderer<SunJinWooBossEntity, PlayerModel<SunJinWooBossEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/sun_jin_woo_boss.png");

    public SunJinWooBossRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SunJinWooBossEntity entity) {
        return TEXTURE;
    }
}
