package net.kazi.kazimod.abilities.Toki;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kazi.kazimod.entities.TimeBubbleEntity;
import net.kazi.kazimod.abilities.Toki.TimeBarAbility;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ChronostasisAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "chronostasis",
            new Pair[]{
                    ImmutablePair.of(
                            "Charges up a temporal pulse, then releases a 1-second AOE burst. " +
                                    "Any entity caught inside is frozen in time — suspended in a bubble, " +
                                    "lifted into the air, and left unable to move or act for 5 seconds.",
                            (Object) null
                    )
            }
    );

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float  COOLDOWN        = 600.0F;
    private static final float  CHARGE_TICKS    = 40.0F;
    private static final float  RANGE           = 20.0F;
    /** Ticks 0‒19: AOE window open (1 second). */
    private static final int    AOE_WINDOW_END  = 20;
    /** Ticks 20‒119: stun + float (5 seconds). Total hold = 120 ticks. */
    private static final float  TOTAL_HOLD      = 120.0F;
    private static final int    STUN_DURATION   = 5;
    private static final int    BUBBLE_LIFE     = 140; // stun(100) + aoe window(20) + extra second(20)
    private static final double FLOAT_HEIGHT    = 3.0;
    private static final double RISE_SPEED      = 0.12;
    private static final float  TIME_COST       = 100.0F;

    public static final AbilityCore<ChronostasisAbility> INSTANCE;

    // ── Components ────────────────────────────────────────────────────────────
    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    /** Single continuous component handles both the AOE window and the stun phase. */
    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addTickEvent(this::onContinuousTick)
                    .addEndEvent(this::onContinuousEnd);

    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    // ── Runtime state ─────────────────────────────────────────────────────────
    private SphereEntity sphereEntity;
    private final List<Integer>        caughtIds    = new ArrayList<>();
    private final Map<Integer, Double> targetFloatY = new HashMap<>();
    /** Counts up from 0 each tick of the continuous phase. */
    private int continuousTick = 0;

    // ── Constructor ───────────────────────────────────────────────────────────
    public ChronostasisAbility(AbilityCore<ChronostasisAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.rangeComponent,
                this.animationComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    // ── Use ───────────────────────────────────────────────────────────────────
    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) return;

        if (this.continuousComponent.isContinuous()) {
            if (this.continuousTick >= AOE_WINDOW_END) {
                this.continuousComponent.triggerContinuity(entity, TOTAL_HOLD);
            }
            return;
        }

        TimeBarAbility bar = getTimeBar(entity);
        if (bar == null || !bar.spendTimePoints(entity, TIME_COST)) {
            if (entity instanceof PlayerEntity) {
                ((PlayerEntity) entity).displayClientMessage(
                        new net.minecraft.util.text.TranslationTextComponent("Not Enough Time Points"),
                        true
                );
            }
            return;
        }

        this.chargeComponent.startCharging(entity, CHARGE_TICKS);
    }

    // ── Phase 1: Charge ───────────────────────────────────────────────────────
    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, KaziAnimations.CHRONOSTASIS);

        if (CommonConfig.INSTANCE.isExperiementalSpheresEnabled()) {
            this.sphereEntity = new SphereEntity(entity.level, entity);
            this.sphereEntity.setColor(new Color(100, 200, 255, 80));
            this.sphereEntity.setRadius(0.35F);
            this.sphereEntity.setDetailLevel(16);
            this.sphereEntity.setAnimationSpeed(1);
            double[] handPos = getHandPos(entity);
            this.sphereEntity.setPos(handPos[0], handPos[1], handPos[2]);
            entity.level.addFreshEntity(this.sphereEntity);
        }
        entity.level.playSound(
                (PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.ROOM_CREATE_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.9F
        );
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (CommonConfig.INSTANCE.isExperiementalSpheresEnabled() && this.sphereEntity != null) {
            this.sphereEntity.setRadius(0.35F);
            double[] handPos = getHandPos(entity);
            this.sphereEntity.setPos(handPos[0], handPos[1], handPos[2]);
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        this.animationComponent.stop(entity);

        // Do NOT remove the sphere here — let it stay visible during the AOE window.
        // It will be removed at the end of tick AOE_WINDOW_END - 1.



        this.caughtIds.clear();
        this.targetFloatY.clear();
        this.continuousTick = 0;

        this.continuousComponent.startContinuity(entity, TOTAL_HOLD);
    }

    // ── Phase 2 + 3: Single continuous tick ───────────────────────────────────
    private void onContinuousTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        if (this.continuousTick < AOE_WINDOW_END) {
            // ── Phase 2: AOE window (ticks 0‒19) ─────────────────────────────
            // Move sphere back to entity center and expand outward rapidly.
            if (CommonConfig.INSTANCE.isExperiementalSpheresEnabled() && this.sphereEntity != null) {
                this.sphereEntity.setPos(entity.getX(), entity.getY(), entity.getZ());
                float expandT = (float) this.continuousTick / (float) AOE_WINDOW_END;
                this.sphereEntity.setRadius(0.5F + (RANGE - 0.5F) * expandT);
            }

            // Use rangeComponent.getTargetsInArea() instead of WyHelper.getNearbyEntities()
            // so that the mod's built-in team/ally check is applied automatically.
            List<LivingEntity> nearby = this.rangeComponent.getTargetsInArea(entity, RANGE);

            for (LivingEntity target : nearby) {
                if (target == entity || this.caughtIds.contains(target.getId())) continue;

                this.caughtIds.add(target.getId());
                this.targetFloatY.put(target.getId(), target.getY() + FLOAT_HEIGHT);

                TimeBubbleEntity bubble = TimeBubbleEntity.create(entity.level, target, BUBBLE_LIFE);
                entity.level.addFreshEntity(bubble);

                entity.level.playSound(
                        (PlayerEntity) null, target.blockPosition(),
                        (SoundEvent) KaziSounds.CHRONOSTASIS_SFX.get(),
                        SoundCategory.PLAYERS, 2.0F, 1.0F
                );
            }

            // At the last AOE tick: collapse sphere and play close sound.
            if (this.continuousTick == AOE_WINDOW_END - 1) {
                removeSphere();
            }

        } else {
            // ── Phase 3: Stun + float (ticks 20‒119) ─────────────────────────
            for (int id : this.caughtIds) {
                Entity raw = entity.level.getEntity(id);
                if (!(raw instanceof LivingEntity)) continue;
                LivingEntity target = (LivingEntity) raw;
                if (!target.isAlive()) continue;

                // Stun effects.
                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.MOVEMENT_BLOCKED.get(), STUN_DURATION, 1, false, false
                ));
                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.NO_HANDS.get(), STUN_DURATION, 0, false, false
                ));
                // Resistance III — amplifier 2 = Resistance III in 1.16.5 (0-indexed).
                target.addEffect(new EffectInstance(
                        Effects.DAMAGE_RESISTANCE, STUN_DURATION, 2, false, false
                ));

                // Float.
                Double floatY = this.targetFloatY.get(id);
                if (floatY == null) continue;

                if (target.getY() < floatY) {
                    AbilityHelper.setDeltaMovement(target, 0.0, RISE_SPEED, 0.0);
                } else {
                    AbilityHelper.slowEntityFall(target);
                    AbilityHelper.setDeltaMovement(target, 0.0, 0.0, 0.0);
                }
            }
        }

        this.continuousTick++;
    }

    private void onContinuousEnd(LivingEntity entity, IAbility ability) {
        removeSphere();
        this.caughtIds.clear();
        this.targetFloatY.clear();
        this.continuousTick = 0;
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void removeSphere() {
        if (this.sphereEntity != null) {
            this.sphereEntity.remove();
            this.sphereEntity = null;
        }
    }

    /**
     * Returns [x, y, z] of the position in front of and at the height of the
     * entity's hands — chest height, offset 0.8 blocks forward along look direction.
     */
    private static double[] getHandPos(LivingEntity entity) {
        double handY    = entity.getY() + entity.getBbHeight() * 0.82;
        double forwardX = -Math.sin(Math.toRadians(entity.yRot)) * 0.8;
        double forwardZ =  Math.cos(Math.toRadians(entity.yRot)) * 0.8;
        return new double[]{
                entity.getX() + forwardX,
                handY,
                entity.getZ() + forwardZ
        };
    }

    // ── Time Bar helper ───────────────────────────────────────────────────────
    private static TimeBarAbility getTimeBar(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return null;
        xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data =
                xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability
                        .getLazy(entity).orElse(null);
        if (data == null) return null;
        for (IAbility abl : data.getEquippedAndPassiveAbilities()) {
            if (abl instanceof TimeBarAbility) return (TimeBarAbility) abl;
        }
        return null;
    }

    // ── Static initialiser ────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Chronostasis",
                AbilityCategory.DEVIL_FRUITS,
                ChronostasisAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TICKS),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{ SourceType.FIST })
                .build();
    }
}