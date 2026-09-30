package net.kazi.kazimod.renderers.entities.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.projectiles.HollowNukeProjectile;
import net.kazi.kazimod.models.abilities.KokuVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class KokuProjectileRenderer<T extends AbilityProjectileEntity>
extends EntityRenderer<T> {
    private final int style;
    private final float radius;

    public KokuProjectileRenderer(EntityRendererManager manager, int style, float radius) {
        super(manager);
        this.style = style;
        this.radius = radius;
        this.shadowRadius = 0.0f;
    }

    public boolean shouldRender(T entity, ClippingHelper frustum, double x, double y, double z) {
        return entity.distanceToSqr(x, y, z) < 256.0D * 256.0D
                && frustum.isVisible(entity.getBoundingBox().inflate(visualExtent(entity)));
    }

    private double visualExtent(T entity) {
        if (entity instanceof HollowNukeProjectile) {
            HollowNukeProjectile nuke = (HollowNukeProjectile) entity;
            return Math.max(24.0D, nuke.getWaveRadius(1.0F) * 2.0D + Math.abs(nuke.getGroundOffset()));
        }
        // Includes the complete rotating wake, not only the projectile's collision box.
        return this.radius * 6.0D + 1.0D;
    }

    public void render(T entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        stack.pushPose();
        if (entity instanceof HollowNukeProjectile) {
            HollowNukeProjectile nuke = (HollowNukeProjectile)((Object)entity);
            KokuVfxMesh.Sink out = KokuVfxMesh.withDetail(KokuVfxRenderer.sink(stack, buffer),
                    VfxDetail.level(entity, this.entityRenderDispatcher, visualExtent(entity)));
            KokuVfxMesh.nuke(out, nuke.getVfxAge(partial), nuke.getWaveRadius(partial), nuke.getVfxOpacity(partial), nuke.getGroundOffset());
        } else {
            Vector3d velocity = entity.getDeltaMovement();
            float directionYaw = (float)Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
            float directionPitch = (float)Math.toDegrees(Math.atan2(-velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)));
            stack.mulPose(Vector3f.YP.rotationDegrees(-directionYaw));
            stack.mulPose(Vector3f.XP.rotationDegrees(directionPitch));
            KokuVfxMesh.Sink out = KokuVfxMesh.withDetail(KokuVfxRenderer.sink(stack, buffer),
                    VfxDetail.level(entity, this.entityRenderDispatcher, visualExtent(entity)));
            KokuVfxMesh.orb(out, this.style, (float)((AbilityProjectileEntity)entity).tickCount + partial, this.radius, 1.0f, velocity.lengthSqr() > 0.01);
        }
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(T entity) {
        return KokuVfxRenderer.UNUSED_TEXTURE;
    }
}
