package net.kazi.kazimod.gengar;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
public final class GengarModel<T extends LivingEntity> extends MorphModel<T> {
    private static CerberusRig RIG;
    private static final String[] CLIPS={"idle","transform","charge","release","passage","haunt","dream","grasp","tag","perish","emerge"};
    private static final Map<LivingEntity,Blend> BLENDS=new WeakHashMap<>();
    private static final class Blend {
        int action=-1;long start=Long.MIN_VALUE;float changed,lastAge,movement,yaw,pitch;
        Map<String,CerberusRig.Pose> previous,from;
    }
    private Map<String,CerberusRig.Pose> pose=Collections.emptyMap();
    public GengarModel(){super(0);}
    public static CerberusRig rig(){if(RIG==null)try(InputStream in=GengarModel.class.getResourceAsStream("/assets/kazimod/models/entities/mega_gengar.json")){
        if(in==null)throw new IOException("Mega Gengar model missing");RIG=CerberusRig.read(new InputStreamReader(in,StandardCharsets.UTF_8));
    }catch(IOException error){throw new IllegalStateException(error);}return RIG;}
    public static Map<String,CerberusRig.Pose> sample(int action,float elapsed,float age,float movement,float attack,float yaw,float pitch){
        CerberusRig rig=rig();Map<String,CerberusRig.Pose> pose=rig.pose();
        rig.apply(pose,"idle",age/20,action==0?1:.15F);
        if(action>0&&action<CLIPS.length)rig.apply(pose,CLIPS[action],elapsed/20,1);
        else {rig.apply(pose,"float",age/20,Math.min(1,movement*3));if(attack>0)rig.apply(pose,"melee",attack*.55F,1);}
        CerberusRig.Pose head=pose.computeIfAbsent("Head",k->new CerberusRig.Pose());head.rotation[1]+=MathHelper.clamp(yaw,-30,30)*.25F;head.rotation[0]+=MathHelper.clamp(pitch,-35,35)*.18F;
        return pose;
    }
    @Override public void setupAnim(T user,float swing,float amount,float age,float yaw,float pitch){
        int action=GengarFeatures.action(user);long start=GengarFeatures.state(user).getLong("Start");
        float partial=MathHelper.clamp(age-user.tickCount,0,1),elapsed=Math.max(0,user.level.getGameTime()-start+partial);
        Blend blend=BLENDS.computeIfAbsent(user,k->new Blend());
        float dt=age-blend.lastAge;boolean fresh=blend.previous==null||dt<0||dt>6;
        if(fresh){blend.previous=null;blend.from=null;blend.movement=amount;blend.yaw=yaw;blend.pitch=pitch;}
        float follow=1-(float)Math.exp(-MathHelper.clamp(dt,0,2)/2.5F);
        blend.movement=MathHelper.lerp(follow,blend.movement,amount);
        blend.yaw=MathHelper.rotLerp(follow,blend.yaw,yaw);blend.pitch=MathHelper.lerp(follow,blend.pitch,pitch);
        pose=sample(action,elapsed,age,blend.movement,user.getAttackAnim(partial),blend.yaw,blend.pitch);
        if(blend.action!=action||blend.start!=start){
            // An interrupted clip blends from its displayed pose, while a fresh summon starts below the floor.
            blend.from=action==1||fresh?null:blend.previous;blend.changed=age;blend.action=action;blend.start=start;
        }
        float duration=action==3||action==10?2.5F:5;
        float weight=MathHelper.clamp((age-blend.changed)/duration,0,1);weight=weight*weight*weight*(weight*(weight*6-15)+10);
        if(blend.from!=null&&weight<1){Set<String> bones=new HashSet<>(blend.from.keySet());bones.addAll(pose.keySet());for(String bone:bones){
            CerberusRig.Pose old=blend.from.get(bone),now=pose.computeIfAbsent(bone,k->new CerberusRig.Pose());for(int i=0;i<3;i++){
                now.rotation[i]=MathHelper.rotLerp(weight,old==null?0:old.rotation[i],now.rotation[i]);now.position[i]=MathHelper.lerp(weight,old==null?0:old.position[i],now.position[i]);
            }
        }}else blend.from=null;blend.previous=pose;blend.lastAge=age;
    }
    @Override public void renderToBuffer(MatrixStack s,IVertexBuilder b,int light,int overlay,float r,float g,float blue,float alpha){
        s.pushPose();try{s.translate(0,1.5,0);s.scale(1/16F,1/16F,1/16F);
            s.last().pose().multiply(Matrix4f.createScaleMatrix(1,-1,1));s.last().normal().mul(Matrix3f.createScaleMatrix(1,-1,1));
            draw(rig().nodes,pose,s,b,light,overlay,r,g,blue,alpha,true);
        }finally{s.popPose();}
    }
    public static void draw(CerberusRig.Node[] nodes,Map<String,CerberusRig.Pose> pose,MatrixStack s,IVertexBuilder b,int light,int overlay,float r,float g,float blue,float alpha,boolean reverse){
        for(CerberusRig.Node n:nodes){if(!n.visible)continue;if(n.children!=null){s.pushPose();CerberusRig.Pose p=pose.get(n.name);float[] o=n.origin,a=n.rotation;
            s.translate(o[0]+(p==null?0:p.position[0]),o[1]+(p==null?0:p.position[1]),o[2]+(p==null?0:p.position[2]));
            s.mulPose(Vector3f.ZP.rotationDegrees(a[2]+(p==null?0:p.rotation[2])));s.mulPose(Vector3f.YP.rotationDegrees(a[1]+(p==null?0:p.rotation[1])));s.mulPose(Vector3f.XP.rotationDegrees(a[0]+(p==null?0:p.rotation[0])));
            s.translate(-o[0],-o[1],-o[2]);draw(n.children,pose,s,b,light,overlay,r,g,blue,alpha,reverse);s.popPose();
        }else if(n.quads!=null)for(CerberusRig.Quad q:n.quads)for(int i=0;i<4;i++){float[] v=q.vertices[reverse?3-i:i];b.vertex(s.last().pose(),v[0],v[1],v[2]).color(r,g,blue,alpha).uv(v[3],v[4]).overlayCoords(overlay).uv2(light).normal(s.last().normal(),q.normal[0],q.normal[1],q.normal[2]).endVertex();}}
    }
    @Override public boolean renderItemInHand(T user,HandSide side,MatrixStack s){return false;}
    @Override public void renderFirstPersonArm(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side){}
    @Override public void renderFirstPersonLeg(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side){}
}
