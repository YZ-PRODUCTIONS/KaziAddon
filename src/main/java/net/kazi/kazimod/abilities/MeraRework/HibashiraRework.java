package net.kazi.kazimod.abilities.MeraRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import xyz.pixelatedw.mineminenomi.abilities.mera.MeraHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.mera.HibashiraParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Kazi-owned baseline copy of Mera's Hibashira for later rework changes. */
public class HibashiraRework extends Ability {
    private static final int ON_HOLD = 100;
    private static final int MIN_COOLDOWN = 200;
    private static final double PILLAR_SIZE = 3.5D;
    private static final float DAMAGE = 5.0F;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "hibashira", ImmutablePair.of(
                    "Creates a fire pillar extending both upwards and downwards, burning every enemy within it.", null));

    public static final AbilityCore<HibashiraRework> INSTANCE = new AbilityCore.Builder<>(
            "Hibashira", AbilityCategory.DEVIL_FRUITS, HibashiraRework::new)
            .addDescriptionLine(DESCRIPTION)
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    CooldownComponent.getTooltip(MIN_COOLDOWN, 250.0F),
                    ContinuousComponent.getTooltip(ON_HOLD), DealDamageComponent.getTooltip(DAMAGE))
            .setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.FIRE)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/hibashira.png"))
            .build();

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(this::onContinuityStart).addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final Interval particleInterval = new Interval(2);
    private final Interval clearHitsInterval = new Interval(20);

    public HibashiraRework(AbilityCore<HibashiraRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(continuousComponent, hitTrackerComponent, dealDamageComponent);
        this.addCanUseCheck(MeraHelper::canUseMeraAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        continuousComponent.triggerContinuity(entity, ON_HOLD);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        hitTrackerComponent.clearHits();
        particleInterval.restartIntervalToZero();
        ((ModDamageSource) dealDamageComponent.getDamageSource(entity)).setUnavoidable();
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (super.canUse(entity).isFail()) continuousComponent.stopContinuity(entity);
        if (particleInterval.canTick()) {
            WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.HIBASHIRA.get(), entity,
                    entity.getX(), entity.getY(), entity.getZ(), HibashiraParticleEffect.NO_DETAILS);
        }
        for (LivingEntity target : WyHelper.getNearbyLiving(entity.getEyePosition(1.0F), entity.level,
                PILLAR_SIZE, 10.0D, PILLAR_SIZE, ModEntityPredicates.getEnemyFactions(entity))) {
            if (hitTrackerComponent.canHit(target) && dealDamageComponent.hurtTarget(entity, target, DAMAGE)) {
                target.setSecondsOnFire(4);
            }
        }
        if (clearHitsInterval.canTick()) hitTrackerComponent.clearHits();
        AbilityHelper.slowEntityFall(entity);
        entity.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        cooldownComponent.startCooldown(entity, MIN_COOLDOWN + continuousComponent.getContinueTime() / 2.0F);
    }
}
