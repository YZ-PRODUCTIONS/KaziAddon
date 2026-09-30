package net.kazi.kazimod.renderers.entities.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.models.abilities.ZushiVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.kazi.kazimod.renderers.entities.ZushiVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import xyz.pixelatedw.mineminenomi.entities.projectiles.zushi.SagariNoRyuseiProjectile;

public class SagariMeteorRenderer
extends EntityRenderer<SagariNoRyuseiProjectile> {
    public SagariMeteorRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public void render(SagariNoRyuseiProjectile entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        float radius = Math.max(0.0f, entity.getSize()) / 6.0f;
        if (radius <= 0.0f) {
            return;
        }
        Vector3d velocity = entity.getDeltaMovement();
        Vector3d wake = velocity.lengthSqr() < 1.0E-4 ? new Vector3d(0.0, 1.0, 0.0) : velocity.normalize().scale(-1.0);
        stack.pushPose();
        float turn = (float)Math.toDegrees(Math.atan2(wake.x, wake.z));
        float tilt = (float)Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, wake.y))));
        stack.mulPose(Vector3f.YP.rotationDegrees(turn));
        stack.mulPose(Vector3f.XP.rotationDegrees(tilt));
        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees(((float)entity.tickCount + partial) * 0.8f));
        ZushiVfxMesh.meteor(ZushiVfxRenderer.solidSink(stack, buffer), radius);
        stack.popPose();
        ZushiVfxMesh.meteorFire(KokuVfxRenderer.sink(stack, buffer), (float)entity.tickCount + partial, radius, (float)velocity.length());
        stack.popPose();
    }

    public boolean shouldRender(SagariNoRyuseiProjectile entity, ClippingHelper frustum, double x, double y, double z) {
        return entity.distanceToSqr(x, y, z) < 147456.0;
    }

    public ResourceLocation getTextureLocation(SagariNoRyuseiProjectile entity) {
        return KokuVfxRenderer.UNUSED_TEXTURE;
    }
}
