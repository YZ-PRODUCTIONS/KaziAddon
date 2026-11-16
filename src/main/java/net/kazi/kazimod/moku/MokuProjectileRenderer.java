package net.kazi.kazimod.moku;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class MokuProjectileRenderer<T extends AbilityProjectileEntity> extends EntityRenderer<T> {
    private static final class Trail {int tick=-1;final List<Vector3d> points=new ArrayList<>(16);}
    private final Map<T,Trail> trails=new WeakHashMap<>();
    private final float radius;
    public MokuProjectileRenderer(EntityRendererManager manager,float radius){super(manager);this.radius=radius;}
    @Override public boolean shouldRender(T e,ClippingHelper camera,double x,double y,double z){
        return e.distanceToSqr(x,y,z)<128*128&&camera.isVisible(e.getBoundingBox().inflate(radius*3+12));
    }
    @Override public void render(T e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light) {
        Minecraft mc=Minecraft.getInstance();double distance=e.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition());
        boolean far=distance>40*40||!MokuEffects.projectileDetail();float age=e.tickCount+partial,alpha=1;
        if(mc.player!=null&&e.getThrower()==mc.player&&mc.options.getCameraType().isFirstPerson()&&distance<16)alpha=.18F;
        if(trails.size()>=128&&!trails.containsKey(e))trails.clear();
        Trail trail=trails.computeIfAbsent(e,k->new Trail());
        if(trail.tick!=e.tickCount) {
            trail.tick=e.tickCount;Vector3d point=new Vector3d(e.xOld,e.yOld,e.zOld);
            if(!trail.points.isEmpty()&&trail.points.get(0).distanceToSqr(point)>32*32)trail.points.clear();
            if(trail.points.isEmpty()||trail.points.get(0).distanceToSqr(point)>.01){trail.points.add(0,point);if(trail.points.size()>16)trail.points.remove(16);}
        }
        Quaternion camera=mc.getEntityRenderDispatcher().cameraOrientation();Vector3d origin=MokuEffects.interpolate(e,partial);
        for(int pass=0;pass<2;pass++) {
            MokuMesh.trail(s,b,camera,trail.points,origin,radius,age,alpha,far,pass);
            MokuMesh.projectile(s,b,camera,radius,age,alpha,e.getId(),far,pass);
        }
    }
    @Override public ResourceLocation getTextureLocation(T e){return MokuMesh.TEXTURE;}
}
