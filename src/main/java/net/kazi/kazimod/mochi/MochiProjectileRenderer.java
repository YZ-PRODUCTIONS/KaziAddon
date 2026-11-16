package net.kazi.kazimod.mochi;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.*;
import net.MrMagicalCart.cartaddon.abilities.mochi2.MochiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class MochiProjectileRenderer<T extends AbilityProjectileEntity> extends EntityRenderer<T> {
    public enum Shape {BULLET,FIST,GRAB,SPIKE,DONUT,RIVER,FIRE}
    private static final class Trail {int tick=-1;float frame=-1,yaw,pitch;final List<Vector3d> points=new ArrayList<>(10);}
    private final Map<T,Trail> trails=new WeakHashMap<>();
    private final Shape shape;private final float radius;private final boolean hard;
    public MochiProjectileRenderer(EntityRendererManager manager,Shape shape,float radius,boolean hard){super(manager);this.shape=shape;this.radius=radius;this.hard=hard;}
    @Override public boolean shouldRender(T entity,ClippingHelper camera,double x,double y,double z) {
        return entity.distanceToSqr(x,y,z)<128*128&&camera.isVisible(entity.getBoundingBox().inflate(shape==Shape.RIVER?32:32+radius));
    }
    @Override public void render(T e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer buffers,int light) {
        boolean far=!MochiEffects.detailed(e);
        float age=(e.level.getGameTime()%100000)+partial+(e.getId()&4095)*1.7F;
        Vector3d origin=MochiEffects.interpolate(e,partial),motion=e.getDeltaMovement();
        if(trails.size()>128)trails.clear();Trail trail=trails.computeIfAbsent(e,k->new Trail());
        if(shape==Shape.DONUT) {
            // Cart's stationary donuts do not tick, so their old coordinates cannot be interpolated.
            Vector3d correction=e.position().subtract(origin);s.pushPose();s.translate(correction.x,correction.y,correction.z);
            if(trail.frame<0){trail.yaw=e.yRot;trail.pitch=e.xRot;}
            float blend=(float)(1-Math.exp(-MathHelper.clamp(age-trail.frame,0,3)*.55));trail.frame=age;
            trail.yaw+=MathHelper.wrapDegrees(e.yRot-trail.yaw)*blend;trail.pitch+=(e.xRot-trail.pitch)*blend;
            s.mulPose(Vector3f.YP.rotationDegrees(-trail.yaw));s.mulPose(Vector3f.XP.rotationDegrees(trail.pitch));
            MochiMesh mesh=new MochiMesh(s,buffers,age,far,light);mesh.ring(radius,radius*.24,MochiMesh.CREAM,false);
            mesh.ringDrips(radius,radius*.24,5,e.getId());s.popPose();return;
        }
        if(shape==Shape.RIVER) {
            s.pushPose();s.mulPose(Vector3f.YP.rotationDegrees(-e.yRot));
            MochiMesh mesh=new MochiMesh(s,buffers,age,far,light);mesh.river(14,24);mesh.drips(5,19,8,e.getId());s.popPose();return;
        }
        if(trail.tick!=e.tickCount) {
            trail.tick=e.tickCount;Vector3d point=new Vector3d(e.xOld,e.yOld,e.zOld);
            if(!trail.points.isEmpty()&&trail.points.get(0).distanceToSqr(point)>32*32)trail.points.clear();
            if(trail.points.isEmpty()||trail.points.get(0).distanceToSqr(point)>.01){trail.points.add(0,point);if(trail.points.size()>10)trail.points.remove(10);}
        }
        MochiMesh mesh=new MochiMesh(s,buffers,age,far,light);
        int count=Math.min(far?4:8,trail.points.size());
        Vector3d direction=motion.lengthSqr()>.001?motion.normalize():e.getLookAngle();
        if(count>0) {
            Vector3d[] points=new Vector3d[count+2];double[] radii=new double[count+2];points[0]=Vector3d.ZERO;radii[0]=radius*.58;
            for(int i=0;i<count;i++) {
                Vector3d offset=trail.points.get(i).subtract(origin);if(offset.length()>24)offset=offset.normalize().scale(24);
                double t=(i+1.0)/(count+1);points[i+1]=offset.add(0,-Math.sin(Math.PI*t)*radius*.18,0);
                radii[i+1]=radius*.45*(1-t)+.012;
            }
            points[count+1]=points[count].subtract(direction.scale(radius*.3));radii[count+1]=.003;
            mesh.tube(points,radii,MochiMesh.CREAM);
        } else mesh.strand(Vector3d.ZERO,direction.scale(-radius),direction.scale(-radius*2),direction.scale(-radius*3),radius*.5,.003,MochiMesh.CREAM);
        if(!far&&(shape==Shape.FIST||shape==Shape.GRAB))mesh.drips(radius*.5,-radius*.6,1,e.getId());
        s.pushPose();MochiMesh.orient(s,direction);mesh=new MochiMesh(s,buffers,age,far,light);
        boolean haki=hard||(e.getThrower()!=null&&MochiHelper.hasArmamentOn(e.getThrower()));
        if(shape==Shape.BULLET)mesh.blob(Vector3d.ZERO,radius,radius,radius*1.45,haki?MochiMesh.HAKI:MochiMesh.CREAM);
        else if(shape==Shape.SPIKE)mesh.strand(new Vector3d(0,0,-radius),Vector3d.ZERO,new Vector3d(0,0,radius*2),new Vector3d(0,0,radius*3),radius,.004,MochiMesh.HAKI);
        else {
            mesh.fist(radius,haki,shape==Shape.GRAB);
            if(shape==Shape.FIRE)for(int i=0;i<(far?4:8);i++) {
                double a=i*Math.PI/4;Vector3d start=new Vector3d(Math.cos(a)*radius,Math.sin(a)*radius,-radius*.2);
                Vector3d end=start.scale(1.1).add(0,0,-radius*(2.2+.4*Math.sin(age*.3+i)));
                mesh.strand(start,start.add(0,0,-radius*.7),end.add(0,0,radius*.6),end,radius*.16,.003,MochiMesh.TOASTED);
            }
        }
        s.popPose();
    }
    @Override public ResourceLocation getTextureLocation(T e){return new ResourceLocation("cartaddon","textures/models/projectiles/mochi_donut.png");}
}
