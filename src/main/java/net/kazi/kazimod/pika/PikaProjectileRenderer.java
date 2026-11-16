package net.kazi.kazimod.pika;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class PikaProjectileRenderer<T extends AbilityProjectileEntity> extends EntityRenderer<T> {
    private final int kind;
    public PikaProjectileRenderer(EntityRendererManager manager,int kind){super(manager);this.kind=kind;}
    @Override public boolean shouldRender(T e,ClippingHelper camera,double x,double y,double z) {
        return e.distanceToSqr(x,y,z)<160*160&&camera.isVisible(e.getBoundingBox().inflate(kind==2?10:12));
    }
    @Override public void render(T e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        Minecraft mc=Minecraft.getInstance();float age=e.tickCount+partial;
        boolean far=e.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>64*64;
        float alpha=mc.player!=null&&e.getThrower()==mc.player&&mc.options.getCameraType().isFirstPerson()&&e.distanceToSqr(mc.player)<9?.3F:1;
        Quaternion camera=mc.getEntityRenderDispatcher().cameraOrientation();
        Vector3d direction=e.getDeltaMovement().lengthSqr()>.0001?e.getDeltaMovement():e.getViewVector(partial);
        if(kind==2) {
            stack.pushPose();PikaEffects.orient(stack,direction);PikaMesh.crescent(stack,buffers,4.5F,age,alpha,far);stack.popPose();
            PikaMesh.flare(stack,buffers,camera,.8F,alpha*.8F,0,far);
        } else {
            float radius=kind==0?.34F:.13F;
            float length=(float)Math.min(kind==0?15:10,Math.max(2,e.getDeltaMovement().length()*1.4));
            stack.pushPose();PikaEffects.orient(stack,direction.scale(-1));
            PikaMesh.beam(stack,buffers,radius,length,age,alpha,far,true);stack.popPose();
            PikaMesh.flare(stack,buffers,camera,kind==0?1.25F:.65F,alpha,0,far);
        }
    }
    @Override public ResourceLocation getTextureLocation(T entity){return net.minecraft.client.renderer.texture.AtlasTexture.LOCATION_BLOCKS;}
}
