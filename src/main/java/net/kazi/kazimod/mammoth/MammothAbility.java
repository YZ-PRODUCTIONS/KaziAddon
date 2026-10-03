package net.kazi.kazimod.mammoth;

import java.util.*;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.models.zoan.MammothMotion;
import net.kazi.kazimod.network.MammothAnimationPacket;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public final class MammothAbility extends Ability {
    public enum Move {
        STAMPEDE("Ancient Stampede",200,MammothCombatMath.STAMPEDE_CHARGE,MammothCombatMath.STAMPEDE_DURATION,true,"Charge for 3 seconds, then rush for 5 seconds. Deals 40 damage with powerful knockback, like Punk Corna Dio."),
        SWEEP("Ancient Sweep",160,40,0,false,"Wind up your trunk, then launch a broad wind slash. Deals 20 damage in full form or 15 in hybrid form."),
        STOMP("Ancient Stomp",200,0,MammothCombatMath.STOMP_DURATION,true,"Stomp for 4.8 seconds in a 15-block radius. Tremors deal 10 damage and hold enemies; the final quake deals 72.5-80 damage and launches them away."),
        VACUUM("Ancient Trunk Vacuum",100,0,MammothCombatMath.VACUUM_DURATION,false,"Extend your trunk and draw nearby enemies toward it for 4 seconds, within 30 blocks. Use again to cancel.");
        public final String title,description;public final int cooldown,charge,duration;public final boolean fullOnly;
        Move(String t,int cd,int c,int d,boolean full,String desc){title=t;cooldown=cd;charge=c;duration=d;fullOnly=full;description=desc;}
    }
    public final Move move;
    public final ChargeComponent charge=new ChargeComponent(this);
    public final ContinuousComponent continuous=new ContinuousComponent(this);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private final ChangeStatsComponent rushStats=new ChangeStatsComponent(this);
    private final Map<UUID,Long> hits=new HashMap<>();
    private final Set<UUID> held=new HashSet<>();
    private MammothVfxEntity vacuum;
    private boolean interrupted,statsApplied;
    private int lastPulse=-1;
    public MammothAbility(AbilityCore<MammothAbility> core,Move move){
        super(core);this.move=move;isNew=true;addComponents(charge,continuous,damage,rushStats);
        if(move==Move.STAMPEDE){
            rushStats.addAttributeModifier(ModAttributes.STEP_HEIGHT,new AbilityAttributeModifier(UUID.fromString("66b5b62c-bdc7-4c73-98b4-5163167872c4"),core,"Ancient Stampede step",2,Operation.ADDITION));
            rushStats.addAttributeModifier(ModAttributes.TOUGHNESS,new AbilityAttributeModifier(UUID.fromString("7ece36bf-763f-4404-9a09-f209752bbf48"),core,"Ancient Stampede toughness",3,Operation.ADDITION));
        }
        addTickEvent((user,a)->{
            boolean active=move==Move.STAMPEDE&&(charge.isCharging()||continuous.isContinuous());
            if(active&&!statsApplied){rushStats.applyModifiers(user);statsApplied=true;}
            else if(!active&&statsApplied){rushStats.removeModifiers(user);statsApplied=false;}
        });
        addCanUseCheck((user,a)->valid(user)?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent(move.fullOnly?"Enter Mammoth Full Form first.":"Enter a Mammoth form first.")));
        addUseEvent((user,a)->{
            if(continuous.isContinuous()){if(move==Move.VACUUM){interrupted=true;continuous.stopContinuity(user);}return;}
            if(charge.isCharging())return;
            interrupted=false;hits.clear();held.clear();lastPulse=-1;
            if(move.charge>0)charge.startCharging(user,move.charge);else continuous.startContinuity(user,move.duration);
        });
        charge.addStartEvent((user,a)->{
            if(!user.level.isClientSide&&move==Move.STAMPEDE)user.level.playSound(null,user.blockPosition(),KaziSounds.MAMMOTH_ROAR.get(),SoundCategory.PLAYERS,4,1);
        });
        charge.addTickEvent((user,a)->{
            if(!valid(user)){interrupted=true;charge.forceStopCharging(user);if(!user.level.isClientSide)cooldownComponent.startCooldown(user,move.cooldown);return;}
            if(move==Move.STAMPEDE)user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),2,2,false,false));
        });
        charge.addEndEvent((user,a)->{
            if(user.level.isClientSide||interrupted||!valid(user))return;
            if(move==Move.SWEEP){
                MammothWindEntity wind=new MammothWindEntity(user.level,user);
                wind.setPos(user.getX(),user.getY()+user.getBbHeight()*.6,user.getZ());
                wind.shoot(user.getLookAngle().x,user.getLookAngle().y,user.getLookAngle().z,1.8F,0);
                user.level.addFreshEntity(wind);MammothAnimationPacket.send(user,MammothMotion.SWEEP);
                user.level.playSound(null,user.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundCategory.PLAYERS,3,.55F);
                cooldownComponent.startCooldown(user,move.cooldown);
            }else continuous.startContinuity(user,move.duration);
        });
        continuous.addStartEvent((user,a)->{if(!user.level.isClientSide&&move==Move.VACUUM)vacuum=MammothVfxEntity.spawn(user,MammothVfxEntity.VACUUM,user.position(),(float)MammothCombatMath.VACUUM_RADIUS,move.duration);});
        continuous.addTickEvent((user,a)->tickMove(user));
        continuous.addEndEvent((user,a)->{
            if(statsApplied){rushStats.removeModifiers(user);statsApplied=false;}
            if(vacuum!=null){vacuum.remove();vacuum=null;}
            if(!user.level.isClientSide){
                if(move==Move.STOMP&&!interrupted&&valid(user)&&continuous.getContinueTime()>=move.duration)finishStomp(user);
                cooldownComponent.startCooldown(user,move.cooldown);
            }
            hits.clear();held.clear();lastPulse=-1;
        });
        addRemoveEvent((user,a)->{interrupted=true;if(charge.isCharging())charge.forceStopCharging(user);if(continuous.isContinuous())continuous.stopContinuity(user);if(vacuum!=null){vacuum.remove();vacuum=null;}if(statsApplied){rushStats.removeModifiers(user);statsApplied=false;}hits.clear();held.clear();});
    }
    private boolean valid(LivingEntity user){return user.isAlive()&&(move.fullOnly?MammothCombat.full(user):MammothCombat.active(user));}
    private void tickMove(LivingEntity user){
        if(!valid(user)){interrupted=true;continuous.stopContinuity(user);return;}
        // Corna Dio predicts movement on both sides; server-only movement snaps players back.
        if(move==Move.STAMPEDE){
            user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),2,2,false,false));
            user.move(MoverType.SELF,user.getLookAngle().multiply(MammothCombatMath.STAMPEDE_SPEED,0,MammothCombatMath.STAMPEDE_SPEED));
        }
        if(user.level.isClientSide)return;
        int t=(int)continuous.getContinueTime();long now=user.level.getGameTime();
        if(move==Move.STAMPEDE){
            Vector3d look=user.getLookAngle();
            if(t/6!=lastPulse){lastPulse=t/6;MammothCombat.quake(user,5,false);user.level.playSound(null,user.blockPosition(),SoundEvents.RAVAGER_STEP,SoundCategory.PLAYERS,2,.65F);}
            for(LivingEntity target:MammothCombat.targets(user,6,6)){
                if(target.distanceToSqr(user)>36||hits.getOrDefault(target.getUUID(),Long.MIN_VALUE)>now)continue;
                hits.put(target.getUUID(),now+25);
                if(damage.hurtTarget(user,target,40)){
                    AbilityHelper.setDeltaMovement(target,look.x*4,.2,look.z*4);
                    if(user.getRandom().nextFloat()>.75F)target.addEffect(new EffectInstance(ModEffects.DIZZY.get(),100,1,false,false));
                }
            }
        }else if(move==Move.STOMP){
            user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),2,2,false,false));
            int beat=t/8;boolean impact=beat!=lastPulse&&t<=88;
            if(impact){lastPulse=beat;MammothCombat.quake(user,15,false);MammothAnimationPacket.send(user,MammothMotion.STOMP);user.level.playSound(null,user.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundCategory.PLAYERS,2,.65F);}
            for(LivingEntity target:MammothCombat.targets(user,15,6)){
                Vector3d d=target.position().subtract(user.position());if(!MammothCombatMath.inStomp(d.x,d.y,d.z))continue;
                if(impact&&t<=80&&damage.hurtTarget(user,target,10))held.add(target.getUUID());
                if(held.contains(target.getUUID())){
                    target.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),3,1,false,false));
                    if(target.hasEffect(ModEffects.MOVEMENT_BLOCKED.get()))AbilityHelper.setDeltaMovement(target,0,Math.min(0,target.getDeltaMovement().y),0);
                }
            }
        }else if(move==Move.VACUUM){
            Vector3d mouth=MammothCombat.mouth(user);
            for(LivingEntity target:MammothCombat.targets(user,MammothCombatMath.VACUUM_RADIUS,20)){
                if(target.distanceToSqr(user)>MammothCombatMath.VACUUM_RADIUS*MammothCombatMath.VACUUM_RADIUS)continue;
                Vector3d delta=mouth.subtract(target.getBoundingBox().getCenter());double distance=delta.length();
                if(distance<.001)continue;
                Vector3d velocity=delta.scale(MammothCombatMath.pullSpeed(distance)/distance);
                AbilityHelper.setDeltaMovement(target,velocity.x,Math.max(-.8,Math.min(.8,velocity.y)),velocity.z);
            }
        }
    }
    private void finishStomp(LivingEntity user){
        MammothCombat.quake(user,15,true);MammothAnimationPacket.send(user,MammothMotion.STOMP_FINISH);
        user.level.playSound(null,user.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundCategory.PLAYERS,5,.55F);
        for(LivingEntity target:MammothCombat.targets(user,15,6)){
            Vector3d d=target.position().subtract(user.position());if(!MammothCombatMath.inStomp(d.x,d.y,d.z))continue;
            if(damage.hurtTarget(user,target,MammothCombatMath.finalDamage(Math.sqrt(d.x*d.x+d.z*d.z)))){
                Vector3d outward=new Vector3d(d.x,0,d.z).normalize();if(outward.lengthSqr()<.01)outward=new Vector3d(user.getLookAngle().x,0,user.getLookAngle().z).normalize();
                AbilityHelper.setDeltaMovement(target,outward.x*6,2.4,outward.z*6);
            }
        }
    }
}
