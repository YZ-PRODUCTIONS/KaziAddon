package net.kazi.kazimod.pika;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod",value=Dist.CLIENT)
public final class PikaEffects {
    private static final int LIMIT=64;
    private static final List<Light> ACTIVE=new ArrayList<>();
    private static World world; private static int clock,nextId=-1900000000;
    private static PikaRenderAnchor anchor;
    private static final class Light {
        int owner,kind,born,expiry; Vector3d pos,offset; float radius; boolean follows;
    }
    private static void world(World current){if(world!=current){ACTIVE.clear();anchor=null;world=current;clock=0;}}
    public static void emit(World level,int owner,int kind,Vector3d pos,float radius) {
        if(level==null||!level.isClientSide||kind<0||kind>PikaVisuals.IMPACT||!Float.isFinite(radius)
                ||!Double.isFinite(pos.x)||!Double.isFinite(pos.y)||!Double.isFinite(pos.z))return;
        world(level);Entity entity=level.getEntity(owner);
        boolean follows=entity!=null&&(kind==PikaVisuals.CHARGE||kind==PikaVisuals.CUT_CHARGE||kind==PikaVisuals.LOGIA);
        int life=kind==PikaVisuals.IMPACT?24:kind==PikaVisuals.TRAVEL?18:kind==PikaVisuals.FLASH?12:12;
        for(Light light:ACTIVE)if(light.owner==owner&&light.kind==kind&&(follows||light.pos.distanceToSqr(pos)<2.25)) {
            light.expiry=clock+life;light.radius=Math.max(light.radius,Math.min(24,radius));
            light.follows=follows;light.offset=Vector3d.ZERO;light.pos=pos;return;
        }
        if(ACTIVE.size()>=LIMIT)ACTIVE.remove(0);
        Light light=new Light();light.owner=owner;light.kind=kind;light.pos=pos;light.radius=MathHelper.clamp(radius,.1F,24);
        light.offset=Vector3d.ZERO;light.follows=follows;
        light.born=clock;light.expiry=clock+life;ACTIVE.add(light);
    }
    public static Vector3d interpolate(Entity e,float p){return new Vector3d(MathHelper.lerp(p,e.xOld,e.getX()),MathHelper.lerp(p,e.yOld,e.getY()),MathHelper.lerp(p,e.zOld,e.getZ()));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getInstance();world(mc.level);
        if(world==null||mc.isPaused())return;clock++;
        ACTIVE.removeIf(v->clock>=v.expiry);
        if(!ACTIVE.isEmpty()&&mc.getCameraEntity()!=null) {
            if(anchor==null||!anchor.isAlive()) {
                anchor=new PikaRenderAnchor(net.kazi.kazimod.init.KaziEntities.PIKA_RENDER_ANCHOR.get(),world);
                anchor.setPos(mc.getCameraEntity().getX(),mc.getCameraEntity().getY(),mc.getCameraEntity().getZ());
                anchor.xOld=anchor.getX();anchor.yOld=anchor.getY();anchor.zOld=anchor.getZ();
                mc.level.putNonPlayerEntity(nextId++,anchor);
            }
            anchor.setPos(mc.getCameraEntity().getX(),mc.getCameraEntity().getY(),mc.getCameraEntity().getZ());
        } else if(anchor!=null){mc.level.removeEntity(anchor.getId());anchor=null;}
    }
    public static void render(MatrixStack stack,IRenderTypeBuffer buffers,float p,Vector3d origin,ClippingHelper frustum) {
        Minecraft mc=Minecraft.getInstance();if(world==null||world!=mc.level)return;
        Quaternion camera=mc.getEntityRenderDispatcher().cameraOrientation();Vector3d eye=mc.gameRenderer.getMainCamera().getPosition();int detailed=0;
        for(Light light:ACTIVE) {
            Entity owner=light.follows?world.getEntity(light.owner):null;
            if(light.follows&&(owner==null||!owner.isAlive()))continue;
            Vector3d pos=owner==null?light.pos:interpolate(owner,p).add(light.offset);
            double distance=pos.distanceToSqr(eye);if(distance>160*160)continue;
            float r=light.kind==PikaVisuals.IMPACT?light.radius:3;
            if(frustum!=null&&!frustum.isVisible(new AxisAlignedBB(pos,pos).inflate(r*3)))continue;
            float age=clock-light.born+p,alpha=MathHelper.clamp((light.expiry-clock-p)/8,0,1);
            if(mc.player!=null&&owner==mc.player&&mc.options.getCameraType().isFirstPerson())alpha*=.08F;
            boolean far=distance>48*48||detailed++>=10;
            stack.pushPose();stack.translate(pos.x-origin.x,pos.y-origin.y,pos.z-origin.z);
            switch(light.kind) {
                case PikaVisuals.CHARGE:case PikaVisuals.CUT_CHARGE:
                    PikaMesh.charge(stack,buffers,camera,age,alpha,light.kind==PikaVisuals.CUT_CHARGE,far);break;
                case PikaVisuals.IMPACT:PikaMesh.burst(stack,buffers,camera,light.radius,age,alpha,far);break;
                case PikaVisuals.TRAVEL:
                    stack.translate(0,1,0);PikaMesh.burst(stack,buffers,camera,1.8F,age,alpha,far);break;
                case PikaVisuals.CUT_TRAIL:
                    stack.translate(0,.2,0);PikaMesh.flare(stack,buffers,camera,1.2F,alpha*.5F,45,far);break;
                case PikaVisuals.LOGIA:
                    PikaMesh.charge(stack,buffers,camera,age,alpha*.55F,false,true);
                    if(owner!=null&&owner.getDeltaMovement().lengthSqr()>.02) {
                        Vector3d velocity=owner.getDeltaMovement().normalize().scale(-1);
                        orient(stack,velocity);PikaMesh.beam(stack,buffers,.3F,(float)Math.min(6,owner.getDeltaMovement().length()*3),age,alpha*.5F,true,true);
                    }break;
                default:PikaMesh.flare(stack,buffers,camera,light.kind==PikaVisuals.JEWEL?2:1.5F,alpha,0,far);
            }
            stack.popPose();
        }
    }
    public static void orient(MatrixStack stack,Vector3d direction) {
        Vector3d d=direction.normalize();
        stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(d.x,d.z))));
        stack.mulPose(Vector3f.XP.rotationDegrees((float)-Math.toDegrees(Math.asin(MathHelper.clamp(d.y,-1,1)))));
    }
}
