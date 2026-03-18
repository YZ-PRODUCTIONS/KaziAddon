package net.kazi.kazimod.abilities.Toki;

import java.util.ArrayList;
import java.util.Arrays;
import net.kazi.kazimod.network.AfterImagePacket;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class TimeAccelerationAbility extends Ability {

    // ── Description ──────────────────────────────────────────────────────────
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "time_acceleration",
            new Pair[]{
                    ImmutablePair.of(
                            "Accelerates the user through time, greatly boosting movement speed, " +
                                    "leaving after-images in their wake, and allowing them to phase " +
                                    "through incoming attacks.",
                            (Object) null
                    )
            }
    );

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float HOLD_TIME       = 100.0F; // 10 s
    private static final float MIN_COOLDOWN    = 100.0F; // 5 s
    private static final float MAX_COOLDOWN    = 400.0F; // 35 s
    private static final float MAX_SPEED       = 1.0F;
    private static final float PROTECTION_TIME = 10.0F;
    /** Spawn one after-image every N ticks while moving. */
    private static final int   AFTERIMAGE_INTERVAL = 3;
    private static final float TIME_COST           = 60.0F;

    // ── Static instance ───────────────────────────────────────────────────────
    public static final AbilityCore<TimeAccelerationAbility> INSTANCE;

    // ── Components ────────────────────────────────────────────────────────────
    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStartContinuity)
                    .addTickEvent(this::onContinuityTick)
                    .addEndEvent(this::onEndContinuity);

    private final DamageTakenComponent damageTakenComponent =
            (new DamageTakenComponent(this))
                    .addOnAttackEvent(this::onDamageTaken);

    // ── Runtime state ─────────────────────────────────────────────────────────
    private int   hitsTaken;
    private float protTimer;
    private boolean prevSprintValue;
    private int afterImageTick;

    // ── Constructor ───────────────────────────────────────────────────────────
    public TimeAccelerationAbility(AbilityCore<TimeAccelerationAbility> core) {
        super(core);
        this.hitsTaken       = 0;
        this.protTimer       = 0.0F;
        this.prevSprintValue = false;
        this.afterImageTick  = 0;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.damageTakenComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    // ── Time Bar helper ───────────────────────────────────────────────────────
    private static TimeBarAbility getTimeBar(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return null;
        IAbilityData data = AbilityDataCapability.getLazy(entity).orElse(null);
        if (data == null) return null;
        for (IAbility abl : data.getEquippedAndPassiveAbilities()) {
            if (abl instanceof TimeBarAbility) return (TimeBarAbility) abl;
        }
        return null;
    }

    // ── Use event ─────────────────────────────────────────────────────────────
    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.triggerContinuity(entity, HOLD_TIME);
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

        this.continuousComponent.triggerContinuity(entity, HOLD_TIME);
    }

    // ── Continuity start ──────────────────────────────────────────────────────
    private void onStartContinuity(LivingEntity entity, IAbility ability) {
        this.hitsTaken       = 0;
        this.protTimer       = 0.0F;
        this.afterImageTick  = 0;
        this.prevSprintValue = entity.isSprinting();
    }

    // ── Continuity tick ───────────────────────────────────────────────────────
    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.protTimer > 0.0F) {
            --this.protTimer;
        }

        // ── Speed boost ───────────────────────────────────────────────────────
        if (AbilityHelper.canUseMomentumAbilities(entity)) {
            if (entity.isSprinting()) {
                Vector3d vec = entity.getLookAngle();
                if (entity.isOnGround()) {
                    AbilityHelper.setDeltaMovement(entity,
                            vec.x * (double) MAX_SPEED,
                            entity.getDeltaMovement().y,
                            vec.z * (double) MAX_SPEED
                    );
                } else {
                    AbilityHelper.setDeltaMovement(entity,
                            vec.x * (double) MAX_SPEED * 0.5,
                            entity.getDeltaMovement().y,
                            vec.z * (double) MAX_SPEED * 0.5
                    );
                }

                this.prevSprintValue = true;
            } else {
                this.prevSprintValue = false;
            }
        }

        // ── After-images ──────────────────────────────────────────────────────
        if (!entity.level.isClientSide && entity instanceof PlayerEntity) {
            if (++this.afterImageTick >= AFTERIMAGE_INTERVAL) {
                this.afterImageTick = 0;

                Vector3d motion = entity.getDeltaMovement();
                double speedSq  = motion.x * motion.x + motion.z * motion.z;
                if (speedSq > 0.01) {
                    PlayerEntity player = (PlayerEntity) entity;
                    AfterImagePacket packet = new AfterImagePacket(
                            entity.getX(), entity.getY(), entity.getZ(),
                            entity.yRot, entity.xRot,
                            player.getGameProfile().getName(),
                            AfterImagePacket.DEFAULT_LIFE
                    );
                    KaziPacketHandler.sendAfterImage(entity, packet);
                }
            }
        }
    }

    // ── Continuity end ────────────────────────────────────────────────────────
    private void onEndContinuity(LivingEntity entity, IAbility ability) {
        float cooldown = MIN_COOLDOWN
                + this.continuousComponent.getContinueTime() * 2.5F
                + 5.0F * (float) Math.pow((double) this.hitsTaken, (double) this.hitsTaken);
        cooldown = Math.min(MAX_COOLDOWN, cooldown);
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    // ── Damage-taken handler ──────────────────────────────────────────────────
    public float onDamageTaken(LivingEntity entity, IAbility ability,
                               DamageSource damageSource, float damage) {

        if (!super.isContinuous() || !AbilityHelper.canUseMomentumAbilities(entity)
                || AbilityHelper.isGrabbing(entity)) {
            return damage;
        }

        boolean isUnavoidable = damageSource instanceof ModDamageSource
                && ((ModDamageSource) damageSource).isUnavoidable();

        ArrayList<String> acceptableSources = new ArrayList<>(Arrays.asList(
                "mob", "player", "ability_projectile", "ability"
        ));

        boolean isDodgeable =
                (damageSource.getDirectEntity() instanceof LivingEntity
                        || damageSource.getDirectEntity() instanceof ProjectileEntity)
                        && acceptableSources.contains(damageSource.getMsgId())
                        && !isUnavoidable;

        if (this.protTimer <= 0.0F) {
            if (isDodgeable) {
                entity.level.playSound(
                        (PlayerEntity) null,
                        entity.blockPosition(),
                        (SoundEvent) ModSounds.TELEPORT_SFX.get(),
                        SoundCategory.PLAYERS,
                        1.0F,
                        0.75F + entity.getRandom().nextFloat() / 2.0F
                );

                ++this.hitsTaken;
                this.protTimer = PROTECTION_TIME;
                return 0.0F;
            }
            return damage;
        }

        return 0.0F;
    }

    // ── Static initialiser ────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Time Acceleration",
                AbilityCategory.DEVIL_FRUITS,
                TimeAccelerationAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(MIN_COOLDOWN, MAX_COOLDOWN),
                        ContinuousComponent.getTooltip(HOLD_TIME)
                })
                .build();
    }
}