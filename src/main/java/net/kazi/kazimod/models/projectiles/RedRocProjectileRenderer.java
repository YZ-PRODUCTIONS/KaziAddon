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


    public RedRocProjectileRenderer(EntityRendererManager renderManager, M model, M stretchModel) {
        super(renderManager, model, stretchModel);
    }


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
