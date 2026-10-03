package net.kazi.kazimod.preserved.cerberus;

import java.util.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

public final class CerberusAbility extends Ability {
    public enum Move {
        FURY("Hell Hath No Fury", "fury", 250, 70, "Sprint toward your prey; use again to leap and bite."),
        EXECUTION("Threefold Execution", "execution", 300, 24, "The side heads catch enemies before the center head bites. Three Hunt marks reduce healing."),
        HOWL("Underworld Howl", "howl", 320, 36, "Three howls reveal enemies, repel close attackers and slow fully marked prey."),
        VIGIL("Hound's Vigil", "vigil", 280, 35, "Counter the first frontal melee strike with a bite. Attacks from behind remain effective."),
        POUNCE("Gravebound Pounce", "pounce", 240, 65, "Leap toward your aim and slam the ground. A direct landing adds a bite."),
        TERRITORY("Gates of the Underworld", "territory", 600, 22, "Create one dark-flame territory. Build Hunt faster inside; crossing its edge briefly slows enemies."),
        GATES("Gates of Hell", "gates", 600, 100, "Charge for two seconds, then all three heads fire six volleys of giant fireballs.");
        public final String title, icon, description;
        public final int cooldown, duration;
        Move(String title, String icon, int cooldown, int duration, String description) {
            this.title=title;this.icon=icon;this.cooldown=cooldown;this.duration=duration;this.description=description;
        }
    }
    public final Move move;
    private int age;
    private int launchAge;
    private boolean launched, landed;
    private final ContinuousComponent continuous = new ContinuousComponent(this)
            .addStartEvent(this::start).addTickEvent(this::during).addEndEvent(this::end);
    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final DamageTakenComponent defense = new DamageTakenComponent(this).addOnAttackEvent(this::defend);
    public CerberusAbility(AbilityCore<CerberusAbility> core, Move move) {
        super(core); this.move=move; this.isNew=true;
        addComponents(continuous, damage, defense);
        addCanUseCheck((e,a) -> CerberusCombat.active(e) ? AbilityUseResult.success()
                : AbilityUseResult.fail(new StringTextComponent("Transform into Cerberus first.")));
        addUseEvent((e,a) -> {
            if (!continuous.isContinuous()) continuous.startContinuity(e,
                    move==Move.EXECUTION && CerberusCombat.hybrid(e)?20:move.duration);
            else if (move == Move.FURY && !launched) leap(e, .65);
        });
        addRemoveEvent((e,a) -> { if (continuous.isContinuous()) continuous.stopContinuity(e); });
    }
    public String animation(float elapsed) {
        switch(move) {
            case FURY:return "";
            case GATES:return elapsed < 2 ? "gates_charge" : "gates_fire";
            default:return "cerberus_"+move.icon;
        }
    }
    private void start(LivingEntity e, IAbility a) {
        age=0; launched=false; landed=false;
        if (!e.level.isClientSide && move == Move.POUNCE) leap(e, .9);
    }
    private void end(LivingEntity e, IAbility a) {
        if (!e.level.isClientSide) cooldownComponent.startCooldown(e, move.cooldown);
    }
    private void leap(LivingEntity e, double up) {
        if (e.level.isClientSide) return;
        launched=true; launchAge=age; Vector3d look=e.getLookAngle();
        double speed=CerberusCombat.hybrid(e)?1.15:1.4;
        AbilityHelper.setDeltaMovement(e,look.x*speed,up+Math.max(0,look.y)*.4,look.z*speed);
    }
    private void during(LivingEntity e, IAbility a) {
        if (e.level.isClientSide) return;
        ++age;
        if (!e.isAlive() || !CerberusCombat.active(e)) { continuous.stopContinuity(e); return; }
        switch(move) {
            case FURY:
                if (!launched) {
                    Vector3d look=e.getLookAngle();
                    AbilityHelper.setDeltaMovement(e,look.x*.85,e.getDeltaMovement().y,look.z*.85);
                } else if (age-launchAge>4 && !landed && (e.isOnGround() || !targets(e, 3.8, true).isEmpty())) {
                    if(targets(e,4,true).isEmpty())e.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,12,1));
                    landed=true; strike(e,4,24,true); continuous.stopContinuity(e);
                }
                break;
            case EXECUTION:
                if(age==7) for(LivingEntity t:targets(e,5,true)) {
                    Vector3d pull=e.position().add(e.getLookAngle().scale(2)).subtract(t.position()).normalize().scale(.4);
                    AbilityHelper.setDeltaMovement(t,pull.x,.08,pull.z);
                }
                if(age==13) strike(e,5,32,true);
                break;
            case HOWL:
                if(age==1 || age==13 || age==25) {
                    CerberusEffectEntity.spawn(e,2,e.position().add(0,2,0),Vector3d.ZERO);
                    e.level.playSound(null,e.blockPosition(),SoundEvents.WOLF_HOWL,SoundCategory.PLAYERS,2,.65F+age*.006F);
                    for(LivingEntity t:targets(e,56,false)) {
                        boolean marked=CerberusCombat.fullyMarked(e,t);
                        t.addEffect(new EffectInstance(Effects.GLOWING,60,0));
                        if(marked)t.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,25,1));
                        if(e.distanceToSqr(t)<25)push(e,t,1.0);
                        CerberusCombat.mark(e,t,"HOWL",false);
                    }
                }
                break;
            case POUNCE:
                if(age>5 && e.isOnGround() && !landed) {
                    landed=true;
                    CerberusEffectEntity.spawn(e,2,e.position().add(0,.2,0),Vector3d.ZERO);
                    for(LivingEntity t:targets(e,5,false)) {
                        boolean direct=e.distanceToSqr(t)<6.25;
                        if(hit(e,t,direct?28:16,direct))push(e,t,.7);
                    }
                    continuous.stopContinuity(e);
                }
                break;
            case TERRITORY: if(age==12)CerberusCombat.territory(e); break;
            case GATES:
                if(age>=40 && age<100 && (age-40)%10==0) {
                    Vector3d aim=e.getEyePosition(1).add(e.getLookAngle().scale(48));
                    for(int head=0;head<3;head++) {
                        Vector3d mouth=CerberusCombat.mouth(e,head);
                        CerberusEffectEntity.spawn(e,0,mouth,aim.subtract(mouth).normalize().scale(1.6));
                    }
                    e.level.playSound(null,e.blockPosition(),SoundEvents.FIRECHARGE_USE,SoundCategory.PLAYERS,2,.7F);
                }
                break;
            default:break;
        }
    }
    private float defend(LivingEntity e, IAbility a, DamageSource source, float amount) {
        if(move!=Move.VIGIL || !continuous.isContinuous() || e.level.isClientSide
                || !(source.getDirectEntity() instanceof LivingEntity) || source.isProjectile())return amount;
        LivingEntity t=(LivingEntity)source.getDirectEntity();
        Vector3d direction=t.position().subtract(e.position()).normalize();
        if(!CerberusCombat.enemy(e,t) || e.getLookAngle().dot(direction)<.4 || e.distanceToSqr(t)>36)return amount;
        continuous.stopContinuity(e);
        hit(e,t,22,true);push(e,t,1);
        e.swing(Hand.MAIN_HAND,true);
        return 0;
    }
    private List<LivingEntity> targets(LivingEntity e,double range,boolean cone) {
        final double r=range*(CerberusCombat.hybrid(e)?.8:1);
        return e.level.getEntitiesOfClass(LivingEntity.class,e.getBoundingBox().inflate(r), t ->
                CerberusCombat.enemy(e,t) && e.distanceToSqr(t)<=r*r && e.canSee(t)
                && (!cone || e.getLookAngle().dot(t.position().subtract(e.position()).normalize())>.25));
    }
    private void strike(LivingEntity e,double range,float amount,boolean bite) {
        for(LivingEntity t:targets(e,range,bite)){if(hit(e,t,amount,bite))push(e,t,CerberusCombat.hybrid(e)?.4:.7);}
    }
    private boolean hit(LivingEntity e,LivingEntity t,float amount,boolean bite) {
        if(!damage.hurtTarget(e,t,amount))return false;
        CerberusCombat.mark(e,t,move.name(),bite); return true;
    }
    private static void push(LivingEntity e,LivingEntity t,double strength) {
        Vector3d v=t.position().subtract(e.position()).normalize().scale(strength);
        AbilityHelper.setDeltaMovement(t,v.x,.2,v.z);
    }
}
