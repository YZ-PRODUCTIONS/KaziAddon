package net.kazi.kazimod.client.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.models.SukunaBossModel;
import net.kazi.kazimod.entities.boss.sukuna.SukunaBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;

public class SukunaBossRenderer extends MobRenderer<SukunaBossEntity, SukunaBossModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/sukuna_boss.png");

    public SukunaBossRenderer(EntityRendererManager manager) {
        super(manager, new SukunaBossModel(), 1.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SukunaBossEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SukunaBossEntity entity, MatrixStack matrixStack, float partialTickTime) {
        matrixStack.scale(1.5F, 1.5F, 1.5F);
    }


}