package net.kazi.kazimod.abilities.BomuRework;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent.GrabState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ExplosiveHoldAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "explosive_hold",
            new Pair[]{ImmutablePair.of(
                    "Grabs a nearby enemy and blasts them repeatedly " +
                            "with explosive force, then hurls them away on the final hit.",
                    (Object) null)}
    );

    // ── Tuning ────────────────────────────────────────────────────────────────
    private static final float  COOLDOWN          = 240.0F;
    private static final float  HOLD_DURATION     = 60.0F;   // 3 blasts × 20 ticks
    private static final float  HIT_DAMAGE        = 25.0F;
    private static final double THROW_POWER_XZ    = 2.0;
    private static final double THROW_POWER_Y     = 0.6;
    private static final int    BLAST_EVERY_TICKS = 20;
    private static final int    MAX_BLASTS        = 3;
    private static final double HOLD_DISTANCE     = 1.8;

    public static final AbilityCore<ExplosiveHoldAbility> INSTANCE;

    // ── Components ────────────────────────────────────────────────────────────
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final AnimationComponent  animationComponent  = new AnimationComponent(this);

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::onHoldStart)
            .addTickEvent(this::onHoldTick)
            .addEndEvent(this::onHoldEnd);

    // continuousComponent is kept solely to satisfy the pull-phase threshold
    // pattern used by GrabEntityComponent — it is NOT used for hit-trigger grabs.
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addEndEvent(this::onContinuityEnd);

    private final GrabEntityComponent grabComponent = new GrabEntityComponent(this, true, false, true, 2.0F)
            .addPullStartEvent(this::onPullStart)
            .addPullEndEvent(this::onPullEnd);

    private final HitTriggerComponent hitTriggerComponent = new HitTriggerComponent(this)
            .addOnHitEvent(this::onHitEvent);

    private final PoolComponent poolComponent;

    private int holdTick   = 0;
    private int blastCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    public ExplosiveHoldAbility(AbilityCore<ExplosiveHoldAbility> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{
                this.dealDamageComponent,
                this.animationComponent,
                this.chargeComponent,
                this.continuousComponent,
                this.grabComponent,
                this.hitTriggerComponent,
                this.poolComponent
        });
        super.addUseEvent(this::onUseEvent);
    }

    // ── Use event ─────────────────────────────────────────────────────────────
    // Mirrors Kagizume: if not charging, try grabNearest → pull. If already
    // continuous (pulling), release and cancel. No hit-trigger grab path here.
    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            // Re-pressing during the hold sequence cancels it
            this.chargeComponent.stopCharging(entity);
            return;
        }

        if (this.continuousComponent.isContinuous()) {
            // Already in pull phase — release and cancel
            this.grabComponent.release(entity);
            this.continuousComponent.stopContinuity(entity);
        } else if (this.grabComponent.getState() == GrabState.IDLE
                && this.grabComponent.grabNearest(entity, 5.5F, 2.5F, false)) {
            // Found someone nearby — begin pulling them in
            this.grabComponent.triggerPulling(entity);
        } else {
            // No target in range; short penalty cooldown like Kagizume
            super.cooldownComponent.startCooldown(entity, 10.0F);
        }
    }

    // ── Hit trigger ───────────────────────────────────────────────────────────
    // Only fires during the pull phase (continuousComponent is active).
    // If the player manages to land a hit on a target while pulling, grab them.
    private boolean onHitEvent(LivingEntity entity, LivingEntity target,
                               xyz.pixelatedw.mineminenomi.init.ModDamageSource source, IAbility ability) {
        if (this.continuousComponent.isContinuous() && !this.grabComponent.hasGrabbedEntity()) {
            if (this.grabComponent.grabManually(entity, target)) {
                this.grabComponent.startPulling(entity);
            }
            target.addEffect(new EffectInstance((Effect) ModEffects.ANTI_KNOCKBACK.get(), 1));
            return false;
        }
        return true;
    }

    // ── Pull phase ────────────────────────────────────────────────────────────
    public void onPullStart(LivingEntity entity, IAbility ability) {
        // Extend continuity window so the pull animation has time to complete
        this.continuousComponent.setThresholdTime(entity, 200.0F);
    }

    public void onPullEnd(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
        if (this.grabComponent.canContinueGrab(entity)) {
            this.holdTick   = 0;
            this.blastCount = 0;
            this.chargeComponent.startCharging(entity, HOLD_DURATION);
        }
    }

    // ── Continuity end ────────────────────────────────────────────────────────
    // Matches Kagizume: only start cooldown if the grab actually failed.
    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.grabComponent.getState() != GrabState.GRABBED) {
                this.grabComponent.release(entity);
            }
            if (!this.grabComponent.canContinueGrab(entity)) {
                super.cooldownComponent.startCooldown(entity, COOLDOWN);
            }
        }
    }

    // ── Hold (charge) phase ───────────────────────────────────────────────────
    private void onHoldStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.POINT_ARMS);
    }

    private void onHoldTick(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 40, 1, false, false));
        entity.addEffect(new EffectInstance((Effect) ModEffects.NO_HANDS.get(), 5, 0));

        if (!entity.level.isClientSide) {
            if (super.canUse(entity).isFail() || !this.grabComponent.canContinueGrab(entity)) {
                this.chargeComponent.stopCharging(entity);
                return;
            }

            LivingEntity target = this.grabComponent.getGrabbedEntity();

            target.addEffect(new EffectInstance((Effect) ModEffects.GRABBED.get(), 2, 3));

            // Float target directly in front of the user at chest height
            Vector3d lookDir  = entity.getLookAngle();
            Vector3d flatLook = new Vector3d(lookDir.x, 0.0, lookDir.z).normalize();
            Vector3d holdPos  = entity.position()
                    .add(flatLook.scale(HOLD_DISTANCE))
                    .add(0.0, 0.5, 0.0);

            Vector3d toHoldPos = holdPos.subtract(target.position());
            target.setDeltaMovement(toHoldPos.x * 0.5, toHoldPos.y * 0.5, toHoldPos.z * 0.5);
            target.fallDistance = 0.0F;
            target.setNoGravity(true);

            holdTick++;
            if (holdTick % BLAST_EVERY_TICKS == 0) {
                blastCount++;
                boolean isFinalBlast = (blastCount >= MAX_BLASTS);
                deliverBlast(entity, target, isFinalBlast);
                if (isFinalBlast) {
                    this.chargeComponent.stopCharging(entity);
                }
            }
        }
    }

    // ── Hold end — mirrors Kagizume's onChargeEnd throw pattern ──────────────
    private void onHoldEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.grabComponent.hasGrabbedEntity()) {
                LivingEntity target = this.grabComponent.getGrabbedEntity();
                target.setNoGravity(false);

                // Use grabComponent.throwTarget like Kagizume, then release
                this.grabComponent.throwTarget(entity, THROW_POWER_XZ, THROW_POWER_Y);
                this.grabComponent.release(entity);
            } else {
                this.grabComponent.release(entity);
            }

            this.animationComponent.stop(entity);
            super.cooldownComponent.startCooldown(entity, COOLDOWN);
        }
    }

    // ── Blast helper ──────────────────────────────────────────────────────────
    private void deliverBlast(LivingEntity entity, LivingEntity target, boolean isFinalBlast) {
        AbilityDamageSource source = (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
        source.setInternal();
        source.setSlash();
        source.markIndirectDamage();

        if (this.dealDamageComponent.hurtTarget(entity, target, HIT_DAMAGE, source)) {

            ((ServerWorld) entity.level).playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundCategory.PLAYERS,
                    4.0F, 1.0F
            );

            WyHelper.spawnParticleEffect(
                    (ParticleEffect) KaziParticleEffects.BAKUGO.get(),
                    entity,
                    target.getX(),
                    target.getEyeY(),
                    target.getZ()
            );
        }

        if (isFinalBlast) {
            this.animationComponent.stop(entity);
        }
    }

    // ── Static registration ───────────────────────────────────────────────────
    static {
        INSTANCE = new AbilityCore.Builder<>("Explosive Hold", AbilityCategory.DEVIL_FRUITS, ExplosiveHoldAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(HIT_DAMAGE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INTERNAL})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .build();
    }
}