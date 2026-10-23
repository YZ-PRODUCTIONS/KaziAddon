package net.kazi.kazimod.renderers.entities.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.models.abilities.GuraVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class GekishinVfxRenderer<T extends AbilityProjectileEntity> extends EntityRenderer<T> {
    private final float radius;
    public GekishinVfxRenderer(EntityRendererManager manager) { this(manager, 5.5F); }
    public GekishinVfxRenderer(EntityRendererManager manager, float radius) { super(manager); this.radius = radius; }
    @Override public boolean shouldRender(T e, ClippingHelper frustum, double x, double y, double z) {
        return e.shouldRender(x, y, z) && frustum.isVisible(e.getBoundingBox().inflate(radius * 1.6));
    }
    @Override public void render(T e, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        stack.pushPose();
        Vector3d movement = e.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-8) {
            movement = new Vector3d(e.getX() - e.xOld, e.getY() - e.yOld, e.getZ() - e.zOld);
        }
        // The mesh front is +Z. Throwable rotation uses opposite signs to player aim.
        float turn = movement.lengthSqr() >= 1.0E-8 ? GuraVfxMesh.travelYaw(movement.x, movement.z)
                : MathHelper.rotLerp(partial, e.yRotO, e.yRot);
        float tilt = movement.lengthSqr() >= 1.0E-8 ? GuraVfxMesh.travelPitch(movement.x, movement.y, movement.z)
                : -MathHelper.lerp(partial, e.xRotO, e.xRot);
        stack.mulPose(Vector3f.YP.rotationDegrees(turn));
        stack.mulPose(Vector3f.XP.rotationDegrees(tilt));
        GuraVfxMesh.projectile(KokuVfxRenderer.sink(stack, buffer), radius, e.tickCount + partial, 1, e.getId());
        stack.popPose();
    }
    @Override public ResourceLocation getTextureLocation(T e) { return KokuVfxRenderer.UNUSED_TEXTURE; }
}
