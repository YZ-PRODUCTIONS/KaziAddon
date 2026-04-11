package net.kazi.kazimod.abilities.AkumaRework;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class FullCounterAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "full_counter",
                    new Pair[]{ImmutablePair.of(
                            "Activate before being hit to reflect the attack back at the attacker with equal damage.", null)});

    private static final float CONTINUOUS_DURATION = 42.0F; // 2.1 second window
    private static final float COOLDOWN = 300.0F; // 15 seconds

    public static final AbilityCore<FullCounterAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::startContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private final DamageTakenComponent damageTakenComponent;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    private boolean countered = false;

    public FullCounterAbility(AbilityCore<FullCounterAbility> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.damageTakenComponent,
                this.dealDamageComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            this.countered = false;
            this.continuousComponent.startContinuity(entity, CONTINUOUS_DURATION);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.5F);
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!this.continuousComponent.isContinuous() || this.countered) {
            return damage;
        }

        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) sourceEntity;

            // Reflect the damage back
            this.dealDamageComponent.hurtTarget(entity, attacker, damage);

            // Visual feedback
            if (!entity.level.isClientSide) {
                for (int i = 0; i < 10; i++) {
                    WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld) entity.level,
                            attacker.getX() + (entity.getRandom().nextDouble() - 0.5) * 2.0,
                            attacker.getY() + (double) attacker.getEyeHeight(),
                            attacker.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2.0);
                }
                WyHelper.spawnParticles(ParticleTypes.EXPLOSION, (ServerWorld) entity.level,
                        entity.getX(), entity.getY() + 1.0, entity.getZ());
            }

            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                    SoundCategory.PLAYERS, 3.0F, 0.5F);

            this.countered = true;
            this.continuousComponent.stopContinuity(entity);
            return 0.0F;
        }

        return damage;
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Full Counter", AbilityCategory.DEVIL_FRUITS, FullCounterAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(CONTINUOUS_DURATION),
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
