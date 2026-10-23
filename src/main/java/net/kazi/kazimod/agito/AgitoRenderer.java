package net.kazi.kazimod.agito;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public final class AgitoRenderer extends EntityRenderer<AgitoEntity> {
    private final CerberusRig rig;
    private final List<CerberusRig.Node> rightHand,leftHand;
    private final Map<AgitoEntity,Visual> visuals=new WeakHashMap<>();
    private static final class Visual {
        float last,stride,locomotion,transitionStart,airWeight;
        AgitoAction action;
        Map<String,CerberusRig.Pose> lastPose,fromPose;
    }
    public AgitoRenderer(EntityRendererManager manager){
        super(manager);shadowRadius=1;
        try(InputStream stream=AgitoRenderer.class.getResourceAsStream("/assets/kazimod/models/entities/agito.json")){
            if(stream==null)throw new IOException("Missing Agito geometry");
            rig=CerberusRig.read(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }catch(IOException error){throw new IllegalStateException("Cannot load Agito model",error);}
        rightHand=rig.path("Right hand");leftHand=rig.path("Left hand");
    }
    @Override public void render(AgitoEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        float time=e.tickCount+partial,age=e.actionAge(partial),walk=MathHelper.lerp(partial,e.animationSpeedOld,e.animationSpeed);
        Visual v=visuals.computeIfAbsent(e,k->new Visual());float dt=MathHelper.clamp(time-v.last,0,2);v.last=time;
        v.locomotion+=(MathHelper.clamp(walk*2.5F,0,1)-v.locomotion)*Math.min(1,dt*.3F);
        v.airWeight+=((e.isOnGround()?0:1)-v.airWeight)*Math.min(1,dt*.3F);
        AgitoAction action=e.action();boolean portal=e.portal();
        Map<String,CerberusRig.Pose> pose=rig.pose();
        if(action==AgitoAction.IDLE){
            rig.apply(pose,"idle",time/20,1-v.locomotion*.8F);
            float run=AgitoVfx.smooth((walk-.32F)/.3F);
            v.stride=(v.stride+dt*walk/MathHelper.lerp(run,5.4F,11.5F))%1;
            rig.apply(pose,"walk",v.stride,v.locomotion*(1-run)*(1-v.airWeight));
            rig.apply(pose,"run",v.stride*.76F,v.locomotion*run*(1-v.airWeight));
            rig.apply(pose,"airborne",time/20,v.airWeight);
            CerberusRig.Pose head=pose.computeIfAbsent("Head",k->new CerberusRig.Pose());
            head.rotation[1]-=MathHelper.clamp(MathHelper.rotLerp(partial,e.yHeadRotO,e.yHeadRot)-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot),-25,25);
            head.rotation[0]-=MathHelper.lerp(partial,e.xRotO,e.xRot)*.3F;
        }else{
            rig.apply(pose,action.clip,age/20,1);
            if(e.isHealing())rig.apply(pose,"healing_breath",time/20,AgitoVfx.smooth(age/20));
        }
        if(v.action!=action){v.action=action;v.fromPose=v.lastPose;v.transitionStart=time;}
        float transition=AgitoVfx.smooth((time-v.transitionStart)/4);
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
        v.lastPose=pose;shadowRadius=portal?0:1;
        if(portal)AgitoVfx.portal(stack,buffers,e.isDismissing()?age*80/60:age);
        if(e.isHealing() && e.owner()!=null){
            net.minecraft.util.math.vector.Vector3d at=e.owner().getPosition(partial).subtract(e.getPosition(partial));
            stack.pushPose();stack.translate(at.x,at.y,at.z);AgitoVfx.healing(stack,buffers,age);stack.popPose();
        }
        stack.pushPose();stack.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot)));
        if(action==AgitoAction.SUMMON)stack.translate(0,-4.3*(1-AgitoVfx.smooth(age/70)),0);
        if(e.isDismissing())stack.translate(0,MathHelper.lerp(AgitoVfx.smooth((age-8)/44),-4.3F*(1-AgitoVfx.smooth(e.dismissalStartAge()/70)),-4.8F),0);
        if(e.deathTime>0)stack.mulPose(Vector3f.ZP.rotationDegrees(Math.min(90,(e.deathTime+partial)*4.5F)));
        float scale=AgitoEntity.MODEL_SCALE/16;stack.scale(scale,scale,scale);
        float reveal=action==AgitoAction.SUMMON?.02F+.98F*AgitoVfx.smooth((age-76)/4):1;
        if(e.isDismissing())reveal=MathHelper.lerp(AgitoVfx.smooth(age/12),.02F+.98F*AgitoVfx.smooth((e.dismissalStartAge()-76)/4),.02F);
        IVertexBuilder buffer=buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(e)));
        draw(rig.nodes,pose,stack,buffer,light,reveal);
        if(!portal && e.deathTime==0){
            // Fist electricity follows the same bone chain as the skin, including the user's hand rotations.
            fists(rightHand,pose,stack,buffers,time,-33.4F,e.isHealing());
            fists(leftHand,pose,stack,buffers,time+7,33.4F,e.isHealing());
        }
        stack.popPose();super.render(e,yaw,partial,stack,buffers,light);
    }
    private void fists(List<CerberusRig.Node> chain,Map<String,CerberusRig.Pose> pose,MatrixStack stack,IRenderTypeBuffer buffers,float time,float x,boolean healing){
        stack.pushPose();for(CerberusRig.Node node:chain)transform(node,pose,stack);
        stack.translate(x,40,-2);AgitoVfx.electricity(stack,buffers,time,healing?.42F:1);stack.popPose();
    }
    private void draw(CerberusRig.Node[] nodes,Map<String,CerberusRig.Pose> pose,MatrixStack stack,IVertexBuilder buffer,int light,float tint){
        for(CerberusRig.Node node:nodes){
            if(!node.visible)continue;
            if(node.children!=null){stack.pushPose();transform(node,pose,stack);draw(node.children,pose,stack,buffer,light,tint);stack.popPose();}
            else if(node.quads!=null)for(CerberusRig.Quad q:node.quads)for(float[] p:q.vertices)
                buffer.vertex(stack.last().pose(),p[0],p[1],p[2]).color(tint,tint,tint,1).uv(p[3],p[4])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(stack.last().normal(),q.normal[0],q.normal[1],q.normal[2]).endVertex();
        }
    }
    private void transform(CerberusRig.Node node,Map<String,CerberusRig.Pose> pose,MatrixStack stack){
        CerberusRig.Pose p=pose.get(node.name);float[] o=node.origin,r=node.rotation;
        stack.translate(o[0]+(p==null?0:p.position[0]),o[1]+(p==null?0:p.position[1]),o[2]+(p==null?0:p.position[2]));
        stack.mulPose(Vector3f.ZP.rotationDegrees(r[2]+(p==null?0:p.rotation[2])));
        stack.mulPose(Vector3f.YP.rotationDegrees(r[1]+(p==null?0:p.rotation[1])));
        stack.mulPose(Vector3f.XP.rotationDegrees(r[0]+(p==null?0:p.rotation[0])));stack.translate(-o[0],-o[1],-o[2]);
    }
    @Override public ResourceLocation getTextureLocation(AgitoEntity e){return AgitoEntity.TEXTURE;}
}
