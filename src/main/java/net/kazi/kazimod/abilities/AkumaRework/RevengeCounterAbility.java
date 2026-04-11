package net.kazi.kazimod.abilities.AkumaRework;

import java.util.List;
import java.util.Optional;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class RevengeCounterAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "revenge_counter",
                    new Pair[]{ImmutablePair.of(
                            "Activate to charge for 5 seconds, becoming invincible, then release double the stored damage from Revenge Passive in a massive AOE.", null)});

    private static final float CHARGE_TIME = 100.0F; // 5 seconds
    private static final float COOLDOWN = 900.0F; // 45 seconds
    private static final float RANGE = 50.0F;

    public static final AbilityCore<RevengeCounterAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::duringChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final DamageTakenComponent damageTakenComponent;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    private boolean isCharging = false;
    private Optional<RevengePassiveAbility> passive = Optional.empty();

    public RevengeCounterAbility(AbilityCore<RevengeCounterAbility> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.damageTakenComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.hitTrackerComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private Optional<RevengePassiveAbility> getPassive(LivingEntity entity) {
        if (!this.passive.isPresent()) {
            IAbilityData data = AbilityDataCapability.get(entity);
            RevengePassiveAbility p = (RevengePassiveAbility) data.getPassiveAbility(RevengePassiveAbility.INSTANCE);
            this.passive = Optional.ofNullable(p);
        }
        return this.passive;
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        Optional<RevengePassiveAbility> p = getPassive(entity);
        if (!this.chargeComponent.isCharging() && p.isPresent() && p.get().getStoredDamage() > 0) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        // During charge, player is invincible
        if (this.isCharging) {
            return 0.0F;
        }
        return damage;
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.isCharging = true;
        this.hitTrackerComponent.clearHits();

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.3F);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        // Invincible during charge + visual
        entity.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 2, 255, false, false));
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));

        // Dark aura particles
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 8; i++) {
                double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                double dist = entity.getRandom().nextDouble() * 2.0;
                double px = entity.getX() + Math.cos(angle) * dist;
                double pz = entity.getZ() + Math.sin(angle) * dist;
                double py = entity.getY() + entity.getRandom().nextDouble() * 2.5;
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.SMOKE,
                        px, py, pz, 1, 0, 0.1, 0, 0.02);
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.LARGE_SMOKE,
                        px, py, pz, 1, 0, 0.1, 0, 0.02);
            }
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.isCharging = false;

        Optional<RevengePassiveAbility> p = getPassive(entity);
        float storedDamage = p.isPresent() ? p.get().getStoredDamage() : 0.0F;
        float releaseDamage = storedDamage * 2.0F;

        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);

        for (LivingEntity target : targets) {
            if (this.hitTrackerComponent.canHit(target)) {
                this.dealDamageComponent.hurtTarget(entity, target, releaseDamage);
            }
        }

        // Massive explosion particles
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 100; i++) {
                double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                double dist = entity.getRandom().nextDouble() * RANGE;
                double px = entity.getX() + Math.cos(angle) * dist;
                double pz = entity.getZ() + Math.sin(angle) * dist;
                double py = entity.getY() + entity.getRandom().nextDouble() * 5.0;
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.EXPLOSION,
                        px, py, pz, 1, 0, 0, 0, 0);
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.LARGE_SMOKE,
                        px, py, pz, 1, 0, 0.2, 0, 0.1);
            }
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 5.0F, 0.5F);

        // Reset stored damage in the passive
        if (p.isPresent()) {
            p.get().resetStoredDamage(entity);
        }

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Revenge Counter", AbilityCategory.DEVIL_FRUITS, RevengeCounterAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
