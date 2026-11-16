package net.kazi.kazimod.ghoul;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.kazi.kazimod.tusk.TuskClient;

public final class GhoulClient {
    public static final class Tube { public int limb; public float[][][] quads; }
    public static final class Asset { public CerberusRig.Node[] nodes; public CerberusRig.Clip[] animations; public Tube[] tubes; private CerberusRig rig; }
    private static final Asset[] ASSETS=new Asset[4];
    private static final Map<PlayerEntity,Visual> VISUALS=new WeakHashMap<>();
    private static final ResourceLocation EYE=new ResourceLocation("kazimod","textures/entities/ghoul_eye.png");
    private static final ModelRenderer EYE_MODEL=new ModelRenderer(64,64,0,0);
    private static final class Visual { float time,deploy; int type; }
    static { EYE_MODEL.addBox(-4,-8,-4,8,8,8,.025F); }
    public static void init(IEventBus bus){
        bus.addListener((FMLClientSetupEvent e)->{
            RenderingRegistry.registerEntityRenderingHandler(GhoulFeatures.SHARD.get(),CrystalRenderer::new);
            e.enqueueWork(()->Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().values().forEach(r->r.addLayer(new KaguneLayer(r))));
        });
        MinecraftForge.EVENT_BUS.addListener(GhoulClient::hud);
        MinecraftForge.EVENT_BUS.addListener(GhoulClient::world);
    }
    public static void receive(GhoulPacket packet){Minecraft mc=Minecraft.getInstance();if(mc.level==null||packet.data==null)return;
        if(packet.data.contains("Fx")){GhoulVfx.receive(packet.data);return;}
        Entity e=mc.level.getEntity(packet.entity);if(e instanceof PlayerEntity)e.getPersistentData().put(GhoulState.TAG,packet.data);
    }
    public static Asset asset(int type){if(ASSETS[type]==null)try(InputStream s=GhoulClient.class.getResourceAsStream("/assets/kazimod/models/entities/ghoul_"+GhoulState.TYPES[type]+".json")){
        if(s==null)throw new IOException("Missing kagune");Asset a=new Gson().fromJson(new InputStreamReader(s,StandardCharsets.UTF_8),Asset.class);a.rig=new CerberusRig();a.rig.nodes=a.nodes;a.rig.animations=a.animations;ASSETS[type]=a;
    }catch(IOException e){throw new IllegalStateException(e);}return ASSETS[type];}
    public static CerberusRig rig(int type){return asset(type).rig;}
    public static ResourceLocation texture(int type){return new ResourceLocation("kazimod","textures/entities/ghoul_"+GhoulState.TYPES[type]+".png");}
    private static float deployment(PlayerEntity p,float partial){
        Visual v=VISUALS.computeIfAbsent(p,k->new Visual());float now=p.tickCount+partial,dt=MathHelper.clamp(now-v.time,0,2);v.time=now;
        int type=MathHelper.clamp(GhoulState.get(p).getInt("Type"),0,3);if(type!=v.type){v.deploy=0;v.type=type;}
        float target=GhoulState.get(p).getBoolean("Ghoul")&&GhoulState.active(p)&&p.isAlive()?1:0;
        v.deploy=MathHelper.clamp(v.deploy+(target>v.deploy?dt/8:-dt/8),target==0?0:v.deploy,target==0?v.deploy:1);return GhoulMotion.smooth(v.deploy);
    }
    private static final class KaguneLayer extends LayerRenderer<AbstractClientPlayerEntity,PlayerModel<AbstractClientPlayerEntity>> {
        KaguneLayer(PlayerRenderer renderer){super(renderer);}
        @Override public void render(MatrixStack s,IRenderTypeBuffer b,int light,AbstractClientPlayerEntity p,float walk,float amount,float partial,float age,float yaw,float pitch){
            if(p.isInvisible()||p.isSpectator())return;float deploy=deployment(p,partial);if(deploy<.005F)return;
            int type=MathHelper.clamp(GhoulState.get(p).getInt("Type"),0,3),move=GhoulState.action(p);float tick=p.level.getGameTime()-GhoulState.get(p).getLong("Start")+partial;
            s.pushPose();
            if(type==1){getParentModel().rightArm.translateAndRotate(s);s.translate(5/16F,22/16F,0);}else{getParentModel().body.translateAndRotate(s);s.translate(0,1.5,0);}
            s.scale(1/16F,-1/16F,1/16F);s.translate(type==1?-5:0,type==1?22:14,3);s.scale(deploy,deploy,deploy);s.translate(type==1?5:0,type==1?-22:-14,-3);
            draw(type,s,b,light,1,move,tick,p.tickCount+partial,target(p,partial),type==1,MathHelper.clamp(amount*3,0,1));s.popPose();
            if(type==1&&(move==5||move==7)){
                s.pushPose();getParentModel().body.translateAndRotate(s);s.translate(0,1.5,0);s.scale(1/16F,-1/16F,1/16F);
                drawShield(s,b,light,GhoulMotion.smooth(tick/5),1);s.popPose();
            }
            s.pushPose();getParentModel().head.translateAndRotate(s);EYE_MODEL.render(s,b.getBuffer(RenderType.entityTranslucent(EYE)),15728880,OverlayTexture.NO_OVERLAY);s.popPose();
        }
    }
    private static Vector3d target(PlayerEntity p,float partial){
        Entity t=p.level.getEntity(GhoulState.get(p).getInt("Target"));int move=GhoulState.action(p);
        if(t==null&&move!=9&&move!=10&&move!=13&&move!=16)return null;
        Vector3d at=t==null?p.getEyePosition(partial).add(p.getViewVector(partial).scale(move==13?5.25:move==16?3.25:4.25)):t.getPosition(partial).add(0,t.getBbHeight()*.5,0);
        Vector3d world=at.subtract(p.getPosition(partial));float yaw=MathHelper.rotLerp(partial,p.yBodyRotO,p.yBodyRot);
        Vector3d local=world.yRot((float)Math.toRadians(yaw));return new Vector3d(-local.x*16,local.y*16,-local.z*16);
    }
    public static void draw(int type,MatrixStack s,IRenderTypeBuffer b,int light,float alpha,int move,float tick,float clock,Vector3d target,boolean attachedArm){
        draw(type,s,b,light,alpha,move,tick,clock,target,attachedArm,0);
    }
    private static void draw(int type,MatrixStack s,IRenderTypeBuffer b,int light,float alpha,int move,float tick,float clock,Vector3d target,boolean attachedArm,float running){
        CerberusRig r=rig(type);Map<String,CerberusRig.Pose> pose=r.pose();r.apply(pose,"idle",clock/20,move==0?1:.2F);
        if(type==0&&move==0)r.apply(pose,"run",clock/20,running);
        if(move>0&&type<2){if(move==5)r.apply(pose,"guard",tick/20,GhoulMotion.smooth(tick/5));else r.apply(pose,"attack"+((move-1)%4+1),tick/GhoulAbility.LIFE[move],1);}
        if(attachedArm)pose.computeIfAbsent("Armor",k->new CerberusRig.Pose()).rotation[0]=0;
        if(attachedArm){Arrays.fill(pose.get("Armor").rotation,0);}
        IVertexBuilder buffer=b.getBuffer(RenderType.entityTranslucent(texture(type)));
        if(type>=2)drawTubes(type,s,buffer,light,alpha,move,tick,clock,target,running);
        else for(CerberusRig.Node n:r.nodes[0].children)if(!"Shield".equals(n.name))TuskClient.draw(new CerberusRig.Node[]{n},pose,s,buffer,light,alpha);
    }
    private static void drawShield(MatrixStack s,IRenderTypeBuffer b,int light,float unfold,float alpha){
        s.pushPose();s.translate(0,17,-12);s.scale(unfold,1,.6F+unfold*.4F);s.translate(0,-17,12);
        for(CerberusRig.Node n:rig(1).nodes[0].children)if("Shield".equals(n.name))TuskClient.draw(new CerberusRig.Node[]{n},Collections.emptyMap(),s,b.getBuffer(RenderType.entityTranslucent(texture(1))),light,alpha);s.popPose();
    }
    private static void drawTubes(int type,MatrixStack s,IVertexBuilder b,int light,float alpha,int move,float tick,float clock,Vector3d target,float running){
        for(Tube tube:asset(type).tubes){
            Vector3d[] centers=new Vector3d[25],u=new Vector3d[25],v=new Vector3d[25];
            for(int i=0;i<25;i++){double t=i/24D;centers[i]=GhoulMotion.point(type,tube.limb,t,move,tick,clock,target);if(move==0)centers[i]=centers[i].add(Math.sin(clock*.18-t*5+tube.limb)*running*t*2,Math.sin(clock*.2-t*4)*running*t*3,running*t*7);}
            // Transport the cross-section along the curve so vertical bends cannot flip the texture frame.
            for(int i=0;i<25;i++){
                Vector3d tangent=centers[Math.min(24,i+1)].subtract(centers[Math.max(0,i-1)]).normalize();
                Vector3d previous=i==0?new Vector3d(0,1,0).cross(tangent):u[i-1];
                Vector3d projected=previous.subtract(tangent.scale(previous.dot(tangent)));
                if(projected.lengthSqr()<.00001)projected=tangent.cross(new Vector3d(0,0,1));
                u[i]=projected.normalize();v[i]=tangent.cross(u[i]).normalize();
            }
            for(float[][] quad:tube.quads)for(float[] p:quad){int i=MathHelper.clamp(Math.round(p[0]*24),0,24);
                double x=u[i].x*p[1]+v[i].x*p[2],y=u[i].y*p[1]+v[i].y*p[2],z=u[i].z*p[1]+v[i].z*p[2],length=Math.sqrt(x*x+y*y+z*z);
                if(length<.00001)length=1;
                b.vertex(s.last().pose(),(float)(centers[i].x+x),(float)(centers[i].y+y),(float)(centers[i].z+z)).color(1,1,1,alpha).uv(p[3],p[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(s.last().normal(),(float)(x/length),(float)(y/length),(float)(z/length)).endVertex();
            }
        }
    }
    public static void playerPose(PlayerModel<?> model,LivingEntity entity,float age){
        if(!(entity instanceof PlayerEntity)||!GhoulState.get(entity).getBoolean("Ghoul")||!GhoulState.active(entity))return;
        int move=GhoulState.action(entity);if(move==0)return;float t=entity.level.getGameTime()-GhoulState.get(entity).getLong("Start")+(age-entity.tickCount),weight=GhoulMotion.smooth(t/4)* (1-GhoulMotion.smooth((t-GhoulAbility.LIFE[move]+5)/5));
        if(move>=5&&move<=8){float pitch=move==8?(-155*GhoulMotion.pulse(t,17,14,3)-65*GhoulMotion.pulse(t,22,4,10)):-70;model.rightArm.xRot=MathHelper.lerp(weight,model.rightArm.xRot,pitch*(float)Math.PI/180);model.rightArm.yRot=MathHelper.lerp(weight,model.rightArm.yRot,move==6?(float)Math.sin(t*.14)*1.2F:-.25F);model.leftArm.xRot=MathHelper.lerp(weight,model.leftArm.xRot,-.35F);}
        else{
            float right=-.45F,left=-.45F,spread=.15F;
            if(move==1){right=-.8F;left=-.55F;spread=.3F;}
            if(move==2){right=left=.7F;spread=.25F;}
            if(move==3){right=-1.1F;left=-.35F;spread=.4F;}
            if(move==4){right=left=-1.2F;spread=.9F;}
            if(move==9){right=-.55F+(float)Math.sin(t*.5)*.15F;left=-.55F-(float)Math.sin(t*.5)*.15F;spread=.4F;}
            if(move==10){right=-1.05F;left=-.25F;spread=.2F;}
            if(move==11){right=left=.7F;spread=.5F;model.rightLeg.xRot+=weight*.45F;model.leftLeg.xRot-=weight*.35F;}
            if(move==12){right=left=-.65F;spread=.65F;}
            if(move==15){right=-1.25F;left=-1.05F;spread=-.35F;}
            if(move==16){right=-.65F;left=.45F;spread=.2F;}
            model.rightArm.xRot=MathHelper.lerp(weight,model.rightArm.xRot,right);model.leftArm.xRot=MathHelper.lerp(weight,model.leftArm.xRot,left);model.rightArm.zRot=MathHelper.lerp(weight,model.rightArm.zRot,spread);model.leftArm.zRot=MathHelper.lerp(weight,model.leftArm.zRot,-spread);
        }
        model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);model.leftPants.copyFrom(model.leftLeg);
    }
    private static void world(RenderWorldLastEvent e){Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){VISUALS.clear();GhoulVfx.clear();return;}GhoulVfx.world(e);
        PlayerEntity p=mc.player;int move=GhoulState.action(p);if(!mc.options.getCameraType().isFirstPerson()||!GhoulState.get(p).getBoolean("Ghoul")||!GhoulState.active(p)||move==0)return;
        MatrixStack s=e.getMatrixStack();Vector3d origin=p.getPosition(e.getPartialTicks()).subtract(mc.gameRenderer.getMainCamera().getPosition());s.pushPose();s.translate(origin.x,origin.y,origin.z);s.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(e.getPartialTicks(),p.yBodyRotO,p.yBodyRot)));s.scale(1/16F,1/16F,1/16F);
        int type=MathHelper.clamp(GhoulState.get(p).getInt("Type"),0,3);float tick=p.level.getGameTime()-GhoulState.get(p).getLong("Start")+e.getPartialTicks();IRenderTypeBuffer.Impl buffers=mc.renderBuffers().bufferSource();
        draw(type,s,buffers,15728880,.35F,move,tick,p.tickCount+e.getPartialTicks(),target(p,e.getPartialTicks()),false);if(move==5||move==7)drawShield(s,buffers,15728880,GhoulMotion.smooth(tick/5),.22F);s.popPose();buffers.endBatch();
    }
    private static void hud(RenderGameOverlayEvent.Post e){Minecraft mc=Minecraft.getInstance();if(e.getType()!=RenderGameOverlayEvent.ElementType.ALL||mc.player==null||mc.options.hideGui||mc.player.isSpectator())return;CompoundNBT n=GhoulState.get(mc.player);if(!n.getBoolean("Ghoul"))return;
        int type=MathHelper.clamp(n.getInt("Type"),0,3),width=146,x=Math.max(4,e.getWindow().getGuiScaledWidth()/2-width/2),y=Math.max(20,e.getWindow().getGuiScaledHeight()-112),color=new int[]{0xffeb426a,0xffb066d1,0xffd42c47,0xff29b6c4}[type];MatrixStack s=e.getMatrixStack();
        AbstractGui.fill(s,x-1,y-1,x+width+1,y+8,0xe0151520);AbstractGui.fill(s,x,y,x+width,y+7,0xff352432);AbstractGui.fill(s,x,y,x+(int)(width*GhoulState.rc(mc.player)/100),y+7,color);
        mc.font.drawShadow(s,GhoulState.TYPES[type].substring(0,1).toUpperCase()+GhoulState.TYPES[type].substring(1),x,y-11,0xffe8dde8);String text=(int)GhoulState.rc(mc.player)+" RC";mc.font.drawShadow(s,text,x+width-mc.font.width(text),y-11,0xffe8dde8);
    }
    private static final class CrystalRenderer extends EntityRenderer<GhoulProjectile>{
        CrystalRenderer(EntityRendererManager manager){super(manager);}
        @Override public ResourceLocation getTextureLocation(GhoulProjectile p){return texture(0);}
        @Override public void render(GhoulProjectile p,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){s.pushPose();Vector3d d=p.getDeltaMovement();s.mulPose(Vector3f.YP.rotation((float)Math.atan2(d.x,d.z)));s.mulPose(Vector3f.XP.rotation((float)-Math.atan2(d.y,Math.hypot(d.x,d.z))));GhoulVfx.crystal(s,b,p.piercing()?1.5F:.7F);s.popPose();}
    }
}
