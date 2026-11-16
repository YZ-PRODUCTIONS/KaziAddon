package net.kazi.kazimod.abilities.GoruRework;

import net.kazi.kazimod.entities.EaVfxEntity;
import net.kazi.kazimod.entities.EaRuptureShape;
import net.kazi.kazimod.entities.WindsRaptureShape;
import net.kazi.kazimod.events.handlers.EaFallProtection;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

/** Ea commits to its charge once activated; the released attack runs independently. */
public final class EaAbility extends Ability {
    public enum Mode {
        EA("Ea: Enuma Elish", 100, 120), PILLAR("Uta Darunaki", 50, 25), WINDS("Ea: Winds of Rapture", 50, 35);
        final String title;
        final float damage, cooldownTicks;
        Mode(String title, float damage, int cooldownSeconds) {
            this.title = title; this.damage = damage; this.cooldownTicks = cooldownSeconds * 20;
        }
        @Override public String toString() { return title; }
    }
    private final AltModeComponent<Mode> modes = new EaModes(this)
            .addChangeModeEvent((user, ability, mode) -> updateDisplay(mode));
    public static final float BASE_DAMAGE = 100.0F;
    public static final float COOLDOWN_TICKS = 120 * 20;
    public static final float CANCEL_COOLDOWN_TICKS = 30 * 20;
    public static final AbilityCore<EaAbility> INSTANCE = new AbilityCore.Builder<>(
            "Ea: Divine Sword of Rapture", AbilityCategory.DEVIL_FRUITS, EaAbility::new)
            .setIcon(new ResourceLocation("kazimod", "textures/abilities/ea_divine_sword_of_rapture.png"))
            .addDescriptionLine(new StringTextComponent("Seriousness automatically selects Uta Darunaki at Normal and Stage 1, Winds of Rapture at Stage 2, and Enuma Elish at Stage 3."),
                    new StringTextComponent("The form is locked when a cast starts. Health changes select the form for your next cast."),
                    new StringTextComponent("Ea: Enuma Elish: Spin Ea's three segments, then tear the space ahead."),
                    new StringTextComponent("Using Enuma Elish protects your next landing from fall damage for up to 30 seconds."),
                    new StringTextComponent("Float, stuck, and Kami-e dodging last until the audio begins fading. The released rupture locks its direction and strikes each enemy once."),
                    new StringTextComponent((int) EaVfxEntity.MAX_RANGE + " block reach. Once activated, the charge cannot be canceled."),
                    new StringTextComponent("Uta Darunaki: Lock your crosshair point, summon a colossal pillar from a golden sky portal, then slam it down after "
                            + (net.kazi.kazimod.models.abilities.UtaPillarMesh.IMPACT_TICK / 20.0F) + " seconds."),
                    new StringTextComponent("Pillar impact: 50 damage in a 10.2 x 32.5 x 10.2 block hitbox, with outward knockback. 25 second cooldown."),
                    new StringTextComponent("Ea: Winds of Rapture: Charge whirling winds for 3 seconds, then fire a narrow, hollow spiral beam up to 40 blocks. 50 damage once per enemy; 35 second cooldown."))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    (user, ability) -> {
                        EaAbility ea = (EaAbility)ability;
                        if (ea.modes.getCurrentMode() == Mode.PILLAR) return new StringTextComponent("Pillar summon: "
                                + (net.kazi.kazimod.models.abilities.UtaPillarMesh.IMPACT_TICK / 20.0F) + " seconds");
                        return ChargeComponent.getTooltip(ea.isWindsMode() ? WindsRaptureShape.CHARGE_TICKS : EaVfxEntity.CHARGE_TICKS)
                                .expand(user, ability);
                    },
                    (user, ability) -> DealDamageComponent.getTooltip(((EaAbility)ability).modes.getCurrentMode().damage).expand(user, ability),
                    (user, ability) -> CooldownComponent.getTooltip(((EaAbility)ability).modes.getCurrentMode().cooldownTicks).expand(user, ability))
            .setSourceHakiNature(SourceHakiNature.SPECIAL)
            .setSourceType(SourceType.INTERNAL).setSourceElement(SourceElement.SHOCKWAVE)
            .setUnlockCheck(SeriousnessAbility::isEligible)
            .build();

    private final ChargeComponent charge = new CommittedChargeComponent(this,
            () -> this.pendingCast, this::chargeTick)
            .addStartEvent(this::beginCharge).addEndEvent(this::finishCharge);
    private final AnimationComponent animation = new AnimationComponent(this);
    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final DamageTakenComponent dodge = new DamageTakenComponent(this)
            .addOnAttackEvent(this::dodgeDuringCharge);
    private EaVfxEntity visual;
    private Mode activeMode = Mode.PILLAR;
    private boolean pendingCast;
    private Vector3d launchOrigin;
    private Vector3d lockedPosition;
    private static final String GRAVITY_LOCK_TAG = "kazimodEaGravityLock";
    private int launchTicks;
    private long dodgeProtectionUntil;
    private boolean ownsChargeStuck;
    private boolean ownsFloat;
    private long stanceUntil;
    private static final int DODGE_PROTECTION_TICKS = 10;
    private static final int ASCENT_TICKS = 40;
    private static final double LAUNCH_HEIGHT = 50.0D;

    public EaAbility(AbilityCore<EaAbility> core) {
        super(core);
        this.isNew = true;
        addComponents(charge, animation, damage, modes, dodge);
        updateDisplay(Mode.PILLAR);
        addEquipEvent((user, ability) -> {
            if (!user.level.isClientSide && stanceUntil == 0) clearStance(user);
            refreshSeriousnessMode(user);
        });
        addUseEvent((user, ability) -> {
            if (!SeriousnessAbility.isEligible(user)) return;
            refreshSeriousnessMode(user);
            if (pendingCast || charge.isCharging()) return;
            if (modes.getCurrentMode() == Mode.PILLAR) {
                if (!user.level.isClientSide) {
                    EaVfxEntity pillar = new EaVfxEntity(KaziEntities.EA_VFX.get(), user.level);
                    pillar.beginPillar(user, this);
                    user.level.addFreshEntity(pillar);
                    cooldownComponent.startCooldown(user, Mode.PILLAR.cooldownTicks);
                }
                return;
            }
            activeMode = modes.getCurrentMode();
            charge.startCharging(user, activeMode == Mode.WINDS ? WindsRaptureShape.CHARGE_TICKS : EaVfxEntity.CHARGE_TICKS);
        });
        addTickEvent((user, ability) -> {
            if (user.level.isClientSide) return;
            refreshSeriousnessMode(user);
            // Saved charge state cannot restore a transient world effect after reconnecting.
            if (charge.isCharging() && !pendingCast) {
                pendingCast = true;
                cancelCharge(user);
                return;
            }
            if (!pendingCast) {
                if (stanceUntil != 0) {
                    if (hasActiveStance(user)) holdPosition(user);
                    else clearStance(user);
                }
                return;
            }
            if (!user.isAlive()) { cancelCharge(user); return; }
            restoreChargingVisual(user);
        });
        addRemoveEvent((user, ability) -> cancelCharge(user));
    }

    public static Mode modeForStage(SeriousnessStage stage) {
        if (stage == SeriousnessStage.STAGE_THREE) return Mode.EA;
        if (stage == SeriousnessStage.STAGE_TWO) return Mode.WINDS;
        return Mode.PILLAR;
    }

    private void refreshSeriousnessMode(LivingEntity user) {
        if (user.level.isClientSide) return;
        Mode desired = modeForStage(SeriousnessAbility.getStage(user));
        if (modes.getCurrentMode() != desired) modes.setMode(user, desired);
    }

    private void beginCharge(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        if (activeMode == Mode.EA) EaFallProtection.grant(user);
        clearStance(user);
        pendingCast = true;
        dodgeProtectionUntil = 0;
        ownsChargeStuck = false;
        stanceUntil = activeMode == Mode.EA
                ? user.level.getGameTime() + EaRuptureShape.AUDIO_FADE_START_TICKS : 0;
        launchOrigin = user.position();
        launchTicks = 0;
        visual = new EaVfxEntity(KaziEntities.EA_VFX.get(), user.level);
        visual.beginCharge(user, this, activeMode == Mode.WINDS);
        user.level.addFreshEntity(visual);
        animation.start(user, KaziAnimations.EA_CHARGE,
                (activeMode == Mode.WINDS ? WindsRaptureShape.CHARGE_TICKS : EaVfxEntity.CHARGE_TICKS) + 5,
                entity -> !entity.isAlive() || !charge.isCharging());
        holdPosition(user);
        visual.setChargeProgress(charge.getChargePercentage());
    }

    private void chargeTick(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide || !pendingCast || !charge.isCharging()) return;
        if (!user.isAlive()) {
            cancelCharge(user);
            return;
        }
        restoreChargingVisual(user);
        holdPosition(user);
        visual.setChargeProgress(charge.getChargePercentage());
        // Release on the final charge tick rather than waiting for the next tick.
        if (charge.getChargeTime() >= charge.getMaxChargeTime()) charge.stopCharging(user);
    }

    private void holdPosition(LivingEntity user) {
        if (activeMode == Mode.WINDS) return;
        if (activeMode == Mode.EA) {
            if (!user.hasEffect(ModEffects.MOVEMENT_BLOCKED.get())) ownsChargeStuck = true;
            user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
            if (!user.isNoGravity()) {
                user.getPersistentData().putBoolean(GRAVITY_LOCK_TAG, true);
                user.setNoGravity(true);
            }
        }
        // Short refreshes also expire safely if the caster disconnects.
        if (!user.hasEffect(ModEffects.REDUCED_FALL.get())) ownsFloat = true;
        user.addEffect(new EffectInstance(ModEffects.REDUCED_FALL.get(), 2, 0, false, false));
        if (launchOrigin != null) {
            double progress = Math.min(++launchTicks, ASCENT_TICKS) / (double) ASCENT_TICKS;
            double height = launchOrigin.y + LAUNCH_HEIGHT * progress;
            if (activeMode == Mode.EA && lockedPosition == null && launchTicks > ASCENT_TICKS
                    && (Math.abs(height - user.getY()) < 0.05D || launchTicks > ASCENT_TICKS + 5)) {
                // Freeze the reached position, including when a ceiling stops ascent.
                lockedPosition = user.position();
            }
            if (lockedPosition != null) {
                // No gravity + no velocity avoids oscillating client/server corrections.
                // Only resync position if something external actually displaces the caster.
                if (user.position().distanceToSqr(lockedPosition) > 0.0001D) {
                    user.teleportTo(lockedPosition.x, lockedPosition.y, lockedPosition.z);
                }
                user.setDeltaMovement(Vector3d.ZERO);
                user.hurtMarked = true;
                user.fallDistance = 0;
                return;
            }
            // Teleporting every tick leaves the player's connection awaiting position
            // acknowledgments and interferes with incoming aim updates. Use motion only.
            // Cap corrections so an obstruction cannot accumulate a violent launch.
            double riseSpeed = LAUNCH_HEIGHT / ASCENT_TICKS;
            user.setDeltaMovement(
                    Math.max(-0.25D, Math.min(0.25D, launchOrigin.x - user.getX())),
                    Math.max(-riseSpeed, Math.min(riseSpeed, height - user.getY())),
                    Math.max(-0.25D, Math.min(0.25D, launchOrigin.z - user.getZ())));
        } else {
            user.setDeltaMovement(Vector3d.ZERO);
        }
        user.hurtMarked = true;
        user.fallDistance = 0;
    }

    private boolean hasActiveStance(LivingEntity user) {
        return activeMode == Mode.EA && user.level.getGameTime() < stanceUntil
                && user.isAlive() && visual != null
                && visual.level == user.level && visual.isAlive() && !visual.isCancelled();
    }

    /** Kami-e's attack filters and ten-tick protection until the audio fade. */
    private float dodgeDuringCharge(LivingEntity user, IAbility ability, DamageSource source, float amount) {
        if (user.level.isClientSide || amount <= 0 || activeMode != Mode.EA
                || !hasActiveStance(user)
                // Ea deliberately blocks its own movement during the charge;
                // Kami-e's momentum check would reject that protected stance.
                || AbilityHelper.isGrabbing(user)) {
            return amount;
        }
        if (user.level.getGameTime() < dodgeProtectionUntil) return 0;

        Entity direct = source.getDirectEntity();
        String type = source.getMsgId();
        boolean eligible = direct instanceof LivingEntity || direct instanceof ProjectileEntity;
        eligible &= "mob".equals(type) || "player".equals(type)
                || "ability_projectile".equals(type) || "ability".equals(type);
        boolean unavoidable = source instanceof ModDamageSource && ((ModDamageSource) source).isUnavoidable();
        if (!eligible || unavoidable) return amount;

        dodgeProtectionUntil = user.level.getGameTime() + DODGE_PROTECTION_TICKS;
        SoundEvent sound = user.getRandom().nextBoolean() ? ModSounds.DODGE_1.get() : ModSounds.DODGE_2.get();
        user.level.playSound(null, user.blockPosition(), sound, SoundCategory.PLAYERS,
                1.0F, .75F + user.getRandom().nextFloat() / 2.0F);
        return 0;
    }

    private void clearChargeStuck(LivingEntity user) {
        if (ownsChargeStuck) {
            EffectInstance stuck = user.getEffect(ModEffects.MOVEMENT_BLOCKED.get());
            // Preserve a longer or stronger immobilization applied by another source.
            if (stuck != null && stuck.getDuration() <= 2 && stuck.getAmplifier() == 0) {
                user.removeEffect(ModEffects.MOVEMENT_BLOCKED.get());
            }
        }
        ownsChargeStuck = false;
    }

    private void clearStance(LivingEntity user) {
        if (user.getPersistentData().getBoolean(GRAVITY_LOCK_TAG)) {
            user.setNoGravity(false);
            user.getPersistentData().remove(GRAVITY_LOCK_TAG);
        }
        lockedPosition = null;
        clearChargeStuck(user);
        if (ownsFloat) {
            EffectInstance floating = user.getEffect(ModEffects.REDUCED_FALL.get());
            if (floating != null && floating.getDuration() <= 2 && floating.getAmplifier() == 0) {
                user.removeEffect(ModEffects.REDUCED_FALL.get());
            }
        }
        ownsFloat = false;
        stanceUntil = 0;
        dodgeProtectionUntil = 0;
        launchOrigin = null;
    }

    private void finishCharge(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        // ChargeComponent also invokes this callback for an early stop.
        if (!pendingCast || !charge.isCharging() || charge.getMaxChargeTime() <= 0
                || charge.getChargeTime() < charge.getMaxChargeTime() || !user.isAlive()
                || visual == null || !visual.isAlive() || visual.isCancelled()) {
            cancelCharge(user);
            return;
        }
        animation.stop(user);
        pendingCast = false;
        if (activeMode != Mode.EA) clearStance(user);
        visual.release(user);
        cooldownComponent.startCooldown(user, activeMode.cooldownTicks);
    }

    private void cancelCharge(LivingEntity user) {
        if (user.level.isClientSide) return;
        clearStance(user);
        if (!pendingCast) return;
        pendingCast = false;
        dodgeProtectionUntil = 0;
        launchOrigin = null;
        if (charge.isCharging()) charge.forceStopCharging(user);
        animation.stop(user);
        if (visual != null && visual.isAlive() && !visual.isReleased() && !visual.isCancelled()) {
            visual.cancel();
        }
        cooldownComponent.startCooldown(user, CANCEL_COOLDOWN_TICKS);
        visual = null;
    }

    /** Release the movement lock before player state is saved or moved to another world. */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "kazimod")
    public static final class StanceCleanup {
        private static void cleanup(LivingEntity user) {
            if (user.level.isClientSide) return;
            xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data =
                    xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability.get(user);
            if (data != null) {
                for (IAbility ability : data.getEquippedAbilities()) {
                    if (ability instanceof EaAbility) ((EaAbility) ability).cancelCharge(user);
                }
            }
            // Also recover a lock saved during an interrupted server shutdown.
            if (user.getPersistentData().getBoolean(GRAVITY_LOCK_TAG)) {
                user.setNoGravity(false);
                user.getPersistentData().remove(GRAVITY_LOCK_TAG);
            }
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
            cleanup(event.getPlayer());
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            cleanup(event.getPlayer());
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
            cleanup(event.getPlayer());
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
            cleanup(event.getEntityLiving());
        }
    }

    public boolean isChargingVfx(EaVfxEntity entity) {
        return pendingCast && visual == entity && charge.isCharging();
    }

    private void restoreChargingVisual(LivingEntity user) {
        if (visual != null && visual.isAlive() && !visual.isCancelled()) return;
        visual = new EaVfxEntity(KaziEntities.EA_VFX.get(), user.level);
        visual.beginCharge(user, this, activeMode == Mode.WINDS);
        user.level.addFreshEntity(visual);
        visual.setChargeProgress(charge.getChargePercentage());
    }

    public boolean isWindsMode() { return modes.getCurrentMode() == Mode.WINDS; }

    public boolean hit(LivingEntity user, LivingEntity target) {
        return hit(user, target, Mode.EA);
    }

    public boolean hit(LivingEntity user, LivingEntity target, Mode castMode) {
        return damage.hurtTarget(user, target, castMode.damage);
    }

    private void updateDisplay(Mode mode) {
        setDisplayName(mode.title);
        String icon = mode == Mode.PILLAR ? "ea_uta_darunaki"
                : mode == Mode.WINDS ? "ea_winds_of_rapture" : "ea_divine_sword_of_rapture";
        setDisplayIcon(new ResourceLocation("kazimod", "textures/abilities/" + icon + ".png"));
    }

    private static final class EaModes extends AltModeComponent<Mode> {
        EaModes(EaAbility ability) { super(ability, Mode.class, Mode.PILLAR, true); }
        @Override public void setNextInCycle(LivingEntity user) {
            ((EaAbility)getAbility()).refreshSeriousnessMode(user);
        }
        @Override public void load(CompoundNBT tag) {
            String mode = tag.getString("currentMode");
            tag.putString("currentMode", "WINDS".equals(mode) || Mode.WINDS.title.equals(mode) ? "WINDS"
                    : "EA".equals(mode) || Mode.EA.title.equals(mode) || "Ea: Divine Sword of Rapture".equals(mode) ? "EA"
                    : "PILLAR");
            super.load(tag);
        }
    }

    @Override public void load(CompoundNBT tag) {
        super.load(tag);
        dodgeProtectionUntil = 0;
        ownsChargeStuck = false;
        ownsFloat = false;
        stanceUntil = 0;
        updateDisplay(modes.getCurrentMode());
        activeMode = modes.getCurrentMode();
        // The transient VFX cannot survive a save/reconnect, but the saved charge can.
        // Keep it pending so cleanup also works if the base ability disables it first.
        pendingCast = charge.isCharging();
        visual = null;
    }
}
