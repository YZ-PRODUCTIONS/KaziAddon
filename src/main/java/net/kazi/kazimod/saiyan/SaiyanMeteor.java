package net.kazi.kazimod.saiyan;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** A bounded, collision-checked carry. Native grab checks retain MMNM dodge/block rules. */
final class SaiyanMeteor {
    private final LivingEntity user;
    private final GrabEntityComponent grab;
    private final DealDamageComponent damage;
    private final Vector3d direction,forward;
    private final boolean userGravity;
    private LivingEntity victim;
    private boolean victimGravity,finished;
    private int phase,ticks,total;

    SaiyanMeteor(LivingEntity user,GrabEntityComponent grab,DealDamageComponent damage){
        this.user=user;this.grab=grab;this.damage=damage;
        direction=user.getLookAngle().normalize();
        Vector3d flat=direction.multiply(1,0,1);
        forward=flat.lengthSqr()<.01?new Vector3d(-Math.sin(Math.toRadians(user.yRot)),0,Math.cos(Math.toRadians(user.yRot))):flat.normalize();
        userGravity=user.isNoGravity();user.setNoGravity(true);
    }
    boolean tick(){
        if(finished||++total>90||!user.isAlive()||user.removed)return false;
        ticks++;user.fallDistance=0;
        user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),2,0,false,false));
        if(phase==0)return lunge();
        if(victim==null||!victim.isAlive()||victim.removed||victim.level!=user.level||victim.isPassenger()
                ||!SaiyanAbility.canHit(user,victim)||!grab.canContinueGrab(user)||grab.getGrabbedEntity()!=victim
                ||user.distanceToSqr(victim)>36)return false;
        victim.fallDistance=0;victim.stopUsingItem();
        victim.addEffect(new EffectInstance(ModEffects.GRABBED.get(),3,3,false,false));
        if(phase==1){
            Vector3d movement=forward.scale(.23).add(0,.55+.2*Math.sin(Math.min(1,ticks/18.0)*Math.PI),0);
            Vector3d before=user.position();
            if(!move(user,movement)||!hold(.2))return false;
            if(ticks>=18||user.getY()-before.y<movement.y*.5){phase=2;ticks=0;SaiyanState.pose(user,12,60);sync();}
            return true;
        }
        if(phase==2){
            if(!hold(.2-1.3*Math.min(1,ticks/5.0)))return false;
            if(ticks>=5){phase=3;ticks=0;}
            return true;
        }
        Vector3d before=victim.position(),step=forward.scale(.2).add(0,-Math.min(2.6,1.1+ticks*.2),0);
        if(!move(victim,step))return false;
        Vector3d delta=victim.position().subtract(before);
        Vector3d userAt=victim.position().subtract(forward.scale(holdDistance())).add(0,1.1,0);
        if(!move(user,userAt.subtract(user.position())))return false;
        if(victim.isOnGround()||delta.y>step.y+.05){
            impact();return false;
        }
        return ticks<48&&!victim.isInWater()&&!victim.isInLava();
    }
    private boolean lunge(){
        Vector3d before=user.position();
        if(!move(user,direction.scale(1.4)))return false;
        Vector3d after=user.position(),start=before.add(0,user.getBbHeight()*.55,0),end=after.add(0,user.getBbHeight()*.55,0);
        LivingEntity closest=null;double distance=Double.MAX_VALUE;
        AxisAlignedBB area=user.getBoundingBox().expandTowards(before.subtract(after)).inflate(.45);
        for(LivingEntity enemy:user.level.getEntitiesOfClass(LivingEntity.class,area)){
            if(!SaiyanAbility.canHit(user,enemy)||!user.canSee(enemy)||enemy.isPassenger()||enemy.hasEffect(ModEffects.GRABBED.get()))continue;
            AxisAlignedBB box=enemy.getBoundingBox().inflate(user.getBbWidth()*.5+.12);
            Optional<Vector3d> hit=box.clip(start,end);
            if(!hit.isPresent()&&!box.contains(start))continue;
            double d=box.contains(start)?0:hit.get().distanceToSqr(start);
            if(d<distance){distance=d;closest=enemy;}
        }
        if(closest!=null){
            if(!grab.grabManually(user,closest))return false;
            if(!damage.hurtTarget(user,closest,5*SaiyanState.power(user))){grab.release(user);return false;}
            victim=closest;victimGravity=victim.isNoGravity();victim.setNoGravity(true);
            phase=1;ticks=0;SaiyanState.pose(user,11,30);sync();
            SaiyanEffect.spawn(user,4,victim.getBoundingBox().getCenter(),10,1);
            return true;
        }
        return ticks<12&&after.distanceToSqr(before)>.01&&!user.horizontalCollision;
    }
    private double holdDistance(){return (user.getBbWidth()+victim.getBbWidth())*.5+.25;}
    private boolean hold(double height){
        Vector3d at=user.position().add(forward.scale(holdDistance())).add(0,height,0);
        if(!move(victim,at.subtract(victim.position())))return false;
        return victim.distanceToSqr(at)<.16;
    }
    private boolean move(LivingEntity entity,Vector3d delta){
        if(!Double.isFinite(delta.lengthSqr())||delta.lengthSqr()>36)return false;
        // Check every substep so a high-speed slam cannot cross an unloaded/protected boundary.
        int steps=Math.max(1,(int)Math.ceil(delta.length()/.4));Vector3d step=delta.scale(1.0/steps);
        for(int i=0;i<steps;i++){
            Vector3d at=entity.position().add(step);BlockPos pos=new BlockPos(at);
            if(at.y<1||at.y+entity.getBbHeight()>=entity.level.getMaxBuildHeight()||!entity.level.hasChunkAt(pos)
                    ||!entity.level.getWorldBorder().isWithinBounds(pos))return false;
            xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea area=
                    xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData.get(entity.level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
            if(area!=null&&!area.canHurtEntities())return false;
            entity.move(MoverType.SELF,step);
        }
        entity.fallDistance=0;AbilityHelper.setDeltaMovement(entity,Vector3d.ZERO);
        if(entity instanceof ServerPlayerEntity)((ServerPlayerEntity)entity).connection.teleport(entity.getX(),entity.getY(),entity.getZ(),entity.yRot,entity.xRot);
        return true;
    }
    private void impact(){
        Vector3d point=victim.position();
        damage.hurtTarget(user,victim,22*SaiyanState.power(user));
        for(LivingEntity enemy:user.level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(point,point).inflate(3)))
            if(enemy!=victim&&SaiyanAbility.canHit(user,enemy)&&enemy.distanceToSqr(point)<=9&&user.canSee(enemy)){
                damage.hurtTarget(user,enemy,8*SaiyanState.power(user));
                AbilityHelper.setDeltaMovement(enemy,enemy.position().subtract(point).multiply(1,0,1).normalize().scale(.8).add(0,.3,0));
            }
        SaiyanEffect.spawn(user,4,point,18,3);
        SaiyanFeatures.play(user,SoundEvents.GENERIC_EXPLODE,.8F,1.3F);
        SaiyanState.pose(user,13,12);sync();finished=true;
    }
    void release(){
        user.setNoGravity(userGravity);user.fallDistance=0;
        if(victim!=null){victim.setNoGravity(victimGravity);victim.fallDistance=0;}
        grab.release(user);
        // Short native locks expire naturally, without clearing somebody else's newer stun.
    }
    private void sync(){if(user instanceof ServerPlayerEntity)SaiyanFeatures.sync((ServerPlayerEntity)user);}
}
