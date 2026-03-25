package net.kazi.kazimod.client.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.models.LuffyBossModel;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;

public class LuffyBossRenderer extends MobRenderer<LuffyBossEntity, LuffyBossModel> {

    // Place at: assets/kazimod/textures/entities/luffy_boss.png
    private static final ResourceLocation TEXTURE_NORMAL =
            new ResourceLocation("kazimod", "textures/entities/luffy_boss.png");

    // Place at: assets/kazimod/textures/entities/luffy_gear_5.png
    private static final ResourceLocation TEXTURE_GEAR5 =
            new ResourceLocation("kazimod", "textures/entities/luffy_gear_5.png");

    public LuffyBossRenderer(EntityRendererManager manager) {
        super(manager, new LuffyBossModel(), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LuffyBossEntity entity) {
        return entity.isGear5Awakened() ? TEXTURE_GEAR5 : TEXTURE_NORMAL;
    }

    @Override
    protected void scale(LuffyBossEntity entity, MatrixStack matrixStack, float partialTick) {
        matrixStack.scale(1.1F, 1.1F, 1.1F);
    }
}