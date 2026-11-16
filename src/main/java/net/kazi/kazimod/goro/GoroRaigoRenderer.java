package net.kazi.kazimod.goro;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;

public final class GoroRaigoRenderer<T extends AbilityProjectileEntity & IFlexibleSizeProjectile> extends EntityRenderer<T> {
    public GoroRaigoRenderer(EntityRendererManager manager) { super(manager); shadowRadius=0; }
    @Override public void render(T entity,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        Vector3d origin=new Vector3d(MathHelper.lerp(partial,entity.xOld,entity.getX()),MathHelper.lerp(partial,entity.yOld,entity.getY()),MathHelper.lerp(partial,entity.zOld,entity.getZ()));
        Vector3d eye=entityRenderDispatcher.camera.getPosition().subtract(origin);
        GoroMesh.raigo(stack,buffers,entity.getSize()*.5F,entity.tickCount+partial,eye,entity.getId(),eye.lengthSqr()>160*160);
    }
    @Override public boolean shouldRender(T entity,ClippingHelper camera,double x,double y,double z) {
        return camera.isVisible(entity.getBoundingBox().inflate(entity.getSize()));
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return null; }
}
