package net.kazi.kazimod.models.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.renderers.abilities.StretchingProjectileRenderer;

@OnlyIn(Dist.CLIENT)
public class RedRocProjectileRenderer<E extends AbilityProjectileEntity, M extends EntityModel<E>>
        extends StretchingProjectileRenderer<E, M> {

    // Base forward offset in blocks — minimum push applied regardless of speed.
    private static final float BASE_FORWARD_OFFSET = 1.0F;

    // How much stretchScaleZ * speed contributes to the forward offset.
    // StretchingProjectileRenderer scales arm length by stretchScaleZ * speed,
    // so we mirror that here to keep the fist flush with the arm tip at any speed.
    // If the arm still clips the fist, increase this. If the fist floats ahead, decrease it.
    private static final float STRETCH_OFFSET_MULTIPLIER = 0.35F;

    private float storedStretchScaleZ = 8.0F;

    public RedRocProjectileRenderer(EntityRendererManager renderManager, M model, M stretchModel) {
        super(renderManager, model, stretchModel);
    }

    @Override
    public void setStretchScale(double x, double y, double z) {
        super.setStretchScale(x, y, z);
        this.storedStretchScaleZ = (float) z;
    }

    @Override
    public void render(E entity, float entityYaw, float partialTicks, MatrixStack matrixStack,
                       IRenderTypeBuffer buffer, int packedLight) {

        Vector3d motion = entity.getDeltaMovement();
        double speed = motion.length();

        if (speed > 0.001) {
            Vector3d dir = motion.normalize();

            // Dynamic offset mirrors how StretchingProjectileRenderer calculates arm length:
            //   arm length ≈ stretchScaleZ * speed
            // So we push the fist forward by the same amount to keep it at the leading tip.
            float dynamicOffset = BASE_FORWARD_OFFSET + (storedStretchScaleZ * (float) speed * STRETCH_OFFSET_MULTIPLIER);

            matrixStack.pushPose();
            matrixStack.translate(
                    dir.x * dynamicOffset,
                    dir.y * dynamicOffset,
                    dir.z * dynamicOffset
            );

            super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);

            matrixStack.popPose();
        } else {
            // Fallback for near-zero velocity on first tick
            super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        }
    }





    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static class Factory extends StretchingProjectileRenderer.Factory {

        private final EntityModel internalStretchingModel;

        public Factory(EntityModel tipModel, EntityModel stretchModel) {
            super(tipModel, stretchModel);
            this.internalStretchingModel = stretchModel;
        }

        @Override
        public EntityRenderer<? super AbilityProjectileEntity> createRenderFor(EntityRendererManager manager) {
            RedRocProjectileRenderer renderer = new RedRocProjectileRenderer(
                    manager,
                    this.model,
                    this.internalStretchingModel
            );
            renderer.setStretchScale(this.stretchScaleX, this.stretchScaleY, this.stretchScaleZ);
            renderer.setScale(this.scaleX, this.scaleY, this.scaleZ);
            renderer.setColor(this.colour);
            renderer.setPlayerTexture(true);
            renderer.setGlowing(this.isGlowing);
            return renderer;
        }
    }
}