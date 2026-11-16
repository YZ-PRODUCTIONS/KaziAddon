package net.kazi.kazimod.moku;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod",value=Dist.CLIENT)
public final class MokuEffects {
    private static final int LIMIT=32;
    private static final List<Smoke> ACTIVE=new ArrayList<>();
    private static final Smoke[] VISIBLE=new Smoke[LIMIT];
    private static World world;private static int clock,nextId=-1800000000,projectileDetails;private static MokuRenderAnchor anchor;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){if(event.phase==TickEvent.Phase.START)projectileDetails=0;}
    public static boolean projectileDetail(){return projectileDetails++<6;}
    private static final class Smoke {
        int owner,kind,born,expiry;float radius;Vector3d position;boolean follows;
        final List<Vector3d> trail=new ArrayList<>(14);
        Vector3d framePosition;float age,alpha;boolean far;
    }
    private static void world(World current){if(current!=world){ACTIVE.clear();world=current;clock=0;anchor=null;}}
    public static void emit(World level,int owner,int kind,Vector3d pos,float radius) {
        if(level==null||!level.isClientSide||kind<0||kind>MokuVisuals.BURST||!Float.isFinite(radius)
                ||!Double.isFinite(pos.x)||!Double.isFinite(pos.y)||!Double.isFinite(pos.z))return;
        world(level);boolean follow=MokuVisuals.follows(kind);int life=kind==MokuVisuals.DEBUFF?24:kind==MokuVisuals.WORLD?18:kind==MokuVisuals.STRIKE?22:18;
        for(Smoke v:ACTIVE)if(v.kind==kind&&v.owner==owner&&(follow||v.position.distanceToSqr(pos)<4)) {
            v.expiry=clock+life;v.radius=MathHelper.clamp(radius,.2F,32);v.position=pos;return;
        }
        if(kind==MokuVisuals.WORLD&&ACTIVE.stream().filter(v->v.kind==MokuVisuals.WORLD).count()>=4)return;
        if(ACTIVE.size()>=LIMIT)ACTIVE.remove(0);
        Smoke v=new Smoke();v.owner=owner;v.kind=kind;v.radius=MathHelper.clamp(radius,.2F,32);v.position=pos;v.follows=follow;
        v.born=clock;v.expiry=clock+life;ACTIVE.add(v);
    }
    public static Vector3d interpolate(Entity e,float p){return new Vector3d(MathHelper.lerp(p,e.xOld,e.getX()),MathHelper.lerp(p,e.yOld,e.getY()),MathHelper.lerp(p,e.zOld,e.getZ()));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getInstance();world(mc.level);
        if(world==null||mc.isPaused())return;clock++;
        ACTIVE.removeIf(v->clock>=v.expiry);
        for(Smoke v:ACTIVE)if(v.follows) {
            Entity e=world.getEntity(v.owner);
            if(e!=null&&e.isAlive()) {
                v.position=e.position();
                if(v.kind==MokuVisuals.FLIGHT) {
                    Vector3d point=e.position().add(0,-.45,0);
                    if(v.trail.isEmpty()||v.trail.get(0).distanceToSqr(point)>.015){
                        if(!v.trail.isEmpty()&&v.trail.get(0).distanceToSqr(point)>32*32)v.trail.clear();
                        v.trail.add(0,point);if(v.trail.size()>14)v.trail.remove(v.trail.size()-1);
                    }
                }
            }
        }
        if(!ACTIVE.isEmpty()&&mc.getCameraEntity()!=null) {
            if(anchor==null||!anchor.isAlive()) {
                anchor=new MokuRenderAnchor(net.kazi.kazimod.init.KaziEntities.MOKU_RENDER_ANCHOR.get(),world);
                anchor.setPos(mc.getCameraEntity().getX(),mc.getCameraEntity().getY(),mc.getCameraEntity().getZ());
                anchor.xOld=anchor.getX();anchor.yOld=anchor.getY();anchor.zOld=anchor.getZ();mc.level.putNonPlayerEntity(nextId++,anchor);
            }
            anchor.setPos(mc.getCameraEntity().getX(),mc.getCameraEntity().getY(),mc.getCameraEntity().getZ());
        } else if(anchor!=null){mc.level.removeEntity(anchor.getId());anchor=null;}
        if(clock%5==0&&mc.getCameraEntity()!=null) {
            Vector3d camera=mc.getCameraEntity().position();ACTIVE.sort(Comparator.comparingDouble(v->-v.position.distanceToSqr(camera)));
        }
    }
    public static void render(MatrixStack s,IRenderTypeBuffer b,float p,Vector3d origin,ClippingHelper frustum) {
        Minecraft mc=Minecraft.getInstance();if(world==null||world!=mc.level)return;
        Vector3d eye=mc.gameRenderer.getMainCamera().getPosition();Quaternion camera=mc.getEntityRenderDispatcher().cameraOrientation();int count=0,detailed=0;
        // Pick close-up detail from near to far, then draw transparent volumes back to front.
        for(int i=ACTIVE.size()-1;i>=0;i--) {
            Smoke v=ACTIVE.get(i);Entity owner=v.follows?world.getEntity(v.owner):null;
            Vector3d pos=owner!=null&&owner.isAlive()?interpolate(owner,p):v.position;
            double distance=eye.distanceToSqr(pos);if(distance>128*128)continue;
            AxisAlignedBB box=new AxisAlignedBB(pos,pos).inflate(v.kind==MokuVisuals.WORLD?v.radius*1.7:Math.max(4,v.radius*3));
            if(frustum!=null&&!frustum.isVisible(box))continue;
            float alpha=MathHelper.clamp((clock-v.born+p)/5,0,1)*MathHelper.clamp((v.expiry-clock-p)/10,0,1);
            if(v.follows&&owner!=null&&!owner.isAlive())alpha=0;
            if(mc.player!=null&&owner==mc.player&&mc.options.getCameraType().isFirstPerson())alpha*=v.kind==MokuVisuals.WORLD?.25F:.08F;
            if(alpha<=0)continue;
            v.framePosition=pos;v.age=clock-v.born+p;v.alpha=alpha;v.far=distance>40*40||detailed++>=4;VISIBLE[count++]=v;
        }
        for(int pass=0;pass<2;pass++)for(int i=count-1;i>=0;i--) {
            Smoke v=VISIBLE[i];Vector3d pos=v.framePosition;
            s.pushPose();s.translate(pos.x-origin.x,pos.y-origin.y,pos.z-origin.z);
            MokuMesh.volume(s,b,camera,v.kind,v.radius,v.age,v.alpha,v.owner,v.far,pass);
            if(v.kind==MokuVisuals.FLIGHT)MokuMesh.trail(s,b,camera,v.trail,pos,.75F,v.age,v.alpha,v.far,pass);
            s.popPose();
        }
    }
    public static final class Renderer extends EntityRenderer<MokuRenderAnchor> {
        private ClippingHelper frustum;
        public Renderer(EntityRendererManager manager){super(manager);}
        @Override public void render(MokuRenderAnchor e,float yaw,float p,MatrixStack s,IRenderTypeBuffer b,int light){MokuEffects.render(s,b,p,interpolate(e,p),frustum);}
        @Override public boolean shouldRender(MokuRenderAnchor e,ClippingHelper frustum,double x,double y,double z){this.frustum=frustum;return true;}
        @Override public ResourceLocation getTextureLocation(MokuRenderAnchor e){return MokuMesh.TEXTURE;}
    }
}
