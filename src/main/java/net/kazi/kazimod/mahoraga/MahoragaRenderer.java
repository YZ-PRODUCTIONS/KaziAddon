package net.kazi.kazimod.mahoraga;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

public final class MahoragaRenderer extends EntityRenderer<MahoragaEntity> {
    private final CerberusRig rig;
    private final Map<MahoragaEntity,Visual> visuals=new WeakHashMap<>();
    private static final class Visual {
        float last,locomotion,wheel,transitionStart,stride;
        boolean initialized;
        MahoragaAction action;
        Map<String,CerberusRig.Pose> lastPose,fromPose;
    }
    public MahoragaRenderer(EntityRendererManager manager){
        super(manager);shadowRadius=1.1F;
        try(InputStream stream=MahoragaRenderer.class.getResourceAsStream("/assets/kazimod/models/entities/mahoraga.json")){
            if(stream==null)throw new IOException("Missing Mahoraga geometry");
            rig=CerberusRig.read(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }catch(IOException error){throw new IllegalStateException("Cannot load Mahoraga model",error);}
    }
    @Override public void render(MahoragaEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        float time=e.tickCount+partial,age=e.actionAge(partial),walk=MathHelper.lerp(partial,e.animationSpeedOld,e.animationSpeed);
        Visual v=visuals.computeIfAbsent(e,k->new Visual());float dt=MathHelper.clamp(time-v.last,0,2);v.last=time;
        float desiredWheel=e.wheelTurns()*45+45*MahoragaVfx.smooth((e.wheelProgress()+ (e.wheelProgress()>0?partial:0))/20F);
        if(!v.initialized){v.wheel=desiredWheel;v.initialized=true;}
        v.wheel+=(desiredWheel-v.wheel)*Math.min(1,dt*.55F);
        float speed=MathHelper.clamp(walk*2.5F,0,1);v.locomotion+=(speed-v.locomotion)*Math.min(1,dt*.3F);
        MahoragaAction action=e.action();Map<String,CerberusRig.Pose> pose=rig.pose();
        boolean portal=action==MahoragaAction.SUMMON || action==MahoragaAction.DISMISS;
        shadowRadius=portal?0:1.1F;
        float attack=action==MahoragaAction.IDLE?0:portal?1:Math.min(MahoragaVfx.smooth(age/3),MahoragaVfx.smooth((action.ticks-age)/4));
        rig.apply(pose,"idle",time/20,1-attack);
        if(!e.isOnGround() && action==MahoragaAction.IDLE)rig.apply(pose,"airborne",time/20,1);
        else if(v.locomotion>.005 && action==MahoragaAction.IDLE){
            float run=MahoragaVfx.smooth((walk-.35F)/.3F);
            // Both gaits share a footfall phase, including during the walk/run blend.
            v.stride=(v.stride+dt*walk/MathHelper.lerp(run,5.75F,12.96F))%1;
            rig.apply(pose,"walk",v.stride*1.25F, v.locomotion*(1-run));
            rig.apply(pose,"run",v.stride*.72F,v.locomotion*run);
        }
        if(action!=MahoragaAction.IDLE)rig.apply(pose,action.clip,age/20,attack);
        if(action==MahoragaAction.IDLE){
            CerberusRig.Pose head=pose.computeIfAbsent("05 Head",k->new CerberusRig.Pose());
            head.rotation[1]-=MathHelper.clamp(MathHelper.rotLerp(partial,e.yHeadRotO,e.yHeadRot)-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot),-30,30);
            head.rotation[0]-=MathHelper.lerp(partial,e.xRotO,e.xRot)*.45F;
        }
        if(v.action!=action){v.action=action;v.fromPose=v.lastPose;v.transitionStart=time;}
        float transition=MahoragaVfx.smooth((time-v.transitionStart)/(action==MahoragaAction.BLOCK?2:4));
        if(v.fromPose!=null && transition<1){
            Set<String> bones=new HashSet<>(v.fromPose.keySet());bones.addAll(pose.keySet());
            for(String bone:bones){
                CerberusRig.Pose from=v.fromPose.get(bone),to=pose.computeIfAbsent(bone,k->new CerberusRig.Pose());
                for(int i=0;i<3;i++){
                    to.rotation[i]=MathHelper.lerp(transition,from==null?0:from.rotation[i],to.rotation[i]);
                    to.position[i]=MathHelper.lerp(transition,from==null?0:from.position[i],to.position[i]);
                }
            }
        }else v.fromPose=null;
        v.lastPose=pose;
        // Keep the persistent wheel turn outside the action-transition snapshots.
        CerberusRig.Pose wheel=pose.get("09 Eight handled wheel"),wheelWithTurn=new CerberusRig.Pose();
        if(wheel!=null)for(int i=0;i<3;i++){wheelWithTurn.rotation[i]=wheel.rotation[i];wheelWithTurn.position[i]=wheel.position[i];}
        Map<String,CerberusRig.Pose> renderPose=new HashMap<>(pose);
        wheelWithTurn.rotation[1]+=v.wheel;renderPose.put("09 Eight handled wheel",wheelWithTurn);pose=renderPose;
        if(portal)MahoragaVfx.portal(stack,buffers,action==MahoragaAction.DISMISS?age*80/MahoragaAction.DISMISS.ticks:age);
        if(e.impactAge(partial)<18){
            Vector3d at=e.impactPosition().subtract(e.getPosition(partial));stack.pushPose();stack.translate(at.x,at.y,at.z);
            MahoragaVfx.impact(stack,buffers,e.impactAge(partial));stack.popPose();
        }
        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot)));
        if(action==MahoragaAction.SUMMON)stack.translate(0,-4.7*(1-MahoragaVfx.smooth(age/70)),0);
        if(action==MahoragaAction.DISMISS){
            float initialDepth=-4.7F*(1-MahoragaVfx.smooth(e.dismissalStartAge()/70));
            stack.translate(0,MathHelper.lerp(MahoragaVfx.smooth((age-8)/44),initialDepth,-5.6F),0);
        }
        if(e.deathTime>0){stack.translate(0,.15,0);stack.mulPose(Vector3f.ZP.rotationDegrees(Math.min(90,(e.deathTime+partial)*4.5F)));}
        float scale=MahoragaEntity.MODEL_SCALE/16;stack.scale(scale,scale,scale);
        float reveal=action==MahoragaAction.SUMMON?.02F+.98F*MahoragaVfx.smooth((age-78)/2):1;
        if(action==MahoragaAction.DISMISS){
            float initialTint=.02F+.98F*MahoragaVfx.smooth((e.dismissalStartAge()-78)/2);
            reveal=MathHelper.lerp(MahoragaVfx.smooth(age/12),initialTint,.02F);
        }
        IVertexBuilder buffer=buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(e)));
        draw(rig.nodes,pose,stack,buffer,light,reveal,action.clip,age/20,attack,time/20);
        if(!e.heldBlock().isAir()){
            stack.pushPose();for(CerberusRig.Node node:rig.path("14 Left hand"))transform(node,pose,stack,action.clip,age/20,attack,time/20);
            stack.translate(30,47,-5);float blockScale=1/scale;stack.scale(blockScale,blockScale,blockScale);stack.translate(-.5,-.5,-.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(e.heldBlock(),stack,buffers,light,OverlayTexture.NO_OVERLAY);
            stack.popPose();
        }
        stack.popPose();
        attackVfx(e,age,stack,buffers,partial);
        super.render(e,yaw,partial,stack,buffers,light);
    }
    private void draw(CerberusRig.Node[] nodes,Map<String,CerberusRig.Pose> pose,MatrixStack stack,IVertexBuilder buffer,int light,float tint,String action,float age,float weight,float idle){
        for(CerberusRig.Node node:nodes){
            if(!node.visible)continue;
            if(node.children!=null){stack.pushPose();transform(node,pose,stack,action,age,weight,idle);draw(node.children,pose,stack,buffer,light,tint,action,age,weight,idle);stack.popPose();}
            else if(node.quads!=null)for(CerberusRig.Quad q:node.quads)for(float[] p:q.vertices)
                buffer.vertex(stack.last().pose(),p[0],p[1],p[2]).color(tint,tint,tint,1).uv(p[3],p[4])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(stack.last().normal(),q.normal[0],q.normal[1],q.normal[2]).endVertex();
        }
    }
    private void transform(CerberusRig.Node node,Map<String,CerberusRig.Pose> pose,MatrixStack stack,String action,float age,float weight,float idle){
        CerberusRig.Pose p=pose.get(node.name);float[] o=node.origin,r=node.rotation;
        stack.translate(o[0]+(p==null?0:p.position[0]),o[1]+(p==null?0:p.position[1]),o[2]+(p==null?0:p.position[2]));
        stack.mulPose(Vector3f.ZP.rotationDegrees(r[2]+(p==null?0:p.rotation[2])));
        stack.mulPose(Vector3f.YP.rotationDegrees(r[1]+(p==null?0:p.rotation[1])));
        stack.mulPose(Vector3f.XP.rotationDegrees(r[0]+(p==null?0:p.rotation[0])));
        if(node.name.startsWith("20 Breath") || node.name.startsWith("21 Breath")){
            float[] scale={1,1,1};sampleScale(scale,rig.clip("idle"),node.name,idle,1-weight);
            if(weight>0)sampleScale(scale,rig.clip(action),node.name,age,weight);
            stack.scale(scale[0],scale[1],scale[2]);
        }
        stack.translate(-o[0],-o[1],-o[2]);
    }
    private void sampleScale(float[] result,CerberusRig.Clip clip,String bone,float time,float weight){
        time=clip.loop?(time%clip.length):MathHelper.clamp(time,0,clip.length);
        for(CerberusRig.Track t:clip.tracks)if(t.bone.equals(bone)){
            CerberusRig.Key a=null,b=null;
            for(CerberusRig.Key k:t.keys)if(k.channel.equals("scale")){if(a==null || k.time<=time)a=k;if(k.time>=time){b=k;break;}}
            if(a==null)return;if(b==null)b=a;float f=a.time==b.time?0:(time-a.time)/(b.time-a.time);
            for(int i=0;i<3;i++)result[i]+=(a.value[i]+(b.value[i]-a.value[i])*f-1)*weight;
        }
    }
    private void attackVfx(MahoragaEntity e,float age,MatrixStack stack,IRenderTypeBuffer buffers,float partial){
        stack.pushPose();stack.mulPose(Vector3f.YP.rotationDegrees(-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot)));
        if((e.action()==MahoragaAction.SLASH_LEFT || e.action()==MahoragaAction.SLASH_RIGHT) && age>9 && age<16){
            stack.translate(0,2.7,0);stack.mulPose(Vector3f.ZP.rotationDegrees(e.action()==MahoragaAction.SLASH_LEFT?-18:18));
            MahoragaVfx.arc(MahoragaVfx.sink(stack,buffers,true),4.4,.13,0,.15,Math.PI-.15,0xe6f8ff,(16-age)/7,40);
        }
        if(e.action()==MahoragaAction.AIR_JUMP && age>=10 && age<17){
            MahoragaVfx.ring(MahoragaVfx.sink(stack,buffers,true),.4+(age-10)*.22,.06,0,0xedf9ff,(17-age)/7,32);
        }
        if(e.action()==MahoragaAction.AIR_BLAST && age>5 && age<30){
            stack.translate(0,3.75,1.0);float p=(age-5)/25;
            for(int i=0;i<3;i++){
                float q=((age*.075F+i/3F)%1);
                stack.pushPose();stack.translate(0,0,(1-q)*3.5);stack.mulPose(Vector3f.XP.rotationDegrees(90));
                MahoragaVfx.ring(MahoragaVfx.sink(stack,buffers,true),.1+(1-q)*1.1,.025,0,0xd9edf3,.4F*p*(float)Math.sin(q*Math.PI),32);stack.popPose();
            }
        }
        stack.popPose();
    }
    @Override public ResourceLocation getTextureLocation(MahoragaEntity e){return MahoragaEntity.TEXTURE;}
}
