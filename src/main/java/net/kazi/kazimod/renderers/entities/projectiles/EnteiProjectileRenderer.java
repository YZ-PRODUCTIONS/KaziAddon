package net.kazi.kazimod.renderers.entities.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.EnteiGeometry;
import net.kazi.kazimod.renderers.entities.EnteiBlastRenderer;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.entities.projectiles.mera.DaiEnkaiEnteiProjectile;

public class EnteiProjectileRenderer
extends EntityRenderer<DaiEnkaiEnteiProjectile> {
    public EnteiProjectileRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public void render(DaiEnkaiEnteiProjectile entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        EnteiBlastRenderer.fireball(stack, buffer, (float)entity.tickCount + partial, EnteiGeometry.fireballRadius(entity.getSize()), 1.0f);
    }

    public boolean shouldRender(DaiEnkaiEnteiProjectile entity, ClippingHelper frustum, double x, double y, double z) {
        return entity.distanceToSqr(x, y, z) < 65536.0;
    }

    public ResourceLocation getTextureLocation(DaiEnkaiEnteiProjectile entity) {
        return KokuVfxRenderer.UNUSED_TEXTURE;
    }
}
