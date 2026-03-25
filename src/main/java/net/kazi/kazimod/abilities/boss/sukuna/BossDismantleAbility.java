package net.kazi.kazimod.abilities.boss.sukuna;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.List;

/**
 * NPC-safe Dismantle for the Sukuna boss.
 * Fixes vs player version:
 *  - No DashComboComponent / AltModeComponent.
 *  - Sound uses null not (PlayerEntity) null cast — safe for NPC callers.
 *  - Short charge (15 ticks) so isCharging() stays true during the goal tick.
 *  - No setUnlockCheck — boss bypasses the hasAwakenedFruit() gate.
 */
public class BossDismantleAbility extends Ability {

    private static final int   CHARGE_TIME = 15;
    private static final float COOLDOWN    = 60.0F;
    private static final int   DISTANCE    = 38;
    private static final float WIDTH       = 3.0F;

    public static final AbilityCore<BossDismantleAbility> INSTANCE;

    private final ChargeComponent     chargeComponent     = new ChargeComponent(this)
            .addStartEvent(this::onChargeStart)
            .addEndEvent(this::onChargeEnd);
    private final AnimationComponent  animationComponent  = new AnimationComponent(this);
    private final RangeComponent      rangeComponent      = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public BossDismantleAbility(AbilityCore<BossDismantleAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent, this.animationComponent,
                this.rangeComponent,  this.dealDamageComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (ModAnimations.UPPER_SLASH != null) {
            this.animationComponent.start(entity, ModAnimations.UPPER_SLASH, 10);
        }
        if (!entity.level.isClientSide) {
            entity.level.playSound(null, entity.blockPosition(),
                    KaziSounds.DISMANTLE_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        if (entity.level.isClientSide) return;

        // 20% of target max HP — matches normal-mode Dismantle
        float percentDamage = entity.getMaxHealth() * 0.075F;

        List<LivingEntity> targets = this.rangeComponent.getTargetsInLine(entity, DISTANCE, WIDTH);
        for (LivingEntity target : targets) {
            AbilityDamageSource source =
                    (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
            source.setInternal();
            source.setSlash();
            source.markIndirectDamage();
            source.setUnavoidable();
            source.bypassArmor();

            if (this.dealDamageComponent.hurtTarget(entity, target, percentDamage, source)) {
                Vector3d dist = target.position().subtract(entity.position())
                        .add(0.0, -1.0, 0.0).normalize();
                AbilityHelper.setDeltaMovement(target,
                        -dist.x * 4.5, 0.1, -dist.z * 4.5);

                entity.level.playSound(null, target.blockPosition(),
                        KaziSounds.CLEAVE_HIT_SFX.get(), SoundCategory.PLAYERS, 4.0F, 1.0F);
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.DISMANTLE.get(),
                        entity, target.getX(), target.getEyeY(), target.getZ());
                break;
            }
        }
        super.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    public boolean isCharging() {
        return this.chargeComponent.isCharging();
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Boss: Dismantle",
                AbilityCategory.DEVIL_FRUITS, BossDismantleAbility::new)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INTERNAL})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .build();
    }
}