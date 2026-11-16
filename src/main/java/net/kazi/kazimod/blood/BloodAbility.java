package net.kazi.kazimod.blood;

import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.util.*;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;

public final class BloodAbility extends Ability {
    private static final String[] IDS = {"piercing_blood", "blood_supernova", "slicing_exorcism", "blood_connection", "flowing_red_scale"};
    private static final String[] NAMES = {"Piercing Blood", "Supernova", "Slicing Exorcism", "Blood Connection", "Flowing Red Scale"};
    private static final int[] COOL = {200,360,160,280,600};
    public static final AbilityCore<BloodAbility>[] CORES = cores();
    // Keep the old registry entry readable, but never unlock or run it.
    static final AbilityCore<BloodAbility> RETIRED_EDGE = new AbilityCore.Builder<BloodAbility>(
            "Blood Edge", "chi_blood_edge", AbilityCategory.DEVIL_FRUITS, AbilityType.ACTION,
            core -> new BloodAbility(core,3,0)).setUnlockCheck(p -> false).build();

    static AbilityCore<BloodAbility> core(int kind) {
        // Projectile, pose and icon IDs keep their original values; 3 is retired.
        if (kind < 0 || kind == 3 || kind > CORES.length) return null;
        return CORES[kind < 3 ? kind : kind - 1];
    }

    @SuppressWarnings("unchecked") private static AbilityCore<BloodAbility>[] cores() {
        AbilityCore<BloodAbility>[] out = new AbilityCore[IDS.length];
        for (int i=0;i<out.length;i++) {
            final int move=i<3?i:i+1, cooldown=COOL[i];
            out[i]=new AbilityCore.Builder<BloodAbility>(NAMES[i], "chi_"+IDS[i], AbilityCategory.DEVIL_FRUITS, AbilityType.ACTION,
                    core -> new BloodAbility(core,move,cooldown)).setUnlockCheck(BloodFeatures::fruit)
                    .setSourceHakiNature(SourceHakiNature.IMBUING).setSourceElement(SourceElement.NONE)
                    .setSourceType(move==2?SourceType.SLASH:move==4?SourceType.FIST:SourceType.PROJECTILE)
                    .setIcon(new ResourceLocation("kazimod","textures/abilities/blood/"+move+".png"))
                    .addDescriptionLine(new TranslationTextComponent("ability.kazimod.chi_"+IDS[i]+".description"))
                    .addDescriptionLine(new TranslationTextComponent(move==5?"ability.kazimod.blood_scale_debuffs":"ability.kazimod.blood_hit_debuffs"))
                    .addAdvancedDescriptionLine(CooldownComponent.getTooltip(cooldown)).build();
        }
        return out;
    }
    public final int move;
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private final ChangeStatsComponent stats=new ChangeStatsComponent(this);
    private BloodProjectile shot;
    private UUID receipt;
    private int ticks;
    private boolean released, committed;
    private BloodAbility(AbilityCore<BloodAbility> core,int move,int cooldown) {
        super(core); this.move=move; isNew=true; addComponents(continuous,stats);
        if(move==3) addCanUseCheck((p,a)->fail("unavailable"));
        else configure(cooldown);
    }
    private void configure(int cooldown) {
        if(move==5){
            modifier(Attributes.ATTACK_DAMAGE,BloodFeatures.SCALE_DAMAGE,.15,AttributeModifier.Operation.MULTIPLY_TOTAL);
            modifier(Attributes.MOVEMENT_SPEED,BloodFeatures.SCALE_SPEED,.2,AttributeModifier.Operation.MULTIPLY_TOTAL);
            modifier(Attributes.KNOCKBACK_RESISTANCE,BloodFeatures.SCALE_RESIST,.25,AttributeModifier.Operation.ADDITION);
        }
        addCanUseCheck((p,a)->{
            if(!BloodFeatures.usable(p))return fail("unavailable");
            if(active())return AbilityUseResult.success();
            if(BloodFeatures.action(p)>=0)return fail("busy");
            return AbilityUseResult.success();
        });
        addUseEvent((p,a)->{
            if(p.level.isClientSide)return;
            if(active()){
                if(move==0&&!released){if(ticks>=minimumCharge(p))release(p);return;}
                if(move==1&&shot!=null&&shot.isAlive()){if(shot.phase()==0)shot.launch();else if(shot.phase()==1)shot.burst();return;}
                if(move==2&&shot!=null&&shot.isAlive()){shot.curve();return;}
                continuous.stopContinuity(p);return;
            }
            ticks=0;shot=null;receipt=null;released=false;committed=false;
            BloodFeatures.state(p).remove("Released");
            BloodFeatures.state(p).remove("NovaAt");
            continuous.triggerContinuity(p,move==5?300:move==1?100:move==2?64:move==0?110:16);
            BloodFeatures.pose(p,move,move==0?110:move==5?18:14);
            if(move==5){BloodFeatures.state(p).putLong("ScaleEnd",p.level.getGameTime()+300);stats.applyModifiers(p);BloodFeatures.sync(p);}
        });
        continuous.addTickEvent((p,a)->{
            if(p.level.isClientSide)return;
            ticks++;
            if(!BloodFeatures.usable(p)){continuous.stopContinuity(p);return;}
            if(move==5)return;
            if(ticks==4){receipt=BloodLedger.begin(p,move==0||move==1?120:move==2?76:90,4);committed=true;}
            if(move==0){
                if(!released&&ticks>=(BloodFeatures.scale(p)?22:30))release(p);
                if(released&&(shot==null||!shot.isAlive()||!shot.steered()||shot.phase()==2&&shot.phaseAge(0)>=6))continuous.stopContinuity(p);
            }else if(ticks==(move==1?10:8)){
                shot=new BloodProjectile(p,move,receipt,move==1?24:move==2?18:8);
                p.level.addFreshEntity(shot);
                sound(p,move==1?SoundEvents.BUBBLE_COLUMN_BUBBLE_POP:SoundEvents.TRIDENT_THROW,.65F);
            }
            if((move==1||move==2)&&ticks>12&&(shot==null||!shot.isAlive()))continuous.stopContinuity(p);
        });
        continuous.addEndEvent((p,a)->{
            if(p.level.isClientSide)return;
            if(move==5){BloodFeatures.state(p).putLong("ScaleEnd",0);stats.removeModifiers(p);}
            if(committed&&shot==null)BloodLedger.miss(p,receipt);
            if(move==0&&shot!=null)shot.stopFiring();
            if(move==1&&shot!=null&&shot.phase()==0)shot.burst();
            if(BloodFeatures.action(p)==move)BloodFeatures.pose(p,-1,0);
            BloodFeatures.state(p).putBoolean("AimLocked",false);
            cooldownComponent.startCooldown(p,cooldown); BloodFeatures.sync(p);
        });
        addRemoveEvent((p,a)->{if(active())continuous.stopContinuity(p);stats.removeModifiers(p);});
    }
    private void modifier(Attribute attr,UUID id,double amount,AttributeModifier.Operation op){stats.addAttributeModifier(attr,new AbilityAttributeModifier(id,this,"Blood technique",amount,op),p->active()&&BloodFeatures.usable(p));}
    private int minimumCharge(LivingEntity p){return BloodFeatures.scale(p)?8:12;}
    private void release(LivingEntity p){
        released=true;float power=ticks>=(BloodFeatures.scale(p)?19:27)?34:26;
        shot=new BloodProjectile(p,0,receipt,power);
        p.level.addFreshEntity(shot);
        BloodFeatures.state(p).putInt("FiringTicks",shot.firingTicks());
        BloodFeatures.state(p).putLong("Released",p.level.getGameTime());BloodFeatures.sync(p);
        sound(p,SoundEvents.TRIDENT_THROW,.55F);
    }
    public boolean active(){return continuous.isContinuous();}
    public void interrupt(LivingEntity p){if(move==0&&!released&&active())continuous.stopContinuity(p);}
    private static AbilityUseResult fail(String key){return AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.blood."+key));}
    static void sound(LivingEntity p,SoundEvent event,float pitch){p.level.playSound(null,p.blockPosition(),event,SoundCategory.PLAYERS,.65F,pitch);}
}
