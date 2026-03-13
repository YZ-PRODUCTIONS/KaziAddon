//
// Reworked: keeps the original aim-based grab + pull phase from Ryu No Kagizume,
// but replaces the old charge (point & crush) with a Dawn-Rocket-style air slam.
// Once the pull completes, both user and target launch upward (half Dawn Rocket height),
// hold briefly, then slam the target straight down for crushing damage.
//

package net.kazi.kazimod.abilities.RyusokenRework;

import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent.GrabState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class RyuNoKagizumeRework extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "cartaddon", "ryu_no_kagizume",
            new Pair[]{ImmutablePair.of(
                    "The user grabs an opponent, launches them both into the air, then slams the target straight down into the ground.",
                    (Object) null)});

    // ── Timings (preserved from original) ─────────────────────────────────────
    /** How long the pull phase lasts before transitioning to the air-slam (ticks). */
    private static final float PULL_TIME        = 200.0F;
    /** How long the air-hold phase lasts before the slam (ticks). Same as original charge time. */
    private static final float HOLD_TICKS       = 30.0F;

    // ── Air-slam tuning ────────────────────────────────────────────────────────
    /** Upward launch velocity — half of Dawn Rocket's 3.0F. */
    private static final float LAUNCH_UP        = 1.5F;
    /** Downward throw speed on slam. */
    private static final float THROW_DOWN       = 30.0F;
    /** Lateral spread in look direction on slam. */
    private static final float THROW_LATERAL    = 2.0F;
    /** Damage dealt on slam impact. */
    private static final float SLAM_DAMAGE      = 75.0F;

    // ── Cooldowns ──────────────────────────────────────────────────────────────
    private static final float COOLDOWN_SUCCESS = 400.0F;
    private static final float COOLDOWN_FAIL    = 200.0F;
    // ──────────────────────────────────────────────────────────────────────────

    public static final AbilityCore<RyuNoKagizumeRework> INSTANCE;

    // ── Components (grab stack identical to original) ─────────────────────────
    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addEndEvent(this::onContinuityEnd);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    /** Air-slam phase — replaces the old "point & crush" charge. */
    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final AnimationComponent animationComponent = new AnimationComponent(this);

    /** Aim-based grab — identical settings to the original ability. */
    private final GrabEntityComponent grabComponent =
            (new GrabEntityComponent(this, true, true, true, 2.0F))
                    .addPullStartEvent(this::onPullStart)
                    .addPullEndEvent(this::onPullEnd);

    private final HitTriggerComponent hitTriggerComponent =
            (new HitTriggerComponent(this)).addOnHitEvent(this::onHitEvent);

    private final PoolComponent poolComponent;

    public RyuNoKagizumeRework(AbilityCore<RyuNoKagizumeRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY,
                new AbilityPool2[]{CartAbilityPools.RYUSOKEN});
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{
                this.dealDamageComponent,
                this.chargeComponent,
                this.animationComponent,
                this.continuousComponent,
                this.grabComponent,
                this.hitTriggerComponent,
                this.poolComponent
        });
        super.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
        super.addUseEvent(this::onUseEvent);
    }

    // ── Use event (identical to original) ────────────────────────────────────
    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            if (this.continuousComponent.isContinuous()) {
                this.grabComponent.release(entity);
                this.continuousComponent.stopContinuity(entity);
            } else if (this.grabComponent.getState() == GrabState.IDLE
                    && this.grabComponent.grabNearest(entity, 5.5F, 2.5F, false)) {
                this.grabComponent.triggerPulling(entity);
            } else {
                super.cooldownComponent.startCooldown(entity, 10.0F);
            }
        }
    }

    // ── Hit event (identical to original) ────────────────────────────────────
    private boolean onHitEvent(LivingEntity entity, LivingEntity target,
                               ModDamageSource source, IAbility ability) {
        if (this.continuousComponent.isContinuous() && !this.grabComponent.hasGrabbedEntity()) {
            if (this.grabComponent.grabManually(entity, target)) {
                this.grabComponent.startPulling(entity);
            }
            target.addEffect(new EffectInstance((Effect) ModEffects.ANTI_KNOCKBACK.get(), 1));
            return false;
        }
        return true;
    }

    // ── Pull start: set pull duration (identical to original) ────────────────
    public void onPullStart(LivingEntity entity, IAbility ability) {
        this.continuousComponent.setThresholdTime(entity, PULL_TIME);
    }

    // ── Pull end: transition into the air-slam charge ────────────────────────
    public void onPullEnd(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
        if (this.grabComponent.canContinueGrab(entity)) {
            this.chargeComponent.startCharging(entity, HOLD_TICKS);
        }
    }

    // ── Continuity end (identical to original) ────────────────────────────────
    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.grabComponent.getState() != GrabState.GRABBED) {
                this.grabComponent.release(entity);
            }
            if (!this.grabComponent.canContinueGrab(entity)) {
                super.cooldownComponent.startCooldown(entity, COOLDOWN_FAIL);
            }
        }
    }

    // ── Charge start: launch both user and target upward ─────────────────────
    public void onChargeStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.POINT_RIGHT_ARM);

        if (!entity.level.isClientSide && this.grabComponent.hasGrabbedEntity()) {
            LivingEntity target = this.grabComponent.getGrabbedEntity();

            // Launch shooter upward
            AbilityHelper.setDeltaMovement(entity,
                    entity.getDeltaMovement().x,
                    LAUNCH_UP,
                    entity.getDeltaMovement().z);

            // Launch grabbed target alongside the shooter
            AbilityHelper.setDeltaMovement(target,
                    entity.getDeltaMovement().x,
                    LAUNCH_UP,
                    entity.getDeltaMovement().z);

            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    SoundEvents.PLAYER_BIG_FALL, SoundCategory.PLAYERS, 3.0F, 1.5F);
        }
    }

    // ── Charge tick: keep both airborne, pin target to user ──────────────────
    public void onChargeTick(LivingEntity entity, IAbility ability) {
        // Keep shooter floating
        AbilityHelper.slowEntityFall(entity);
        // Prevent shooter from acting during the hold

        if (!entity.level.isClientSide) {
            if (!super.canUse(entity).isFail() && this.grabComponent.canContinueGrab(entity)) {
                LivingEntity target = this.grabComponent.getGrabbedEntity();

                // Pin target to user position each tick (same as Dawn Rocket's grab tick)
                target.teleportTo(entity.getX(), entity.getY(), entity.getZ());
                AbilityHelper.setDeltaMovement(target, 0.0, 0.0, 0.0);

                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.DIZZY.get(), 10, 0, false, false));
                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 0, false, false));
                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.ANTI_KNOCKBACK.get(), 10, 0, false, false));
            }
        }
    }

    // ── Charge end: slam target straight down, deal damage ───────────────────
    public void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.grabComponent.hasGrabbedEntity()) {
            LivingEntity target = this.grabComponent.getGrabbedEntity();

            // Slam straight down with a small look-direction lateral component
            Vector3d look = entity.getLookAngle()
                    .multiply(THROW_LATERAL, 0.0, THROW_LATERAL)
                    .add(0.0, -THROW_DOWN, 0.0);
            AbilityHelper.setDeltaMovement(target, look);

            if (this.dealDamageComponent.hurtTarget(entity, target, SLAM_DAMAGE)) {
                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.FRAGILE.get(), 60, 0, false, false));
                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.DIZZY.get(), 80, 1, false, false));
            }

            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    SoundEvents.PLAYER_BIG_FALL, SoundCategory.PLAYERS, 3.0F, 0.8F);
        }

        this.grabComponent.release(entity);
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, COOLDOWN_SUCCESS);
    }

    // ── Unlock check (identical to original) ─────────────────────────────────
    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;
        IEntityStats props = EntityStatsCapability.get(player);
        IQuestData questProps = QuestDataCapability.get(player);
        return props.getFightingStyle().equals(CartValues.RYUSOKEN)
                && questProps.hasFinishedQuest(CartQuests.RYUSOKEN_TRIAL_02);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Ryu No Kagizume",
                AbilityCategory.STYLE,
                RyuNoKagizumeRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(SLAM_DAMAGE, 0.0F),
                        ChargeComponent.getTooltip(HOLD_TICKS),
                        CooldownComponent.getTooltip(COOLDOWN_SUCCESS)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setUnlockCheck(RyuNoKagizumeRework::canUnlock)
                .build();
    }
}