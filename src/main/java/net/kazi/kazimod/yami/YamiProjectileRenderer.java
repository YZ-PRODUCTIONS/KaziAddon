package net.kazi.kazimod.yami;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;

public final class YamiProjectileRenderer<T extends AbilityProjectileEntity> extends EntityRenderer<T> {
    private final float radius;
    private final boolean road;
    public YamiProjectileRenderer(EntityRendererManager manager,float radius,boolean road) {
        super(manager);this.radius=radius;this.road=road;shadowRadius=0;
    }
    private float radius(T entity) {
        return entity instanceof IFlexibleSizeProjectile?Math.max(.25F,((IFlexibleSizeProjectile)entity).getSize()*.5F):radius;
    }
    @Override public void render(T entity,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        float r=radius(entity),age=entity.tickCount+partial,alpha=Math.min(1,(age+1)/4);
        Vector3d position=YamiEffects.interpolate(entity,partial);
        double distance=entityRenderDispatcher.camera.getPosition().distanceToSqr(position);
        boolean far=distance>32*32;
        if(entity.getThrower()==Minecraft.getInstance().player&&Minecraft.getInstance().options.getCameraType().isFirstPerson()&&distance<r*r*1.4)alpha*=.22F;
        if(road) {
            YamiMesh.vapor(stack,buffers,entityRenderDispatcher.cameraOrientation(),1.2F,2,age,alpha,entity.getId(),far?2:5,false);
        } else {
            YamiMesh.sphere(stack,buffers,entityRenderDispatcher.cameraOrientation(),r,age,alpha,entity.getId(),far);
        }
        Vector3d motion=entity.getDeltaMovement();
        if(!far && motion.lengthSqr()>.04) {
            Vector3d back=motion.normalize().scale(-Math.min(2,motion.length())*.6);
            for(int i=1;i<=1;i++) {
                stack.pushPose();stack.translate(back.x*i,back.y*i,back.z*i);
                YamiMesh.vapor(stack,buffers,entityRenderDispatcher.cameraOrientation(),r*(1-i*.16F),r,age-i*3,alpha*(1-i*.22F),entity.getId()+i,3,true);
                stack.popPose();
            }
        }
    }
    @Override public boolean shouldRender(T entity,ClippingHelper frustum,double x,double y,double z) {
        return entity.shouldRenderAtSqrDistance(entity.distanceToSqr(x,y,z))&&frustum.isVisible(entity.getBoundingBox().inflate(radius(entity)*2.5));
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return YamiMesh.SMOKE; }
}
