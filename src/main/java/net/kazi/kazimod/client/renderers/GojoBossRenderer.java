package net.kazi.kazimod.client.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.models.GojoBossModel;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;

public class GojoBossRenderer extends MobRenderer<GojoBossEntity, GojoBossModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/gojo_boss.png");

    public GojoBossRenderer(EntityRendererManager manager) {
        super(manager, new GojoBossModel(), 0.9f);
    }

    @Override
    public ResourceLocation getTextureLocation(GojoBossEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(GojoBossEntity entity, MatrixStack stack, float partialTick) {
        // The model geometry is 35 units tall (head 9 + body 13 + legs 13).
        // The entity hitbox is 2.5 blocks = 40 units.
        // Scale 40/35 ≈ 1.143, rounded up slightly to 1.15 so the model
        // visually fills the hitbox without leaving a gap at the top.
        stack.scale(1.15F, 1.15F, 1.15F);
    }
}