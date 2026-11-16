package net.kazi.kazimod.leopard;
import java.util.Map;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public final class LeopardCombatVisual {
    public static void barrage(Map<String,CerberusRig.Pose> pose,float age,float weight) {
        for(int side:new int[]{-1,1}) {
            float phase=age*1.7F+(side>0?(float)Math.PI:0),thrust=(1+(float)Math.sin(phase))*.5F;
            rotate(pose,"Arm "+side,48+50*thrust,side*(6-10*thrust),side*5,weight);
            rotate(pose,"Forearm "+side,60-55*thrust,0,0,weight);
            rotate(pose,"Hand "+side,-8,0,0,weight);
            for(int i=0;i<4;i++) {rotate(pose,"Finger "+side+" "+i,55,0,0,weight);rotate(pose,"Finger tip "+side+" "+i,80,0,0,weight);}
        }
        rotate(pose,"Chest",-5,4*(float)Math.sin(age*1.7F),0,weight);
    }
    private static void rotate(Map<String,CerberusRig.Pose> p,String bone,float x,float y,float z,float w) {
        CerberusRig.Pose value=p.computeIfAbsent(bone,k->new CerberusRig.Pose());
        value.rotation[0]=MathHelper.lerp(w,value.rotation[0],x);value.rotation[1]=MathHelper.lerp(w,value.rotation[1],y);value.rotation[2]=MathHelper.lerp(w,value.rotation[2],z);
    }
    public static LivingEntity target(LivingEntity owner) {
        if(owner.getPersistentData().getLong("kaziLeopardGrabUntil")<owner.level.getGameTime())return null;
        Entity target=owner.level.getEntity(owner.getPersistentData().getInt("kaziLeopardGrab"));
        return target instanceof LivingEntity&&target!=owner&&target.isAlive()&&target.distanceToSqr(owner)<256?(LivingEntity)target:null;
    }
    public static Vector3d localTarget(LivingEntity owner,LivingEntity target,float partial) {
        double dx=MathHelper.lerp(partial,target.xo,target.getX())-MathHelper.lerp(partial,owner.xo,owner.getX());
        double dz=MathHelper.lerp(partial,target.zo,target.getZ())-MathHelper.lerp(partial,owner.zo,owner.getZ());
        double dy=MathHelper.lerp(partial,target.yo,target.getY())-MathHelper.lerp(partial,owner.yo,owner.getY());
        double yaw=Math.toRadians(MathHelper.rotLerp(partial,owner.yBodyRotO,owner.yBodyRot));
        return new Vector3d((-dx*Math.cos(yaw)-dz*Math.sin(yaw))*16,(dy+target.getBbHeight()*.5)*16,(dx*Math.sin(yaw)-dz*Math.cos(yaw))*16);
    }
    public static void tail(MatrixStack stack,IVertexBuilder out,Vector3d center,float radius,float age,int light,int overlay) {
        Vector3d start=new Vector3d(0,25,5),side=new Vector3d(14,25,9),end=center.add(radius,0,0),previous=start;
        for(int i=1;i<=12;i++) {
            double t=i/12.0,u=1-t;
            Vector3d p=start.scale(u*u).add(side.scale(2*u*t)).add(end.scale(t*t));
            segment(stack,out,previous,p,2.4F,false,light,overlay);previous=p;
        }
        for(int i=1;i<=36;i++) {
            double t=i/36.0,angle=t*Math.PI*3.4,r=radius+.12*Math.sin(age*.12+t*10);
            Vector3d p=center.add(Math.cos(angle)*r,t*9,Math.sin(angle)*r);
            segment(stack,out,previous,p,(float)(2.4-.8*t),i>32,light,overlay);previous=p;
        }
    }
    private static void segment(MatrixStack s,IVertexBuilder b,Vector3d a,Vector3d end,float width,boolean tip,int light,int overlay) {
        Vector3d d=end.subtract(a);float length=(float)d.length();if(length<.001)return;
        s.pushPose();s.translate(a.x,a.y,a.z);
        s.mulPose(Vector3f.ZP.rotation((float)-Math.atan2(d.x,d.y)));
        s.mulPose(Vector3f.XP.rotation((float)Math.atan2(d.z,Math.hypot(d.x,d.y))));
        float h=width*.5F,top=length+.3F;
        float[][] v={{-h,-.3F,-h},{h,-.3F,-h},{h,top,-h},{-h,top,-h},{-h,-.3F,h},{h,-.3F,h},{h,top,h},{-h,top,h}};
        int[][] faces={{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        float u=tip?.003F:.253F,v0=tip?.253F:.503F;
        for(int f=0;f<6;f++)for(int j=3;j>=0;j--) {
            float[] p=v[faces[f][j]],n=normals[f];
            b.vertex(s.last().pose(),p[0],p[1],p[2]).color(1F,1F,1F,1F).uv(u+(j==1||j==2?.04F:0),v0+(j>=2?.08F:0))
                .overlayCoords(overlay).uv2(light).normal(s.last().normal(),n[0],n[1],n[2]).endVertex();
        }
        s.popPose();
    }
    private LeopardCombatVisual(){}
}
