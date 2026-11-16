package net.kazi.kazimod.mochi;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Map;
import java.util.WeakHashMap;
import net.MrMagicalCart.cartaddon.abilities.mochi2.*;
import net.kazi.kazimod.abilities.MochiRework.ZanGiriMochiClone;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

public final class MochiLayers {
    private static final Map<LivingEntity,float[]> CLAP_POSES=new WeakHashMap<>();
    private MochiLayers(){}
    public static void render(String layer,BipedModel<?> parent,LivingEntity entity,MatrixStack s,IRenderTypeBuffer b,float age,int light) {
        boolean hard=MochiHelper.hasArmamentOn(entity),far=!MochiEffects.detailed(entity);
        if(layer.equals("KakuMochiLayer")) {
            arm(s,b,parent.rightArm,age,light,.38,1,hard,false,false,far);
            arm(s,b,parent.leftArm,age+7,light,.38,1,hard,false,false,far);return;
        }
        if(layer.equals("MochiTsukiLayer")||layer.equals("MusoMochiHadanLayer")||layer.equals("YakiMochiLayer")) {
            arm(s,b,parent.rightArm,age,light,layer.equals("YakiMochiLayer")?.65:.36,1.15,hard,
                    layer.equals("MochiTsukiLayer"),layer.equals("MusoMochiHadanLayer"),far);return;
        }
        if(layer.equals("CarvedMochiLayer")) {
            Ability ability=(Ability)AbilityDataCapability.get(entity).getEquippedAbility(ZanGiriMochiClone.INSTANCE);
            if(ability==null)ability=(Ability)AbilityDataCapability.get(entity).getEquippedAbility(ZanGiriMochiAbility.INSTANCE);
            s.pushPose();parent.rightArm.translateAndRotate(s);
            if(ability!=null&&ability.isCharging())s.mulPose(Vector3f.YP.rotation(age*.38F));
            s.scale(1,-1,1);MochiMesh mesh=new MochiMesh(s,b,age,far,light);
            Vector3d end=new Vector3d(-.5,-1.25,-.12);
            mesh.strand(Vector3d.ZERO,new Vector3d(-.12,-.4,.1),new Vector3d(-.7,-.7,.1),end,.18,.55,MochiMesh.CREAM);
            s.translate(end.x,end.y,end.z);MochiMesh.orient(s,new Vector3d(0,-1,0));
            mesh.fist(.85,true,false);mesh.ring(.72,.18,MochiMesh.HAKI,true);s.popPose();return;
        }
        s.pushPose();s.scale(1,-1,1);
        if(layer.equals("KuriMochiLayer")) {
            s.translate(0,.9,.5);MochiMesh mesh=new MochiMesh(s,b,age,far,light);
            mesh.ring(1.1,.32,hard?MochiMesh.HAKI:MochiMesh.CREAM,true);mesh.ringDrips(1.1,.32,4,entity.getId());
        } else if(layer.equals("GaltingMochiLayer")) {
            for(int side=-1;side<=1;side+=2)for(int row=0;row<3;row++) {
                double phase=age*.52+row*2.1+(side+1)*1.6;
                double reach=.8+.8*(.5+.5*Math.sin(phase));
                Vector3d root=new Vector3d(side*.42,-.1,.15),end=new Vector3d(side*(.65+row*.22),.25-row*.42,-reach);
                appendage(s,b,root,end,.30,age+row*9,light,hard,false,far);
            }
        } else if(layer.equals("KashiwaMochiLayer")) {
            Ability ability=(Ability)AbilityDataCapability.get(entity).getEquippedAbility(KashiwaMochiAbility.INSTANCE);
            boolean charging=ability!=null&&ability.isCharging();
            float[] pose=CLAP_POSES.computeIfAbsent(entity,e->new float[]{1.45F,age});
            float elapsed=Math.max(0,Math.min(2,age-pose[1]));pose[1]=age;
            pose[0]+=(float)(((charging?1.45:.45)-pose[0])*(1-Math.exp(-elapsed*.55)));
            double spread=pose[0];
            for(int side=-1;side<=1;side+=2) {
                Vector3d root=new Vector3d(side*1.7,-.2,.65),hand=new Vector3d(side*spread,-.15,-1.4);
                donut(s,b,root,1,.30,age+side*9,light,far,entity.getId()+side);
                appendage(s,b,root,hand,.7,age,light,hard,true,far);
            }
        } else if(layer.equals("SupremeSmashingMochiLayer")||layer.equals("MusoDonutSlamLayer")) {
            boolean slam=layer.equals("MusoDonutSlamLayer");
            for(int side=-1;side<=1;side+=2) {
                Vector3d root=new Vector3d(side*1.7,1.4,.5);
                Vector3d hand=new Vector3d(side*1.7,slam?-.9:.45,-1.7-.25*Math.sin(age*.13+side));
                donut(s,b,root,1.1,.32,age,light,far,entity.getId()+side);
                appendage(s,b,root,hand,1.0,age,light,true,false,far);
            }
        } else if(layer.equals("ChikaraMochiLayer")) {
            donut(s,b,new Vector3d(0,.3,.9),1.5,.38,age,light,far,entity.getId());
        }
        s.popPose();
    }
    private static void arm(MatrixStack s,IRenderTypeBuffer b,ModelRenderer arm,float age,int light,double radius,double length,
                            boolean hard,boolean drill,boolean barrels,boolean far) {
        s.pushPose();arm.translateAndRotate(s);s.scale(1,-1,1);
        MochiMesh mesh=new MochiMesh(s,b,age,far,light);double sway=.035*Math.sin(age*.12);
        Vector3d end=new Vector3d(sway,-length,0);
        mesh.strand(Vector3d.ZERO,new Vector3d(sway,-length*.3,.05),new Vector3d(-sway,-length*.7,.03),end,.16,radius*.63,MochiMesh.CREAM);
        mesh.drips(radius*.4,-length*.78,2,(int)(radius*100));
        s.translate(end.x,end.y,end.z);MochiMesh.orient(s,new Vector3d(0,-1,0));
        if(drill) {
            s.mulPose(Vector3f.ZP.rotation(age*.7F));
            mesh.strand(new Vector3d(0,0,-.2),new Vector3d(0,0,.2),new Vector3d(0,0,.8),new Vector3d(0,0,1.3),radius,.005,hard?MochiMesh.HAKI:MochiMesh.CREAM);
            for(int i=0;i<3;i++){s.pushPose();s.translate(0,0,i*.3);mesh.ring(radius*(1-i*.25),.035,MochiMesh.CREAM,false);s.popPose();}
        } else if(barrels) {
            s.mulPose(Vector3f.ZP.rotation(age*.15F));mesh.blob(Vector3d.ZERO,radius*1.3,radius*1.3,.4,MochiMesh.CREAM);
            for(int i=0;i<6;i++) {double a=i*Math.PI/3;s.pushPose();s.translate(Math.cos(a)*radius*.8,Math.sin(a)*radius*.8,.35);
                mesh.ring(radius*.32,radius*.12,hard?MochiMesh.HAKI:MochiMesh.CREAM,false);s.popPose();}
        } else mesh.fist(radius,hard,false);
        s.popPose();
    }
    private static void appendage(MatrixStack s,IRenderTypeBuffer b,Vector3d root,Vector3d hand,double radius,float age,int light,boolean hard,boolean open,boolean far) {
        MochiMesh mesh=new MochiMesh(s,b,age,far,light);
        Vector3d delta=hand.subtract(root);
        mesh.strand(root,root.add(delta.scale(.28)).add(.05*Math.sin(age*.1),.22,0),
                root.add(delta.scale(.68)).add(0,-.12,0),hand,radius*.55,radius*.68,MochiMesh.CREAM);
        s.pushPose();s.translate(hand.x,hand.y,hand.z);MochiMesh.orient(s,delta);mesh.fist(radius,hard,open);s.popPose();
    }
    private static void donut(MatrixStack s,IRenderTypeBuffer b,Vector3d center,double r,double thickness,float age,int light,boolean far,int seed) {
        s.pushPose();s.translate(center.x,center.y,center.z);MochiMesh mesh=new MochiMesh(s,b,age,far,light);
        mesh.ring(r,thickness,MochiMesh.CREAM,false);mesh.ringDrips(r,thickness,3,seed);s.popPose();
    }
}
