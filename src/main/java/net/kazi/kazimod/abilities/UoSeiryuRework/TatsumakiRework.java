package net.kazi.kazimod.abilities.UoSeiryuRework;

import java.util.List;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.abilities.uoseriyu.KaenDaikoAbility;
import net.MrMagicalCart.cartaddon.abilities.uoseriyu.UoSeiryuHelper;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class TatsumakiRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"cartaddon", (String)"tatsumaki", (Pair[])new Pair[]{ImmutablePair.of((Object)"The user coils their body into a raging vortex, summoning violent tornadoes that lift and crush nearby enemies.", null)});
    private static final int COOLDOWN = 1000;
    private static final int DURATION = 100;
    private static final float DAMAGE = 10.0f;
    private static final float RANGE = 25.0f;
    public static final AbilityCore<TatsumakiRework> INSTANCE = new AbilityCore.Builder<TatsumakiRework>("Tatsumaki", AbilityCategory.DEVIL_FRUITS, TatsumakiRework::new).addDescriptionLine(DESCRIPTION).addDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, RequireMorphComponent.getTooltip()}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip((float)10.0f), ContinuousComponent.getTooltip((float)100.0f), CooldownComponent.getTooltip((float)1000.0f), RangeComponent.getTooltip((float)25.0f, (RangeComponent.RangeType)RangeComponent.RangeType.AOE)}).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this).addStartEvent(this::onStart).addTickEvent(this::onTick).addEndEvent(this::onEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent((IAbility)this);
    private final RequireMorphComponent requireMorphComponent = new RequireMorphComponent((IAbility)this, (MorphInfo)CartMorphs.SEIRYU_FLY.get(), new MorphInfo[0]);
    private final AnimationComponent animationComponent = new AnimationComponent((IAbility)this);
    private final Interval damageInterval = new Interval(20);
    private final Interval particleInterval = new Interval(2);
    private int spiralAngle = 0;
    private final PoolComponent poolComponent = new PoolComponent((IAbility)this, CartAbilityPools.SEIRYU_ABILITY, new AbilityPool2[0]);

    public TatsumakiRework(AbilityCore<TatsumakiRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.animationComponent, this.continuousComponent, this.dealDamageComponent, this.requireMorphComponent});
        super.addCanUseCheck(UoSeiryuHelper::usingKaenDaiko);
        this.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, 100.0f);
    }

    private void onStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.YAW_SPIN);
        this.spiralAngle = 0;
        this.damageInterval.restartIntervalToZero();
        this.particleInterval.restartIntervalToZero();
    }

    private void onTick(LivingEntity entity, IAbility ability) {
        if (!entity.isAlive()) {
            return;
        }
        this.spawnTornadoParticles(entity);
        this.applyAOEDamage(entity);
        IAbilityData abilityDataProps = AbilityDataCapability.get((LivingEntity)entity);
        KaenDaikoAbility kaenDaikoAbility = (KaenDaikoAbility)abilityDataProps.getEquippedAbility(KaenDaikoAbility.INSTANCE);
        if (kaenDaikoAbility != null && kaenDaikoAbility.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void applyAOEDamage(LivingEntity entity) {
        if (!this.damageInterval.canTick()) {
            return;
        }
        AbilityDamageSource source = (AbilityDamageSource)this.dealDamageComponent.getDamageSource(entity);
        source.setMagic();
        source.setPiercing(0.35f);
        List<LivingEntity> targets = WyHelper.getNearbyLiving((Vector3d)entity.position(), (IWorld)entity.level, (double)25.0, (double)10.0, (double)25.0, (Predicate)ModEntityPredicates.getEnemyFactions((LivingEntity)entity));
        for (LivingEntity target : targets) {
            boolean flag = this.dealDamageComponent.hurtTarget(entity, target, 10.0f, (DamageSource)source);
            if (!flag) continue;
            target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 20, 1));
            // Requested customization: fling targets away instead of preventing knockback.
            Vector3d away = target.position().subtract(entity.position());
            if (away.lengthSqr() < 0.01D) away = entity.getLookAngle();
            away = away.normalize().scale(2.75D).add(0.0D, 0.85D, 0.0D);
            AbilityHelper.setDeltaMovement(target, away.x, away.y, away.z);
            target.hurtMarked = true;
            target.addEffect(new EffectInstance(Effects.LEVITATION, 20, 2));
        }
    }

    private void spawnTornadoParticles(LivingEntity entity) {
        if (!this.particleInterval.canTick() || entity.level.isClientSide) {
            return;
        }
        double radius = 25.0;
        double heightStep = 2.0;
        int circles = 10;
        int particlesPerCircle = 6;
        this.spiralAngle += 6;
        if (this.spiralAngle > 360) {
            this.spiralAngle = 0;
        }
        double entityY = entity.getY() - 4.0;
        for (int i = 0; i < circles; ++i) {
            for (int j = 0; j < particlesPerCircle; ++j) {
                double angleRad = Math.toRadians((double)this.spiralAngle + 360.0 / (double)particlesPerCircle * (double)j);
                double x = entity.getX() + radius * Math.cos(angleRad);
                double z = entity.getZ() + radius * Math.sin(angleRad);
                double y = entityY + heightStep * (double)i;
                WyHelper.spawnParticleEffect((ParticleEffect)((ParticleEffect)ModParticleEffects.O_TATSUMAKI.get()), (Entity)entity, (double)x, (double)y, (double)z);
            }
        }
    }

    private void onEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 1000.0f);
    }
}

