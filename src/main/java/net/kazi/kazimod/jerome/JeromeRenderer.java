package net.kazi.kazimod.jerome;

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

public final class JeromeRenderer extends EntityRenderer<JeromeEntity> {
    private final CerberusRig rig;
    private final Map<JeromeEntity,Visual> visuals=new WeakHashMap<>();
    private static class Visual {float last,stride,walk,start;JeromeAction action;Map<String,CerberusRig.Pose> lastPose,from;}
    public JeromeRenderer(EntityRendererManager manager){
        super(manager);shadowRadius=.75F;
        try(InputStream stream=getClass().getResourceAsStream("/assets/kazimod/models/entities/jerome.json")){
            if(stream==null)throw new IOException("Missing Jerome model");rig=CerberusRig.read(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }catch(IOException e){throw new IllegalStateException("Cannot load Jerome",e);}
    }
    public Map<String,CerberusRig.Pose> pose(JeromeEntity e,float partial){
        float time=e.tickCount+partial;Visual v=visuals.computeIfAbsent(e,k->new Visual());float dt=MathHelper.clamp(time-v.last,0,2);v.last=time;
        float speed=MathHelper.lerp(partial,e.animationSpeedOld,e.animationSpeed);
        v.walk+=(MathHelper.clamp(speed*2.2F,0,1)-v.walk)*Math.min(1,dt*.25F);v.stride+=dt*speed*.075F;
        Map<String,CerberusRig.Pose> pose=rig.pose();JeromeAction action=e.action();
        if(action==JeromeAction.IDLE){
            rig.apply(pose,"idle",time/20,1-v.walk);rig.apply(pose,"run",v.stride,v.walk);
            CerberusRig.Pose head=pose.computeIfAbsent("Head",k->new CerberusRig.Pose());head.rotation[0]-=MathHelper.lerp(partial,e.xRotO,e.xRot)*.35F;
        }else rig.apply(pose,action.clip,e.actionAge(partial)/20,1);
        if(v.action!=action){v.action=action;v.from=v.lastPose;v.start=time;}
        float blend=JeromeVfx.smooth((time-v.start)/5);
        if(v.from!=null&&blend<1){Set<String> names=new HashSet<>(v.from.keySet());names.addAll(pose.keySet());for(String bone:names){
            CerberusRig.Pose from=v.from.get(bone),to=pose.computeIfAbsent(bone,k->new CerberusRig.Pose());
            for(int i=0;i<3;i++){to.rotation[i]=MathHelper.lerp(blend,from==null?0:from.rotation[i],to.rotation[i]);to.position[i]=MathHelper.lerp(blend,from==null?0:from.position[i],to.position[i]);}
        }}else v.from=null;v.lastPose=pose;return pose;
    }
    @Override public void render(JeromeEntity e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
        Map<String,CerberusRig.Pose> pose=pose(e,partial);s.pushPose();s.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot)));
        float scale=e.renderScale(partial);s.scale(scale,scale,scale);shadowRadius=.8F*scale;
        if(e.deathTime>0)s.mulPose(Vector3f.ZP.rotationDegrees(Math.min(90,(e.deathTime+partial)*4.5F)));
        s.pushPose();s.scale(1/16F,1/16F,1/16F);draw(rig.nodes,pose,s,b.getBuffer(RenderType.entityCutoutNoCull(JeromeEntity.TEXTURE)),light);s.popPose();
        if(e.isAlive())JeromeVfx.boss(s,b,e.action(),e.actionAge(partial),e.tickCount+partial,e.rage());
        s.popPose();super.render(e,yaw,partial,s,b,light);
    }
    public static void draw(CerberusRig.Node[] nodes,Map<String,CerberusRig.Pose> pose,MatrixStack s,IVertexBuilder b,int light){
        for(CerberusRig.Node n:nodes){if(!n.visible)continue;if(n.children!=null){
            s.pushPose();CerberusRig.Pose p=pose.get(n.name);float[] o=n.origin,r=n.rotation;
            s.translate(o[0]+(p==null?0:p.position[0]),o[1]+(p==null?0:p.position[1]),o[2]+(p==null?0:p.position[2]));
            s.mulPose(Vector3f.ZP.rotationDegrees(r[2]+(p==null?0:p.rotation[2])));s.mulPose(Vector3f.YP.rotationDegrees(r[1]+(p==null?0:p.rotation[1])));s.mulPose(Vector3f.XP.rotationDegrees(r[0]+(p==null?0:p.rotation[0])));s.translate(-o[0],-o[1],-o[2]);
            draw(n.children,pose,s,b,light);s.popPose();
        }else if(n.quads!=null)for(CerberusRig.Quad q:n.quads)for(float[] p:q.vertices)
            b.vertex(s.last().pose(),p[0],p[1],p[2]).color(1F,1F,1F,1F).uv(p[3],p[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(s.last().normal(),q.normal[0],q.normal[1],q.normal[2]).endVertex();}
    }
    @Override public ResourceLocation getTextureLocation(JeromeEntity e){return JeromeEntity.TEXTURE;}
}


