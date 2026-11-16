package net.kazi.kazimod.yami;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.*;
import net.minecraft.block.Block;
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
import xyz.pixelatedw.mineminenomi.init.ModBlocks;

/** Client-local volumes replace existing effect packets without spawning particle entities. */
@Mod.EventBusSubscriber(modid="kazimod",value=Dist.CLIENT)
public final class YamiEffects {
    public static final int POOL=0, CHARGE=1, PULL=2, HAND=3, BURST=4, WORLD=5, RELEASE=6, ABSORB=7, COAT=8;
    public static final int PORTAL=10,TORNADO=11;
    private static final int LIMIT=32, DEBRIS_LIMIT=16;
    private static final List<Volume> ACTIVE=new ArrayList<>();
    private static final Volume[] VISIBLE=new Volume[LIMIT];
    private static World world;
    private static int clock;
    private static YamiRenderAnchor anchorEntity;
    private static int nextAnchorId=-2000000000;
    private static final List<Debris> DEBRIS=new ArrayList<>();
    private static final class Debris {
        Vector3d start,end;int born,state;
    }
    private static final class Volume {
        int kind,owner,born,expiry,seed;
        Vector3d position,previous;
        float radius;
        BlockPos anchor;
        boolean follows;
        Vector3d framePosition;
        float frameAge,frameAlpha;
        boolean frameFar;
    }
    private static void world(World current) {
        if(current!=world) {ACTIVE.clear();DEBRIS.clear();anchorEntity=null;world=current;clock=0;}
    }
    public static void debris(World level,Vector3d start,Vector3d end,int state) {
        world(level);if(DEBRIS.size()>=DEBRIS_LIMIT)DEBRIS.remove(0);
        Debris d=new Debris();d.start=start;d.end=end;d.state=state;d.born=clock;DEBRIS.add(d);
    }
    public static void emit(World level, Entity owner, int kind, double x,double y,double z) {
        if(level==null || !level.isClientSide)return;
        world(level);
        Vector3d pos=new Vector3d(x,y,z);
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return;
        int id=owner==null?-1:owner.getId();
        boolean follow=owner!=null && (kind==CHARGE || kind==HAND || kind==COAT || kind==PORTAL || kind==TORNADO);
        if(follow)pos=attachment(owner,kind,1);
        BlockPos anchor=kind==POOL?darknessNear(pos):null;
        if(anchor!=null)pos=new Vector3d(pos.x,anchor.getY()+1.025,pos.z);
        float radius=kind==TORNADO?15:kind==PORTAL?2:kind==POOL?3.2F:kind==WORLD?10:kind==RELEASE?4.5F:kind==ABSORB?6:kind==BURST?5:kind==PULL?2.2F:kind==HAND?1.1F:kind==COAT?.7F:1.6F;
        int life=kind==POOL?38:kind==BURST?34:kind==WORLD?32:kind==RELEASE?45:kind==ABSORB?30:kind==COAT?602:16;
        for(Volume v:ACTIVE) if(v.kind==kind && (v.owner==id||kind==POOL) && (follow || v.position.distanceToSqr(pos)<(kind==POOL?36:kind==PULL?36:4))) {
            v.expiry=clock+life;
            if(kind==PULL) v.position=pos;
            if(anchor!=null)v.anchor=anchor;
            return;
        }
        if(ACTIVE.size()>=LIMIT) {
            Volume oldest=null;
            for(Volume candidate:ACTIVE)if(candidate.kind==POOL&&(oldest==null||candidate.expiry<oldest.expiry))oldest=candidate;
            if(kind==POOL) {
                Entity camera=Minecraft.getInstance().getCameraEntity();
                if(camera==null||oldest==null)return;
                for(Volume candidate:ACTIVE)if(candidate.kind==POOL&&candidate.position.distanceToSqr(camera.position())>oldest.position.distanceToSqr(camera.position()))oldest=candidate;
                if(pos.distanceToSqr(camera.position())>=oldest.position.distanceToSqr(camera.position()))return;
            }
            if(oldest!=null)ACTIVE.remove(oldest);
            else if(kind==POOL)return;
            else ACTIVE.remove(0);
        }
        Volume v=new Volume();v.kind=kind;v.owner=id;v.position=v.previous=pos;v.radius=radius;
        v.born=clock;v.expiry=clock+life;v.seed=Objects.hash(id,kind,new BlockPos(pos));v.follows=follow;
        v.anchor=anchor;
        ACTIVE.add(v);
    }
    public static void stopCoat(World level,int owner) {
        stop(level,owner,COAT);
    }
    public static void stop(World level,int owner,int kind) {
        world(level);for(Volume v:ACTIVE)if(v.kind==kind&&v.owner==owner)v.expiry=Math.min(v.expiry,clock+6);
    }
    private static boolean dark(BlockPos pos) {
        if(world==null||!world.hasChunkAt(pos))return false;
        Block block=world.getBlockState(pos).getBlock();
        return block==ModBlocks.DARKNESS.get() || block==net.MrMagicalCart.cartaddon.init.CartBlocks.REWORKED_DARKNESS_BLOCK.get();
    }
    private static BlockPos darknessNear(Vector3d p) {
        BlockPos pos=new BlockPos(p);
        for(int dy=1;dy>=-2;dy--)if(dark(pos.offset(0,dy,0)))return pos.offset(0,dy,0);
        return null;
    }
    private static Vector3d attachment(Entity owner,int kind,float partial) {
        Vector3d p=interpolate(owner,partial);
        if(kind==CHARGE||kind==TORNADO)return p;
        if(kind==PORTAL&&owner instanceof net.minecraft.entity.LivingEntity)
            return YamiCombat.portal((net.minecraft.entity.LivingEntity)owner).add(p.subtract(owner.position()));
        double yaw=Math.toRadians(MathHelper.rotLerp(partial,owner.yRotO,owner.yRot));
        Vector3d forward=owner.getViewVector(partial);
        return p.add(0,owner.getBbHeight()*.69,0).add(forward.scale(.65))
                .add(Math.cos(yaw)*(kind==HAND?.33:-.33),0,Math.sin(yaw)*(kind==HAND?.33:-.33));
    }
    static Vector3d interpolate(Entity e,float p) {
        return new Vector3d(MathHelper.lerp(p,e.xOld,e.getX()),MathHelper.lerp(p,e.yOld,e.getY()),MathHelper.lerp(p,e.zOld,e.getZ()));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();world(mc.level);
        if(world==null||mc.isPaused())return;
        clock++;
        DEBRIS.removeIf(d->clock-d.born>=20);
        if((!ACTIVE.isEmpty()||!DEBRIS.isEmpty())&&mc.getCameraEntity()!=null) {
            if(anchorEntity==null||!anchorEntity.isAlive()||world.getEntity(anchorEntity.getId())!=anchorEntity) {
                anchorEntity=new YamiRenderAnchor(net.kazi.kazimod.init.KaziEntities.YAMI_RENDER_ANCHOR.get(),world);
                anchorEntity.setPos(mc.getCameraEntity().getX(),mc.getCameraEntity().getY(),mc.getCameraEntity().getZ());
                anchorEntity.xOld=anchorEntity.getX();anchorEntity.yOld=anchorEntity.getY();anchorEntity.zOld=anchorEntity.getZ();
                mc.level.putNonPlayerEntity(nextAnchorId++,anchorEntity);
            }
            anchorEntity.setPos(mc.getCameraEntity().getX(),mc.getCameraEntity().getY(),mc.getCameraEntity().getZ());
        } else if(anchorEntity!=null) {
            mc.level.removeEntity(anchorEntity.getId());anchorEntity=null;
        }
        Iterator<Volume> it=ACTIVE.iterator();
        while(it.hasNext()) {
            Volume v=it.next();v.previous=v.position;
            if(v.follows) {
                Entity owner=world.getEntity(v.owner);
                if(owner==null||!owner.isAlive())v.expiry=Math.min(v.expiry,clock+5);
                else v.position=attachment(owner,v.kind,1);
            }
            if(v.kind==POOL&&clock%10==0&&v.anchor!=null) {
                if(dark(v.anchor)&&!world.getBlockState(v.anchor.above()).getMaterial().isSolidBlocking())v.expiry=clock+20;
                else v.anchor=null;
            }
            if(clock>=v.expiry)it.remove();
        }
        if(clock%5==0&&mc.getCameraEntity()!=null) {
            Vector3d camera=mc.getCameraEntity().position();
            ACTIVE.sort(Comparator.comparingDouble(v->v.position.distanceToSqr(camera)));
        }
    }
    public static void render(MatrixStack stack,IRenderTypeBuffer buffers,float p,Vector3d origin) {
        render(stack,buffers,p,origin,null);
    }
    public static void render(MatrixStack stack,IRenderTypeBuffer buffers,float p,Vector3d origin,ClippingHelper frustum) {
        Minecraft mc=Minecraft.getInstance();if(world==null||world!=mc.level)return;
        Vector3d camera=mc.gameRenderer.getMainCamera().getPosition();
        Quaternion rotation=mc.getEntityRenderDispatcher().cameraOrientation();
        int detailed=0,count=0;
        for(Volume v:ACTIVE) {
            Vector3d pos=v.previous.add(v.position.subtract(v.previous).scale(p));
            Entity owner=v.follows?world.getEntity(v.owner):null;
            if(v.kind==PORTAL&&owner==mc.player&&mc.options.getCameraType().isFirstPerson())continue;
            if(owner!=null)pos=attachment(owner,v.kind,p);
            double distance=camera.distanceToSqr(pos);
            if(distance>96*96)continue;
            if(frustum!=null&&!frustum.isVisible(new AxisAlignedBB(pos,pos).inflate(v.radius*4)))continue;
            boolean far=distance>32*32 || detailed++>=6;
            float age=clock-v.born+p;
            float alpha=Math.min(1,age/5)*Math.min(1,(v.expiry-clock-p)/10);
            if(owner==mc.player&&mc.options.getCameraType().isFirstPerson())alpha*=v.kind==CHARGE?.12F:v.kind==TORNADO?.22F:.4F;
            if(alpha<=.005F)continue;
            v.framePosition=pos;v.frameAge=age;v.frameAlpha=alpha;v.frameFar=far;VISIBLE[count++]=v;
        }
        // One ink pass and one smoke pass, instead of flushing both buffers for every volume.
        for(int pass=0;pass<2;pass++)for(int i=0;i<count;i++) {
            Volume v=VISIBLE[i];Vector3d pos=v.framePosition;
            stack.pushPose();stack.translate(pos.x-origin.x,pos.y-origin.y,pos.z-origin.z);
            if(v.kind==PORTAL) {
                Entity owner=world.getEntity(v.owner);
                if(owner!=null) {
                    Vector3d direction=owner.getViewVector(p);
                    stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(direction.x,direction.z))));
                    stack.mulPose(Vector3f.XP.rotationDegrees((float)Math.toDegrees(Math.acos(MathHelper.clamp(direction.y,-1,1)))));
                }
            }
            if(pass==0)YamiMesh.effectGeometry(stack,buffers,v.kind,v.radius,v.frameAge,v.frameAlpha,v.seed,v.frameFar);
            else YamiMesh.effectSmoke(stack,buffers,rotation,v.kind,v.radius,v.frameAge,v.frameAlpha,v.seed,v.frameFar);
            stack.popPose();
        }
        Arrays.fill(VISIBLE,0,count,null);
        for(Debris d:DEBRIS) {
            if(camera.distanceToSqr(d.start)>32*32)continue;
            float t=MathHelper.clamp((clock-d.born+p)/20,0,1);
            Vector3d pos=d.start.add(d.end.subtract(d.start).scale(t*t)).add(0,Math.sin(t*Math.PI)*1.8,0);
            float scale=.8F*(1-t*.9F);
            stack.pushPose();stack.translate(pos.x-origin.x,pos.y-origin.y,pos.z-origin.z);
            stack.mulPose(Vector3f.YP.rotationDegrees(t*240));stack.mulPose(Vector3f.XP.rotationDegrees(t*130));
            stack.scale(scale,scale,scale);stack.translate(-.5,-.5,-.5);
            mc.getBlockRenderer().renderSingleBlock(Block.stateById(d.state),stack,buffers,15728880,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            stack.popPose();
        }
    }
}
