package net.kazi.kazimod.mochi;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.MrMagicalCart.cartaddon.blocks.MochiBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod",value=Dist.CLIENT)
public final class MochiEffects {
    static final int LIMIT=48;
    private static final List<Effect> ACTIVE=new ArrayList<>();
    private static final List<BlockPos> TERRAIN=new ArrayList<>();
    private static World world;private static int clock,details;
    private static final class Effect {int owner,kind,born,expiry;Vector3d pos;float radius;}
    private static void world(World current){if(current!=world){ACTIVE.clear();TERRAIN.clear();world=current;clock=0;details=0;}}
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){if(event.phase==TickEvent.Phase.START)details=0;}
    public static boolean detailed(Entity entity){return entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())<36*36&&details++<6;}
    public static Vector3d interpolate(Entity e,float p){return new Vector3d(MathHelper.lerp(p,e.xOld,e.getX()),MathHelper.lerp(p,e.yOld,e.getY()),MathHelper.lerp(p,e.zOld,e.getZ()));}
    public static void emit(World level,int owner,int kind,Vector3d pos,float radius) {
        if(level==null||!level.isClientSide||kind<0||kind>MochiVisuals.IMPACT||!Float.isFinite(radius)
                ||!Double.isFinite(pos.x)||!Double.isFinite(pos.y)||!Double.isFinite(pos.z))return;
        world(level);int life=MochiVisuals.follows(kind)?12:kind==MochiVisuals.SLAM?28:24;
        for(Effect e:ACTIVE)if(e.owner==owner&&e.kind==kind&&(MochiVisuals.follows(kind)||e.pos.distanceToSqr(pos)<1)) {
            e.expiry=clock+life;e.pos=pos;return;
        }
        if(ACTIVE.size()>=LIMIT)ACTIVE.remove(0);
        Effect e=new Effect();e.owner=owner;e.kind=kind;e.pos=pos;e.radius=MathHelper.clamp(radius,.15F,16);e.born=clock;e.expiry=clock+life;ACTIVE.add(e);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getInstance();world(mc.level);
        if(world==null||mc.isPaused())return;clock++;ACTIVE.removeIf(e->clock>=e.expiry);
        if(clock%10==0&&mc.getCameraEntity()!=null) {
            TERRAIN.clear();BlockPos center=mc.getCameraEntity().blockPosition();
            scan:for(int ring=0;ring<=8;ring++)for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=ring)continue;
                for(int y=-4;y<=3;y++) {
                    BlockPos pos=center.offset(x,y,z);
                    if(world.hasChunkAt(pos)&&world.getBlockState(pos).getBlock() instanceof MochiBlock&&world.getBlockState(pos.above()).isAir()) {
                        TERRAIN.add(pos);if(TERRAIN.size()>=128)break scan;
                    }
                }
            }
        }
    }
    @SubscribeEvent public static void render(RenderWorldLastEvent event) {
        Minecraft mc=Minecraft.getInstance();if(world==null||world!=mc.level||(ACTIVE.isEmpty()&&TERRAIN.isEmpty()))return;
        MatrixStack s=event.getMatrixStack();Vector3d camera=mc.gameRenderer.getMainCamera().getPosition();
        IRenderTypeBuffer.Impl buffers=mc.renderBuffers().bufferSource();float p=event.getPartialTicks();int close=0;
        for(Effect e:ACTIVE) {
            Entity owner=MochiVisuals.follows(e.kind)?world.getEntity(e.owner):null;
            if(owner!=null&&(!owner.isAlive()||owner.isInvisible()||(owner==mc.player&&mc.options.getCameraType().isFirstPerson())))continue;
            Vector3d pos=owner==null?e.pos:interpolate(owner,p);
            if(e.kind==MochiVisuals.FLOW&&world.getBlockState(new BlockPos(pos)).getBlock() instanceof MochiBlock)
                pos=new Vector3d(pos.x,Math.floor(pos.y)+1.03,pos.z);
            double dist=pos.distanceToSqr(camera);if(dist>96*96)continue;
            float age=clock-e.born+p;
            float shrink=MathHelper.clamp((e.expiry-clock-p)/6,0,1);
            s.pushPose();s.translate(pos.x-camera.x,pos.y-camera.y,pos.z-camera.z);s.scale(shrink,shrink,shrink);
            MochiMesh mesh=new MochiMesh(s,buffers,age,dist>32*32||close++>=6,WorldRenderer.getLightColor(world,new BlockPos(pos)));
            if(MochiVisuals.follows(e.kind)) {
                double h=owner==null?1.8:owner.getBbHeight(),r=owner==null?.5:owner.getBbWidth()*.65;
                for(int j=0;j<3;j++) {
                    double y=h*(.22+j*.25);
                    mesh.strand(new Vector3d(-r,y,0),new Vector3d(-r,y+.3,r),new Vector3d(r,y-.2,r),new Vector3d(r,y,0),.13,.10,MochiMesh.CREAM);
                }
                mesh.drips(r,h*.5,4,e.owner);
            } else if(e.kind==MochiVisuals.BARRAGE) {
                mesh.blob(Vector3d.ZERO,e.radius,.15,e.radius,MochiMesh.CREAM);mesh.drips(e.radius,-.08,6,e.owner);
            } else mesh.splash(e.radius,MathHelper.clamp(age/(e.kind==MochiVisuals.SLAM?28:24),0,1),e.owner);
            s.popPose();
        }
        for(BlockPos pos:TERRAIN) {
            if(!(world.getBlockState(pos).getBlock() instanceof MochiBlock)||!world.getBlockState(pos.above()).isAir())continue;
            s.pushPose();s.translate(pos.getX()-camera.x,pos.getY()+1-camera.y,pos.getZ()-camera.z);
            new MochiMesh(s,buffers,(world.getGameTime()%100000)+p,true,WorldRenderer.getLightColor(world,pos.above())).tile(pos.getX(),pos.getZ());
            s.popPose();
        }
        buffers.endBatch(MochiMesh.SURFACE);
    }
}
