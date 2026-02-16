package net.kazi.kazimod.abilities.onirework;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SkullBasherRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "skull_basher", new Pair[]{ImmutablePair.of("The user collides their skull with any target within range, applying bleeding.", (Object)null)});
    private static final float HOLD_TIME = 30.0F;
    private static final float CHARGE = 20.0F;
    private static final int MAX_COOLDOWN = 300;
    private static final float RANGE = 2.0F;
    private static final float DAMAGE = 25.0F;
    public static final AbilityCore<SkullBasherRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;
    private final PoolComponent poolComponent;
    private Interval particleInterval = new Interval(2);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);

    public SkullBasherRework(AbilityCore<SkullBasherRework> core) {
        super(core);
        this.isNew = true;
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY);
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.changeStatsComponent, this.continuousComponent, this.rangeComponent, this.dealDamageComponent, this.hitTrackerComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, 15.0F);
        }

        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }

    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.particleInterval.canTick()) {
            Vector3d look = entity.getLookAngle().normalize().scale((double)-2.5F);
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.SKULL_BASHER.get(), entity, entity.getX() + look.x, entity.getY() + (double)2.0F, entity.getZ() + look.z);
        }

        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, 15.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.changeStatsComponent.applyModifiers(entity);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            Vector3d look = entity.getLookAngle();
            Vector3d speed = look.multiply(1.35, (double)0.0F, 1.35);
            entity.move(MoverType.SELF, speed);
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 1.2F);
            Iterator var = targets.iterator();

            LivingEntity target;
            for(Vector3d knockbackVec = speed.multiply((double)1.40F, (double)1.25F, (double)1.40F); var.hasNext(); AbilityHelper.setDeltaMovement(target, knockbackVec.x, 1.4, knockbackVec.z)) {
                target = (LivingEntity)var.next();
                if (this.hitTrackerComponent.canHit(target)) {
                    this.dealDamageComponent.hurtTarget(entity, target, 60.0F);
                    target.addEffect(new EffectInstance((Effect)ModEffects.BLEEDING.get(), 60, 0, false, false));
                    target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
                    this.continuousComponent.stopContinuity(entity);
                }
            }
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.cooldownComponent.startCooldown(entity, 240.0F + this.continuousComponent.getContinueTime() * 2.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        IEntityStats props = EntityStatsCapability.get(entity);
        boolean race = props.getRace().equals(CartValues.ONI);
        return race && props.getDoriki() >= (double)2500.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Skull Basher", AbilityCategory.RACIAL, SkullBasherRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(25.0F), ChargeComponent.getTooltip(30.0F), ContinuousComponent.getTooltip(20.0F), CooldownComponent.getTooltip(240.0F, 300.0F), RangeComponent.getTooltip(1.8F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.HARDENING).setUnlockCheck(SkullBasherRework::canUnlock).build();
        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(UUID.fromString("ecff1ac2-77c2-445e-b42d-be8428a775b0"), INSTANCE, "Skull Basher Step Height Modifier", (double)1.0F, Operation.ADDITION);
    }
}

