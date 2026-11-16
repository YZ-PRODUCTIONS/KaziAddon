package net.kazi.kazimod.gomu;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.models.abilities.EntityArmModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;

/** World-space connections cannot inherit projectile-only scaling wrappers. */
public final class GomuLimbRenderer extends AbilityProjectileRenderer<AbilityProjectileEntity, EntityModel<AbilityProjectileEntity>> {
    public GomuLimbRenderer(EntityRendererManager manager) {
        super(manager, new EntityArmModel(), null);
        setPlayerTexture(true);
    }

    @Override public ResourceLocation getTextureLocation(AbilityProjectileEntity projectile) {
        ResourceLocation texture = super.getTextureLocation(projectile);
        if (texture != null) return texture;
        if (projectile.getThrower() != null) {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(projectile.getThrower());
            if (renderer != null) return renderer.getTextureLocation(projectile.getThrower());
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override public boolean shouldRender(AbilityProjectileEntity projectile, ClippingHelper frustum, double x, double y, double z) {
        if (((GomuLimbData)projectile).kazi$arm() != 0 && projectile.getThrower() != null
                && projectile.getThrower().distanceToSqr(x, y, z) < 128 * 128) return true;
        return super.shouldRender(projectile, frustum, x, y, z);
    }

    @Override public void render(AbilityProjectileEntity projectile, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        if (!GomuLimbClient.render(projectile, partial, stack, buffer, light, this)) {
            setScale((float)Math.max(1, GomuLimbs.fistWidth(projectile) * 4));
            super.render(projectile, yaw, partial, stack, buffer, light);
        }
    }
}
