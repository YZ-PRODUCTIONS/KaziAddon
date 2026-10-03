package net.kazi.kazimod.preserved.sahur;

import java.util.*;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.kazi.kazimod.abilities.TripelT.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

public final class SahurAnimator {
    private static final Map<LivingEntity,State> STATES=new WeakHashMap<>();
    private static final Map<ModelRenderer,float[]> REST=new WeakHashMap<>();
    private static final class State { float last=-1,movement;SahurPose pose; }
    public static void hand(com.mojang.blaze3d.matrix.MatrixStack stack, ModelRenderer arm, ModelRenderer elbow) {
        arm.translateAndRotate(stack);
        // Retain the existing neutral grip while following the newly animated elbow.
        stack.translate(elbow.x / 16.0, elbow.y / 16.0, elbow.z / 16.0);
        stack.mulPose(net.minecraft.util.math.vector.Vector3f.XP.rotation(elbow.xRot + .2618F));
        stack.translate(-elbow.x / 16.0, -elbow.y / 16.0, -elbow.z / 16.0);
    }
    public static void apply(LivingEntity e,float stride,float amount,float age,float yaw,float pitch,boolean god,
                             ModelRenderer head,ModelRenderer body,ModelRenderer right,ModelRenderer left,
                             ModelRenderer legRight,ModelRenderer legLeft,ModelRenderer elbowRight,ModelRenderer elbowLeft,
                             ModelRenderer wingRight,ModelRenderer wingLeft,ModelRenderer halo){
        State state=STATES.computeIfAbsent(e,k->new State());
        float dt=state.last<0?1:Math.max(0,Math.min(3,age-state.last));state.last=age;
        float blend=(float)(1-Math.exp(-dt/2.3));state.movement+=(Math.min(1,amount)-state.movement)*blend;
        int action=SahurPose.IDLE;float charge=0,time=0;
        if(AbilityDataCapability.get(e)!=null)for(IAbility a:AbilityDataCapability.get(e).getEquippedAndPassiveAbilities()){
            boolean active=false,charging=false;float progress=0,elapsed=0;
            for(AbilityComponent<?> c:a.getComponents().values()){
                if(c instanceof ContinuousComponent){active|=((ContinuousComponent)c).isContinuous();elapsed=((ContinuousComponent)c).getContinueTime();}
                if(c instanceof ChargeComponent){charging|=((ChargeComponent)c).isCharging();progress=((ChargeComponent)c).getChargePercentage();}
            }
            if(!active&&!charging)continue;
            if(a instanceof HomeRunSwingAbility)action=god?SahurPose.DIVINE:SahurPose.SWING;
            else if(a instanceof TungTungTungBarrageAbility || a instanceof ArrowsOfLightAbility)action=god?SahurPose.PORTALS:SahurPose.BARRAGE;
            else if(a instanceof SahurYellAbility)action=god?SahurPose.RUSH:SahurPose.ROAR;
            else if(a instanceof SwingingCounterAbility)action=god?SahurPose.SHIELD:SahurPose.COUNTER;
            else continue;
            charge=charging?Math.max(.05F,Math.min(1,progress)):0;time=elapsed;break;
        }
        boolean flying=e instanceof PlayerEntity&&((PlayerEntity)e).abilities.flying;
        float partial=Math.max(0,Math.min(1,age-e.tickCount));
        SahurPose target=SahurPose.sample(age,stride,state.movement,e.isSprinting(),flying,god,yaw,pitch,e.getAttackAnim(partial),action,charge,time);
        if (!flying && !e.isOnGround() && !e.isInWater()) {
            float tuck = e.getDeltaMovement().y > 0 ? .28F : -.13F;
            target.joints[4][0] += tuck; target.joints[5][0] += tuck;
            target.joints[2][2] += .13F; target.joints[3][2] -= .13F;
        }
        if(state.pose==null)state.pose=target;
        else {for(int j=0;j<8;j++)for(int k=0;k<3;k++)state.pose.joints[j][k]+=(target.joints[j][k]-state.pose.joints[j][k])*blend;
            state.pose.elbowRight+=(target.elbowRight-state.pose.elbowRight)*blend;state.pose.elbowLeft+=(target.elbowLeft-state.pose.elbowLeft)*blend;
            state.pose.bob+=(target.bob-state.pose.bob)*blend;state.pose.halo+=(target.halo-state.pose.halo)*blend;}
        ModelRenderer[] parts={head,body,right,left,legRight,legLeft,wingRight,wingLeft};
        for(int i=0;i<parts.length;i++){ModelRenderer part=parts[i];if(part==null)continue;
            float[] rest=REST.computeIfAbsent(part,k->new float[]{k.x,k.y,k.z});
            part.xRot=state.pose.joints[i][0];part.yRot=state.pose.joints[i][1];part.zRot=state.pose.joints[i][2];
            if(i>=6){
                float[] anchor=SahurRigMath.wingAnchor(i==6),torso=REST.get(body);
                float[] moved=SahurRigMath.rotate(anchor[0]-torso[0],anchor[1]-torso[1],anchor[2]-torso[2],body.xRot,body.yRot,body.zRot);
                float[] live={body.x+moved[0],body.y+moved[1],body.z+moved[2]};
                float[] pos=SahurRigMath.anchorPosition(rest,anchor,state.pose.joints[i],live);
                part.setPos(pos[0],pos[1],pos[2]);continue;
            }
            // Correct the animated pivot only: rest geometry still uses the original local coordinates.
            float anchorY=i==0||i==2||i==3?-14:i==4||i==5?5.5F:rest[1];
            double dy=rest[1]-anchorY,sx=Math.sin(part.xRot),cx=Math.cos(part.xRot);
            double x=dy*sx*Math.sin(part.yRot),y=dy*cx,z=dy*sx*Math.cos(part.yRot);
            double xx=x*Math.cos(part.zRot)-y*Math.sin(part.zRot),yy=x*Math.sin(part.zRot)+y*Math.cos(part.zRot);
            part.setPos(rest[0]+(float)xx,anchorY+(float)yy+state.pose.bob,rest[2]+(float)z);
        }
        elbowRight.xRot=-.2618F-state.pose.elbowRight;elbowLeft.xRot=-.2618F-state.pose.elbowLeft;
        if(halo!=null)halo.y=-7.8909F+state.pose.halo;
    }
}
