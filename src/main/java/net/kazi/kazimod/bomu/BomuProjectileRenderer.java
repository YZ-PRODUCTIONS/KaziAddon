package net.kazi.kazimod.bomu;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class BomuProjectileRenderer<T extends AbilityProjectileEntity> extends EntityRenderer<T> {
    private final float size;
    public BomuProjectileRenderer(EntityRendererManager manager, float size) { super(manager); this.size=size; shadowRadius=0; }
    @Override public void render(T e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        Vector3d v=e.getDeltaMovement();
        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(v.x,v.z))));
        stack.mulPose(Vector3f.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z)))));
        BomuMesh.projectile(stack,buffers,e.tickCount+partial,e.getId(),size);
        stack.popPose();
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return BomuVfxRenderer.UNUSED; }
}
