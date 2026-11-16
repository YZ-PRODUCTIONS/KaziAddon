package net.kazi.kazimod.decay;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.potion.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import net.kazi.kazimod.petal.PetalFeatures;

public final class DecayAbility extends Ability {
    public static final float CATASTROPHE_RADIUS=72;
    public static final String[] NAMES={"Decaying Grasp","Decay Wave","Ruin Rush","Crumbling Guard","Chain Decay","Catastrophe"};
    public static final int[] COOLDOWN={120,280,180,300,440,1800},LIFE={18,26,24,60,32,72};
    public static final String[] DESCRIPTIONS={
        "Reach forward after a brief windup. Touch a visible enemy within 4 blocks for 14 damage and 9 decay damage over 3 seconds. Briefly slows them; missing still costs the cooldown.",
        "Touch the ground, then spread decay across connected surfaces within 14 blocks. One hit per enemy for 18 damage plus 6 decay damage. Bypasses dodge abilities and Future Sight; jump or fly clear of the ground to evade. Limited surface destruction.",
        "Lunge along your aim for up to 0.8 seconds. The first enemy touched takes 16 damage and 6 decay damage. Walls stop the rush. No invulnerability.",
        "Hold a frontal guard for up to 3 seconds; use again to cancel. Halves frontal damage and crumbles projectiles below 35 damage. Nearby attackers take 6 damage, at most once per second. Rear attacks bypass it.",
        "Mark a visible enemy within 18 blocks. After 0.9 seconds, decay jumps between up to 4 enemies within 6 blocks of each other, dealing 14 damage plus 6 decay damage each. Spread out or break line of sight to escape.",
        "Crouch and charge for 3 seconds, then unleash decay across a 72-block radius. Deals 40 damage plus 9 decay damage once per enemy, bypassing dodge abilities and Future Sight. Jump or fly clear. Terrain within 10 blocks above and below the origin crumbles into dust, leaving a 2.5-block central pillar intact. Protected terrain survives. Taking 8 or more damage interrupts the charge."};
    public static final AbilityCore<DecayAbility>[] CORES=create();
    @SuppressWarnings("unchecked") private static AbilityCore<DecayAbility>[] create(){AbilityCore<DecayAbility>[] a=new AbilityCore[6];for(int i=0;i<6;i++){final int id=i;a[i]=new AbilityCore.Builder<DecayAbility>(NAMES[i],"decay_"+NAMES[i].toLowerCase(Locale.ROOT).replace(' ','_'),AbilityCategory.DEVIL_FRUITS,AbilityType.ACTION,c->new DecayAbility(c,id)).setUnlockCheck(DecayFeatures::awakened).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(i==0||i==2?SourceType.FIST:SourceType.INDIRECT).setSourceElement(SourceElement.NONE).setIcon(new ResourceLocation("kazimod","textures/abilities/decay/"+i+".png")).addDescriptionLine(new StringTextComponent(DESCRIPTIONS[i])).addDescriptionLine(new StringTextComponent("Cooldown: "+COOLDOWN[i]/20+"s.")).build();}return a;}
    public final int move;
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private int ticks;private LivingEntity target;private DecayField field;private boolean hit;
    public DecayAbility(AbilityCore<DecayAbility> core,int move){super(core);this.move=move;isNew=true;addComponents(continuous,damage);
        addCanUseCheck((p,a)->{
            if(!DecayFeatures.usable(p))return fail("Awaken Sabi with an Awakening Essence first.");
            if(continuous.isContinuous())return AbilityUseResult.success();
            if(DecayFeatures.action(p)>=0)return fail("Finish your current decay technique first.");
            if((move==1||move==5)&&(!p.isOnGround()||DecayField.surface(p.level,p.blockPosition().below())==null))return fail("Stand on solid ground to spread decay.");
            if(move==4&&PetalFeatures.target(p,18)==null)return fail("Aim at a visible enemy within 18 blocks.");
            return AbilityUseResult.success();
        });
        addUseEvent((p,a)->{if(p.level.isClientSide)return;if(continuous.isContinuous()){continuous.stopContinuity(p);return;}ticks=0;hit=false;field=null;target=move==4?PetalFeatures.target(p,18):null;DecayFeatures.state(p).putBoolean("Interrupted",false);DecayFeatures.pose(p,move,LIFE[move]+2);
            if(move==1||move==5)field=DecayField.spawn(p,move,p.position(),move==1?90:900,move==1?14:CATASTROPHE_RADIUS);
            else {field=DecayField.spawn(p,move,p.position(),LIFE[move],1);if(target!=null)field.target(target);}
            continuous.triggerContinuity(p,LIFE[move]);sound(p,SoundEvents.STONE_BREAK,.75F,.65F);
        });
        continuous.addTickEvent((p,a)->{if(p.level.isClientSide)return;ticks++;
            if(!DecayFeatures.usable(p)||DecayFeatures.action(p)!=move||move==5&&DecayFeatures.state(p).getBoolean("Interrupted")){continuous.stopContinuity(p);return;}
            if(move==5&&ticks<60){AbilityHelper.setDeltaMovement(p,new Vector3d(0,Math.min(0,p.getDeltaMovement().y),0));if(!p.isOnGround()){continuous.stopContinuity(p);return;}}
            if(move==0&&ticks==8){LivingEntity t=PetalFeatures.target(p,4);if(t!=null&&strike(p,t,14)){mark(p,t,3);t.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,20,1));}}
            if(move==2&&ticks>=5&&ticks<21&&!hit){Vector3d velocity=p.getLookAngle().scale(1.15);Vector3d from=p.getEyePosition(1),to=PetalFeatures.aim(p,2.5);LivingEntity chosen=null;double distance=Double.MAX_VALUE;
                for(LivingEntity t:p.level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(from,to).inflate(1),t->PetalFeatures.canHit(p,t))){Optional<Vector3d> intersection=t.getBoundingBox().inflate(.45).clip(from,to);if(intersection.isPresent()&&p.canSee(t)&&intersection.get().distanceToSqr(from)<distance){distance=intersection.get().distanceToSqr(from);chosen=t;}}
                if(chosen!=null){if(strike(p,chosen,16))mark(p,chosen,2);hit=true;AbilityHelper.setDeltaMovement(p,Vector3d.ZERO);continuous.stopContinuity(p);return;}
                if(p.horizontalCollision||!p.level.noCollision(p.getBoundingBox().move(velocity))){continuous.stopContinuity(p);return;}AbilityHelper.setDeltaMovement(p,velocity);p.fallDistance=0;
            }
            if(move==4&&ticks==18){Set<UUID> struck=new HashSet<>();LivingEntity next=target;Vector3d last=p.getEyePosition(1);for(int i=0;i<4&&next!=null;i++){
                LivingEntity body=PetalFeatures.bodyOwner(next);if(body==null||!PetalFeatures.canHit(p,next)||!p.canSee(next)||i==0&&p.distanceToSqr(next)>22*22)break;
                struck.add(body.getUUID());if(strike(p,next,14))mark(p,next,2);DecayField.spawn(p,7,last,22,1).end(next.position().add(0,next.getBbHeight()*.5,0));last=next.position();LivingEntity previous=next;
                next=p.level.getEntitiesOfClass(LivingEntity.class,previous.getBoundingBox().inflate(6),t->PetalFeatures.canHit(p,t)&&PetalFeatures.bodyOwner(t)!=null&&!struck.contains(PetalFeatures.bodyOwner(t).getUUID())&&t.distanceToSqr(previous)<=36&&previous.canSee(t)&&p.canSee(t)).stream().min(Comparator.comparingDouble(t->t.distanceToSqr(previous))).orElse(null);
            }}
            if((move==1&&ticks==16)||(move==5&&ticks==60))sound(p,SoundEvents.GENERIC_EXPLODE,move==5?1.2F:.7F,.55F);
        });
        continuous.addEndEvent((p,a)->{if(p.level.isClientSide)return;if(field!=null&&((move!=1&&move!=5)||ticks<(move==5?60:16)))field.remove();if(move==2)AbilityHelper.setDeltaMovement(p,p.getDeltaMovement().scale(.2));if(DecayFeatures.action(p)==move)DecayFeatures.pose(p,-1,0);cooldownComponent.startCooldown(p,COOLDOWN[move]);field=null;target=null;});
        addRemoveEvent((p,a)->{if(continuous.isContinuous())continuous.stopContinuity(p);});
    }
    public boolean strike(LivingEntity p,LivingEntity target,float amount){
        LivingEntity body=PetalFeatures.bodyOwner(target);if(body==null||!PetalFeatures.canHit(p,body))return false;
        net.minecraft.util.DamageSource source=damage.getDamageSource(p);
        if((move==1||move==5)&&source instanceof xyz.pixelatedw.mineminenomi.init.ModDamageSource)((xyz.pixelatedw.mineminenomi.init.ModDamageSource)source).setUnavoidable();
        return damage.hurtTarget(p,body,amount,source);
    }
    public static boolean bypassesDodge(net.minecraft.util.DamageSource source){
        if(!(source instanceof AbilityDamageSource))return false;
        AbilityCore<?> core=((AbilityDamageSource)source).getAbilitySource();return core==CORES[1]||core==CORES[5];
    }
    public void mark(LivingEntity p,LivingEntity t,int power){LivingEntity body=PetalFeatures.bodyOwner(t);if(body==null||!body.isAlive())return;for(DecayField old:p.level.getEntitiesOfClass(DecayField.class,body.getBoundingBox().inflate(4),f->f.kind()==6&&f.target()==body))old.remove();DecayField.spawn(p,6,body.position(),61,power).source(move).target(body);}
    private static AbilityUseResult fail(String s){return AbilityUseResult.fail(new StringTextComponent(s));}
    private static void sound(LivingEntity p,SoundEvent s,float volume,float pitch){p.level.playSound(null,p.blockPosition(),s,SoundCategory.PLAYERS,volume,pitch);}
}
