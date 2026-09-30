package net.kazi.kazimod.abilities.VampAwaken;

import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.particles.effects.battovampire.PhantomVeilParticleEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Cart 0.7.5 Phantom Veil, with only its Zoan-form requirement removed. */
public class AwakenedPhantomVeilAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "cartaddon", "phantom_veil",
            new Pair[]{ImmutablePair.of(
                    "Creates a zone of smoke that reduces incoming damage by 50% and confuses all those nearby. (Stuns entities)",
                    null)});
    private static final int ON_HOLD = 100;
    private static final int MIN_COOLDOWN = 400;
    private static final int MAX_COOLDOWN = 450;
    private static final double RANGE = 20.0D;
    private static final float DAMAGE = 10.0F;
    private static final ResourceLocation ICON =
            new ResourceLocation("cartaddon", "textures/abilities/phantom_veil.png");

    public static final AbilityCore<AwakenedPhantomVeilAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final Interval particleInterval = new Interval(2);
    private final Interval clearHitsInterval = new Interval(20);
    private final DamageTakenComponent damageTakenComponent =
            new DamageTakenComponent(this, this::damageTakenEvent, DamageTakenComponent.DamageState.HURT);

    public AwakenedPhantomVeilAbility(AbilityCore<AwakenedPhantomVeilAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(
                this.damageTakenComponent,
                this.dealDamageComponent,
                this.continuousComponent,
                this.hitTrackerComponent);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, ON_HOLD);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.particleInterval.restartIntervalToZero();
        ((ModDamageSource) this.dealDamageComponent.getDamageSource(entity)).setUnavoidable();
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.VANISH.get(), 5, 0, false, false));
        if (super.canUse(entity).isFail()) {
            this.continuousComponent.stopContinuity(entity);
        }

        if (this.particleInterval.canTick()) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect<?>) CartParticleEffects.PHANTOM_VAIL.get(),
                    entity,
                    entity.getX(), entity.getY(), entity.getZ(),
                    new PhantomVeilParticleEffect.Details(5.0F, 4.0F, 4.0F));
        }

        for (LivingEntity target : WyHelper.getNearbyLiving(
                entity.position(), entity.level, RANGE, 10.0D, RANGE,
                ModEntityPredicates.getEnemyFactions(entity))) {
            if (this.hitTrackerComponent.canHit(target)
                    && this.dealDamageComponent.hurtTarget(entity, target, DAMAGE)) {
                target.addEffect(new EffectInstance((Effect) ModEffects.BLEEDING.get(), 100, 0));
                target.addEffect(new EffectInstance(Effects.CONFUSION, 100, 1));
                if (!(target instanceof PlayerEntity)) {
                    target.addEffect(new EffectInstance(
                            (Effect) ModEffects.MOVEMENT_BLOCKED.get(), 15, 0, false, false));
                }
            }
        }

        if (this.clearHitsInterval.canTick()) {
            this.hitTrackerComponent.clearHits();
        }

        AbilityHelper.slowEntityFall(entity);
        entity.addEffect(new EffectInstance(
                (Effect) ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        float cooldown = MIN_COOLDOWN + this.continuousComponent.getContinueTime() / 2.0F;
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    private float damageTakenEvent(LivingEntity entity, IAbility ability,
                                   DamageSource damageSource, float damage) {
        if (AbilityHelper.isDodging(entity)) return damage;
        return this.continuousComponent.isContinuous() ? damage * 0.5F : damage;
    }

    private static boolean canUnlock(LivingEntity entity) {
        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        return devilFruit != null
                && devilFruit.hasAwakenedFruit()
                && devilFruit.hasDevilFruit(CartAbilities.BATTO_BATTO_NO_MI_MODEL_VAMPIRE);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Phantom Veil", AbilityCategory.DEVIL_FRUITS, AwakenedPhantomVeilAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(MIN_COOLDOWN, MAX_COOLDOWN),
                        ContinuousComponent.getTooltip(ON_HOLD),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip((float) RANGE, RangeComponent.RangeType.AOE),
                        ChangeStatsComponent.getTooltip())
                .setIcon(ICON)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.SMOKE)
                .setUnlockCheck(AwakenedPhantomVeilAbility::canUnlock)
                .build();
    }
}
