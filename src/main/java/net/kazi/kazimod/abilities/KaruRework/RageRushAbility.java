package net.kazi.kazimod.abilities.KaruRework;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import net.kazi.kazimod.abilities.KaruRework.IngaZarashiRework;
import xyz.pixelatedw.mineminenomi.abilities.karu.KarmaAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.packets.server.entities.SPinCameraPacket;
import xyz.pixelatedw.mineminenomi.packets.server.entities.SUnpinCameraPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public class RageRushAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "rage_rush", new Pair[]{ImmutablePair.of("The user charges forward in a rage, striking all enemies in their path and launching them high into the air. Scales with karma. Requires Inga Zarashi to be active.", (Object)null)});
    private static final float CHARGE_TIME = 5.0F; // 0.25 seconds (5 ticks)
    private static final float CONTINUITY_TIME = 25.0F;
    private static final float MIN_COOLDOWN = 400.0F; // 8 seconds at 0 karma (low karma = low cooldown)
    private static final float MAX_COOLDOWN = 600.0F; // 30 seconds at 100 karma (high karma = high cooldown)

    // Scaling values based on karma (0-100)
    private static final float MIN_RANGE = 2.0F;
    private static final float MAX_RANGE = 10.0F;
    private static final float MIN_DAMAGE = 40.0F;
    private static final float MAX_DAMAGE = 110.0F;
    private static final float MIN_MOVEMENT_SPEED = 2.5F; // 25% of original 3.5
    private static final float MAX_MOVEMENT_SPEED = 14.0F; // 4x the original 3.5
    private static final float KARMA_DRAIN = 10.0F;

    private static final float MAX_YAW_CHANGE = 90.0F;
    private static final float MAX_PITCH_CHANGE = 12.0F;
    public static final AbilityCore<RageRushAbility> INSTANCE;

    private Optional<KarmaAbility> karmaAbility = Optional.empty();
    private Vector3d lockedDirection = null; // Store the locked direction
    private double usedKarma = 0.0; // Changed to double for better precision
    private float lockedSpeed = 0.0F; // Store the calculated speed based on karma
    private float lockedRange = 0.0F; // Store the calculated range based on karma
    private float lockedDamage = 0.0F; // Store the calculated damage based on karma
    private float lockedKnockbackMultiplier = 0.0F; // Store the calculated knockback multiplier

    // ADD THE MISSING COOLDOWN COMPONENT
    private final CooldownComponent cooldownComponent = new CooldownComponent(this);

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true))
            .addStartEvent(this::startContinuityEvent)
            .addTickEvent(this::duringContinuityEvent)
            .addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this))
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::tickChargeEvent)
            .addEndEvent(this::endChargeEvent);

    public RageRushAbility(AbilityCore<RageRushAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.cooldownComponent, // ADD TO COMPONENTS
                this.animationComponent,
                this.changeStatsComponent,
                this.chargeComponent,
                this.continuousComponent,
                this.rangeComponent,
                this.dealDamageComponent,
                this.hitTrackerComponent
        });

        this.changeStatsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(this::canUse);
        this.addUseEvent(this::useEvent);
    }

    private AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        KarmaAbility karma = (KarmaAbility)props.getPassiveAbility(KarmaAbility.INSTANCE);
        if (karma == null) {
            return AbilityUseResult.fail((ITextComponent)null);
        }

        // Check if player is using Inga Zarashi transformation
        IngaZarashiRework ingaZarashi = (IngaZarashiRework)props.getEquippedAbility(IngaZarashiRework.INSTANCE);
        if (ingaZarashi == null || !ingaZarashi.isContinuous()) {
            return AbilityUseResult.fail(new TranslationTextComponent("Rage Rush can only be used while Inga Zarashi is active!"));
        }

        this.karmaAbility = Optional.ofNullable(karma);
        return AbilityUseResult.success();
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        // CAPTURE KARMA VALUE IMMEDIATELY WHEN ABILITY IS USED
        if (this.karmaAbility.isPresent()) {
            this.usedKarma = Math.max(0.0, Math.min(100.0, this.karmaAbility.get().getKarma())); // Clamp between 0-100
        } else {
            this.usedKarma = 0.0;
        }

        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }

        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        // Use Tekkai animation (CROSSED_ARMS) instead of MOUNTAIN_EATER
        this.animationComponent.start(entity, ModAnimations.CROSSED_ARMS);
        this.hitTrackerComponent.clearHits();

        // Add camera lock like Shi Shishi Sonson
        if (entity instanceof ServerPlayerEntity) {
            WyNetwork.sendTo(SPinCameraPacket.pinClampedYawAndPitch(entity.yRot, MAX_YAW_CHANGE, entity.xRot, MAX_PITCH_CHANGE), (ServerPlayerEntity)entity);
        }

        // Lock the direction at the start of charging
        this.lockedDirection = entity.getLookAngle().normalize();
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, CONTINUITY_TIME);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.applyModifiers(entity);
        this.hitTrackerComponent.clearHits();

        // Make sure direction is locked (in case it wasn't set during charge)
        if (this.lockedDirection == null) {
            this.lockedDirection = entity.getLookAngle().normalize();
        }

        // USE THE ALREADY CAPTURED KARMA VALUE
        // Calculate karma ratio (0.0 to 1.0) with better precision
        double karmaRatio = this.usedKarma / 100.0;

        // Lock all scaled values based on karma at ability start
        this.lockedSpeed = (float)(MIN_MOVEMENT_SPEED + (MAX_MOVEMENT_SPEED - MIN_MOVEMENT_SPEED) * karmaRatio);
        this.lockedRange = (float)(MIN_RANGE + (MAX_RANGE - MIN_RANGE) * karmaRatio);
        this.lockedDamage = (float)(MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * karmaRatio);
        this.lockedKnockbackMultiplier = (float)(0.25 + (0.75 * karmaRatio));

        // Debug logging
        System.out.println("=== RAGE RUSH DEBUG ===");
        System.out.println("Used Karma: " + this.usedKarma);
        System.out.println("Karma Ratio: " + karmaRatio);
        System.out.println("Locked Speed: " + this.lockedSpeed + " (min: " + MIN_MOVEMENT_SPEED + ", max: " + MAX_MOVEMENT_SPEED + ")");
        System.out.println("Locked Range: " + this.lockedRange);
        System.out.println("Locked Damage: " + this.lockedDamage);
        System.out.println("Locked Knockback Multiplier: " + this.lockedKnockbackMultiplier);

        // Drain karma when ability starts
        if (this.karmaAbility.isPresent()) {
            this.karmaAbility.get().addKarma(entity, -KARMA_DRAIN);
        }
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            if (entity instanceof PlayerEntity && this.lockedDirection != null) {
                // Use the locked direction with locked speed
                // Scale the entire normalized direction vector by the locked speed
                Vector3d speed = this.lockedDirection.scale((double)this.lockedSpeed);
                entity.move(MoverType.SELF, speed);
            }

            List<LivingEntity> list = this.rangeComponent.getTargetsInArea(entity, this.lockedRange);

            for(LivingEntity target : list) {
                if (this.hitTrackerComponent.canHit(target)) {
                    this.dealDamageComponent.hurtTarget(entity, target, this.lockedDamage);

                    // Apply scaled knockback based on karma with strong upward launch
                    // Base horizontal knockback from Skull Basher (1.40F), but much stronger vertical (2.5F base)
                    Vector3d knockbackVec = this.lockedDirection.multiply(
                            (double)(1.40F * this.lockedKnockbackMultiplier),
                            (double)0.0F,  // Don't use the locked direction's Y for knockback
                            (double)(1.40F * this.lockedKnockbackMultiplier)
                    );

                    // Add strong upward knockback that scales with karma
                    double verticalKnockback = 2.5F * this.lockedKnockbackMultiplier;

                    AbilityHelper.setDeltaMovement(target, knockbackVec.x, verticalKnockback, knockbackVec.z);
                }
            }

            this.hitTrackerComponent.clearHits();
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.hitTrackerComponent.clearHits();
        this.animationComponent.stop(entity);

        // Clear the locked direction
        this.lockedDirection = null;

        // Unlock camera like Shi Shishi Sonson
        if (entity instanceof ServerPlayerEntity) {
            WyNetwork.sendTo(new SUnpinCameraPacket(), (ServerPlayerEntity)entity);
        }

        // Calculate cooldown based on karma used (higher karma = higher cooldown)
        // At 0 karma: 8 seconds (160 ticks), at 100 karma: 30 seconds (600 ticks)
        // Use the stored karma value for consistency
        double karmaRatio = this.usedKarma / 100.0;
        float cooldown = (float)(MIN_COOLDOWN + (MAX_COOLDOWN - MIN_COOLDOWN) * karmaRatio);

        // Debug logging
        System.out.println("=== RAGE RUSH COOLDOWN DEBUG ===");
        System.out.println("Used Karma: " + this.usedKarma);
        System.out.println("Karma Ratio: " + karmaRatio);
        System.out.println("Cooldown: " + cooldown + " ticks (" + (cooldown/20.0F) + " seconds)");
        System.out.println("Min Cooldown: " + MIN_COOLDOWN + ", Max Cooldown: " + MAX_COOLDOWN);

        // Apply the calculated cooldown
        this.cooldownComponent.startCooldown(entity, cooldown);

        // Reset stored karma for next use
        this.usedKarma = 0.0;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Rage Rush", AbilityCategory.DEVIL_FRUITS, RageRushAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(CONTINUITY_TIME),
                        CooldownComponent.getTooltip(MIN_COOLDOWN, MAX_COOLDOWN),
                        RangeComponent.getTooltip(MIN_RANGE, MAX_RANGE, RangeType.AOE),
                        DealDamageComponent.getTooltip(MIN_DAMAGE, MAX_DAMAGE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .build();

        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("a7c8f674-ba38-463d-a84e-21ae454cc1df"),
                INSTANCE,
                "Rage Rush Step Height Modifier",
                (double)1.5F,
                Operation.ADDITION
        );
    }
}
