package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.nagi.DragonSummonModels;
import net.kazi.kazimod.entities.DragonStrikeEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

/** Each articulated Blockbench dragon follows its own entity, never the caster's aim or arm. */
public final class DragonStrikeRenderer extends EntityRenderer<DragonStrikeEntity> {
    private static final double VISUAL_RADIUS = 10;

    public DragonStrikeRenderer(EntityRendererManager manager) {
        super(manager);
        shadowRadius = .5F;
        DragonSummonModels.installReloadListener();
    }
    @Override public ResourceLocation getTextureLocation(DragonStrikeEntity entity) {
        return DragonSummonModels.texture(entity.getKind());
    }
    @Override public boolean shouldRender(DragonStrikeEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, VISUAL_RADIUS);
    }
    @Override public void render(DragonStrikeEntity entity, float yaw, float partial,
                                 MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        stack.pushPose();
        try {
            GaeBolgVfxRenderer.orient(stack, entity.getViewVector(partial));
            double distance = entityRenderDispatcher.camera.getPosition().distanceToSqr(entity.position());
            // Only tiny bevels/scales are omitted in the distance. Particle settings do not
            // downgrade the actual dragon models or strip their distinguishing features.
            boolean distant = distance > 48 * 48;
            float phase = entity.getKind().ordinal() * .37F + (entity.getId() & 15) * .13F;
            DragonSummonModels.render(entity.getKind(), stack, buffers, light,
                    entity.getAge(partial), entity.getAttack(partial), phase, distant);
        } finally { stack.popPose(); }
    }
}
