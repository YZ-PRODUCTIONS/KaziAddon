package net.kazi.kazimod.leopard;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public final class TailGrabAbility extends Ability {
    public static final AbilityCore<TailGrabAbility> INSTANCE = new AbilityCore.Builder<>("Tail Grab",AbilityCategory.DEVIL_FRUITS,TailGrabAbility::new)
            .setUnlockCheck(LeopardFeatures::awakened).setIcon(new ResourceLocation("kazimod","textures/abilities/tail_grab.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.tail_grab.description"))
            .addAdvancedDescriptionLine(ChargeComponent.getTooltip(140),CooldownComponent.getTooltip(300))
            .setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(SourceType.FIST).build();
    private final GrabEntityComponent grab = new GrabEntityComponent(this,true,true,true,2);
    private final ContinuousComponent armed = new ContinuousComponent(this,true);
    private final ChargeComponent hold = new ChargeComponent(this);
    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final HitTriggerComponent hit = new HitTriggerComponent(this);
    private int ticks;
    public TailGrabAbility(AbilityCore<TailGrabAbility> core) {
        super(core);isNew=true;
        addComponents(grab,armed,hold,damage,hit,new PoolComponent(this,ModAbilityPools.GRAB_ABILITY),new RequireMorphComponent(this,LeopardFeatures.MORPH.get()));
        addCanUseCheck((u,a)->LeopardFeatures.empowered(u)?AbilityUseResult.success():AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.leopard.form_required")));
        addUseEvent((u,a)->{
            if(u.level.isClientSide)return;
            if(hold.isCharging()){hold.stopCharging(u);return;}
            if(armed.isContinuous()){grab.release(u);armed.stopContinuity(u);return;}
            if(grab.getState()==GrabEntityComponent.GrabState.IDLE&&grab.grabNearest(u,false))grab.triggerPulling(u);
            else armed.startContinuity(u);
        });
        hit.addTryHitEvent((u,t,s,a)->armed.isContinuous()&&!grab.hasGrabbedEntity()?HitTriggerComponent.HitResult.HIT:HitTriggerComponent.HitResult.PASS);
        hit.addOnHitEvent((u,t,s,a)->{if(grab.grabManually(u,t))grab.startPulling(u);t.addEffect(new EffectInstance(ModEffects.ANTI_KNOCKBACK.get(),1));return false;});
        grab.addPullStartEvent((u,a)->{armed.setThresholdTime(u,200);signal(u);});
        grab.addPullTickEvent((u,a)->{if(!u.level.isClientSide){if(!LeopardFeatures.empowered(u)){grab.release(u);cooldownComponent.startCooldown(u,300);}else if(u.tickCount%5==0)signal(u);}});
        grab.addPullEndEvent((u,a)->{armed.stopContinuity(u);if(grab.canContinueGrab(u))hold.startCharging(u,140);});
        grab.addReleaseEvent((u,target,a)->LeopardGrabPacket.send(u,-1));
        armed.addTickEvent((u,a)->{if(!u.level.isClientSide){if(!LeopardFeatures.empowered(u)){grab.release(u);armed.stopContinuity(u);}else if(u.tickCount%5==0)signal(u);}});
        armed.addEndEvent((u,a)->{if(!u.level.isClientSide){if(grab.getState()!=GrabEntityComponent.GrabState.GRABBED)grab.release(u);if(!grab.canContinueGrab(u))cooldownComponent.startCooldown(u,300);}});
        hold.addStartEvent((u,a)->{ticks=0;signal(u);});
        hold.addTickEvent((u,a)->{
            if(u.level.isClientSide)return;
            if(!LeopardFeatures.empowered(u)||canUse(u).isFail()||!grab.canContinueGrab(u)){hold.stopCharging(u);return;}
            u.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),2,1));
            if(++ticks%30==0)damage.hurtTarget(u,grab.getGrabbedEntity(),6);
            if(ticks%5==0)signal(u);
        });
        hold.addEndEvent((u,a)->{if(!u.level.isClientSide){grab.release(u);LeopardGrabPacket.send(u,-1);cooldownComponent.startCooldown(u,300);}});
    }
    private void signal(LivingEntity user){LeopardGrabPacket.send(user,grab.hasGrabbedEntity()?grab.getGrabbedEntity().getId():-1);}
}
