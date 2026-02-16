//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KitsuneRework;

import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent.GrabState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;

public class FoxAssaultRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "fox_assault", new Pair[]{ImmutablePair.of("The user dashes forward and grabs a nearby target, holding them in place before launching them into the ground with a powerful kick.", (Object)null)});
    private static final int COOLDOWN = 400;
    private static final int HOLD_TIME = 20;
    private static final float RANGE = 2.5F;
    public static final AbilityCore<FoxAssaultRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::onContinuityStart).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final GrabEntityComponent grabEntityComponent;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);
    private final PoolComponent poolComponent;
    private final HitTrackerComponent hitTrackerComponent;
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final DamageTakenComponent damageTakenComponent;
    private final RequireMorphComponent requireMorphComponent;
    boolean hasFallDamage;
    public static int overuse = 100;

    public FoxAssaultRework(AbilityCore<FoxAssaultRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.INIT_JUMP, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
        this.hitTrackerComponent = new HitTrackerComponent(this);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.KITSUNE_HYBRID.get(), new MorphInfo[]{(MorphInfo)CartMorphs.KITSUNE_WALK.get()});
        this.hasFallDamage = false;
        this.grabEntityComponent = new GrabEntityComponent(this, true, false, true, 2.0F);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.damageTakenComponent, this.hitTrackerComponent, this.chargeComponent, this.poolComponent, this.dealDamageComponent, this.continuousComponent, this.rangeComponent, this.grabEntityComponent, this.requireMorphComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!WyHelper.isInChallengeDimension(entity.level)) {
            boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, overuse);
            if (isOnMaxOveruse) {
                return;
            }
        }

        if (!this.continuousComponent.isContinuous() && !this.chargeComponent.isCharging()) {
            this.continuousComponent.triggerContinuity(entity, 30.0F);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        if (!entity.level.isClientSide) {
            Vector3d look = entity.getLookAngle().normalize();
            Vector3d speed = look.scale(entity.isOnGround() ? (double)4.0F : (double)3.0F);
            AbilityHelper.setDeltaMovement(entity, speed.x, speed.y, speed.z);
        }

        this.animationComponent.start(entity, ModAnimations.PHOENIX_KICK);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            LivingEntity grabbedTarget = this.grabEntityComponent.getGrabbedEntity();
            if (!super.canUse(entity).isFail() && (grabbedTarget == null || this.grabEntityComponent.canContinueGrab(entity))) {
                if (grabbedTarget == null) {
                    this.grabEntityComponent.grabNearest(entity, 4.0F, 1.0F, false);
                }
            } else {
                this.continuousComponent.stopContinuity(entity);
            }

            if (this.grabEntityComponent.hasGrabbedEntity()) {
                this.continuousComponent.stopContinuity(entity);
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.grabEntityComponent.getState() != GrabState.GRABBED) {
                this.grabEntityComponent.release(entity);
            }

            if (!this.grabEntityComponent.canContinueGrab(entity)) {
                super.cooldownComponent.startCooldown(entity, 500.0F);
            } else {
                this.chargeComponent.startCharging(entity, 10.0F);
            }
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hasFallDamage = false;
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (!super.canUse(entity).isFail() && this.grabEntityComponent.canContinueGrab(entity)) {
                LivingEntity grabbedTarget = this.grabEntityComponent.getGrabbedEntity();
                entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 2, 1));
                grabbedTarget.addEffect(new EffectInstance((Effect)ModEffects.GRABBED.get(), 2, 3));
                grabbedTarget.teleportTo(entity.getX(), entity.getY(), entity.getZ());
            } else {
                this.chargeComponent.stopCharging(entity);
            }
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.grabEntityComponent.hasGrabbedEntity()) {
            LivingEntity grabbedTarget = this.grabEntityComponent.getGrabbedEntity();
            if (this.dealDamageComponent.hurtTarget(entity, grabbedTarget, 50.0F)) {
                grabbedTarget.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 40, 0));
            }

            // Launch them down into the ground
            Vector3d look = entity.getLookAngle().multiply((double)4.0F, (double)4.0F, (double)4.0F).add((double)0.0F, (double)-20.0F, (double)0.0F);
            AbilityHelper.setDeltaMovement(grabbedTarget, look);

            this.grabEntityComponent.release(entity);
        }

        if (entity instanceof PlayerEntity) {
            super.cooldownComponent.startCooldown(entity, 400.0F);
        } else {
            super.cooldownComponent.startCooldown(entity, 300.0F);
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (!this.hasFallDamage && damageSource == DamageSource.FALL) {
            this.hasFallDamage = true;
            return 0.0F;
        } else {
            return damage;
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Fox Assault", AbilityCategory.DEVIL_FRUITS, FoxAssaultRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(10.0F, 65.0F), ChargeComponent.getTooltip(20.0F), ContinuousComponent.getTooltip(30.0F), CooldownComponent.getTooltip(500.0F), RangeComponent.getTooltip(2.5F, RangeType.LINE)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, RequireMorphComponent.getTooltip()}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).build();
    }
}