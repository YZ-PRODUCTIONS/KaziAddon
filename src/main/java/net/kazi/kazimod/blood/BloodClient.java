package net.kazi.kazimod.blood;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class BloodClient {
    private static final IRenderTypeBuffer.Impl BUFFER=IRenderTypeBuffer.immediate(new BufferBuilder(262144));
    private static final Map<Integer,Pending> PENDING=new LinkedHashMap<>();
    private static World world;
    private static int handDepth;
    public static void init(IEventBus bus){
        MinecraftForge.EVENT_BUS.addListener(BloodHands::render);
        bus.addListener((FMLClientSetupEvent e)->{
            RenderingRegistry.registerEntityRenderingHandler(BloodFeatures.PROJECTILE.get(),ShotRenderer::new);
            e.enqueueWork(()->Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().values().forEach(r->r.addLayer(new BodyLayer(r))));
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e)->{
            if(e.phase!=TickEvent.Phase.END)return;world();if(world==null)return;
            Iterator<Map.Entry<Integer,Pending>> it=PENDING.entrySet().iterator();
            while(it.hasNext()){Map.Entry<Integer,Pending> item=it.next();Entity p=world.getEntity(item.getKey());
                if(p instanceof LivingEntity){p.getPersistentData().put(BloodFeatures.TAG,item.getValue().data);it.remove();}
                else if(world.getGameTime()>item.getValue().until)it.remove();}
        });
    }
    private static void world(){World next=Minecraft.getInstance().level;if(world!=next){world=next;PENDING.clear();handDepth=0;}}
    public static void receive(BloodPacket packet){
        world();if(world==null)return;Entity p=world.getEntity(packet.id);
        if(p instanceof LivingEntity)p.getPersistentData().put(BloodFeatures.TAG,packet.data.copy());
        else{if(PENDING.size()>=64)PENDING.remove(PENDING.keySet().iterator().next());PENDING.put(packet.id,new Pending(packet.data.copy(),world.getGameTime()+40));}
    }
    public static void beginHand(){handDepth++;} public static void endHand(){handDepth=Math.max(0,handDepth-1);}
    public static boolean pose(PlayerModel<?> m,LivingEntity p,float time){return handDepth==0&&BloodPose.apply(m,p,time);}
    public static void render(MatrixStack stack,ActiveRenderInfo camera,float partial,Matrix4f projection){
        world();if(world==null)return;Minecraft mc=Minecraft.getInstance();Vector3d eye=camera.getPosition();
        IVertexBuilder out=BUFFER.getBuffer(BloodMesh.TYPE);int count=0;
        for(Entity e:mc.level.entitiesForRendering()){
            if(!e.isAlive()||e.distanceToSqr(eye)>96*96)continue;
            if(e instanceof BloodProjectile){if(count++>=32)continue;renderShot((BloodProjectile)e,stack.last().pose(),out,eye,partial);}
            else if(e instanceof LivingEntity){
                LivingEntity p=(LivingEntity)e;
                if(p==mc.getCameraEntity()&&mc.options.getCameraType().isFirstPerson())continue;
                int action=BloodFeatures.action(p);if(action!=0)continue;
                CompoundNBT n=BloodFeatures.state(p);float age=BloodPose.age(p,partial);
                if(n.getLong("Released")>=n.getLong("Start"))continue;
                Vector3d origin=p.getEyePosition(partial).add(p.getLookAngle().scale(.65)).add(0,-.35,0).subtract(eye);
                BloodEffects.convergence(out,stack.last().pose(),origin,p.getLookAngle(),age,BloodFeatures.scale(p)?22:30,1);
            }
        }
        mc.getMainRenderTarget().bindWrite(false);BUFFER.endBatch();
        RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableTexture();RenderSystem.defaultBlendFunc();RenderSystem.color4f(1,1,1,1);
    }
    private static void renderShot(BloodProjectile f,Matrix4f m,IVertexBuilder out,Vector3d eye,float partial){
        Vector3d c=f.visualPosition(partial).subtract(eye),dir=f.visualDirection();
        float age=f.age(partial),phaseAge=f.phaseAge(partial),fade=f.phase()==2?1-BloodMesh.smooth(phaseAge/12):1;
        if(f.kind()==0){
            Vector3d start=f.origin().subtract(eye);
            if(f.steered()&&f.phase()==1){
                dir=f.beamDirection(partial);
                start=f.beamStart(partial).subtract(eye);
                c=f.beamEnd(partial).subtract(eye);
                start=start.add(dir.scale(Math.min(.65,start.distanceTo(c)*.5)));
            }
            Minecraft mc=Minecraft.getInstance();
            if(f.getThrower()==mc.player&&mc.getCameraEntity()==mc.player&&mc.options.getCameraType().isFirstPerson())
                start=start.add(0,.295,0).add(dir.scale(.38));
            BloodEffects.piercing(out,m,start,c,dir,age,phaseAge,f.phase()==2);
        }else if(f.kind()==1){
            BloodEffects.supernova(out,m,c,dir,age,f.phase(),phaseAge);
        }else if(f.kind()==2){BloodMesh.disc(out,m,c,dir,age,fade);}
        else if(f.kind()==4){
            LivingEntity p=f.getThrower();
            if(p!=null){Vector3d start=p.getEyePosition(partial).add(p.getLookAngle().scale(.45)).add(0,-.3,0).subtract(eye);BloodMesh.tether(out,m,start,c,age,1);}
            BloodMesh.fist(out,m,c,dir,1);
        }
    }
    private static final class BodyLayer extends LayerRenderer<AbstractClientPlayerEntity,PlayerModel<AbstractClientPlayerEntity>>{
        BodyLayer(PlayerRenderer r){super(r);}
        @Override public void render(MatrixStack s,IRenderTypeBuffer b,int light,AbstractClientPlayerEntity p,float w,float a,float partial,float time,float yaw,float pitch){
            if(p.isInvisible()||!p.isAlive())return;
            if(BloodFeatures.scale(p)){
                s.pushPose();getParentModel().head.translateAndRotate(s);s.scale(.0625F,.0625F,.0625F);
                IVertexBuilder out=b.getBuffer(BloodMesh.TYPE);Matrix4f m=s.last().pose();
                BloodMesh.quad(out,m,new Vector3d(-3.6,-3.6,-4.06),new Vector3d(3.6,-3.6,-4.06),new Vector3d(3.6,-2.85,-4.06),new Vector3d(-3.6,-2.85,-4.06),.18F,1);
                for(int side:new int[]{-1,1})for(int i=0;i<3;i++){
                    double x=side*(1+i*.7),y=-3.3+i*.55;
                    BloodMesh.quad(out,m,new Vector3d(x,y,-4.07),new Vector3d(x+side*.55,y+.4,-4.07),new Vector3d(x+side*.65,y+1.2,-4.07),new Vector3d(x+side*.2,y+.55,-4.07),.35F,1);
                }
                s.popPose();
            }
        }
    }
    private static final class ShotRenderer extends EntityRenderer<BloodProjectile>{
        ShotRenderer(EntityRendererManager m){super(m);shadowRadius=0;}
        @Override public ResourceLocation getTextureLocation(BloodProjectile p){return new ResourceLocation("kazimod","textures/items/chi_chi_no_mi.png");}
        @Override public void render(BloodProjectile p,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){}
    }
    private static final class Pending{final CompoundNBT data;final long until;Pending(CompoundNBT data,long until){this.data=data;this.until=until;}}
    private BloodClient(){}
}
