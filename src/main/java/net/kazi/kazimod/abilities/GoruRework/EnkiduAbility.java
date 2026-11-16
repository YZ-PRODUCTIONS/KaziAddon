package net.kazi.kazimod.abilities.GoruRework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.kazi.kazimod.entities.EnkiduVfxEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

/** Seriousness selects the counter, focused bind, area bind or absolute bind for this Goru slot. */
public final class EnkiduAbility extends Ability {
    public static final double RANGE = 64;
    public static final double AREA_RADIUS = 12;
    public static final double AREA_HALF_HEIGHT = 8;
    public static final int MAX_AREA_TARGETS = 8;
    public static final double ABSOLUTE_RADIUS = 40;
    private static final int CANCEL_COOLDOWN = 10 * 20;
    private static final int COUNTER_WINDOW_TICKS = 4 * 20;
    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/enkidu_chains_of_heaven.png");

    public enum Mode {
        SINGLE("Chains of Heaven", 5, 60, 600, 20),
        AREA("Chains of Heaven: Enkidu", 10, 60, 600, 20),
        COUNTER("ZASSHU", 5, 60, 600, 20),
        ABSOLUTE("Enkidu: Absolute Bind", 10, 60, 600, 20);
        public final String title;
        public final int chargeTicks, bindTicks, cooldownTicks;
        public final float damage;
        Mode(String title, int charge, int bind, int cooldown, float damage) {
            this.title = title; chargeTicks = charge; bindTicks = bind; cooldownTicks = cooldown; this.damage = damage;
        }
        @Override public String toString() { return title; }
    }

    public static final AbilityCore<EnkiduAbility> INSTANCE = new AbilityCore.Builder<>(
            "Enkidu: Chains of Heaven", AbilityCategory.DEVIL_FRUITS, EnkiduAbility::new)
            .setIcon(ICON).setSourceHakiNature(SourceHakiNature.SPECIAL)
            .setSourceType(SourceType.INTERNAL).setSourceElement(SourceElement.SHOCKWAVE)
            .addDescriptionLine(
                    new StringTextComponent("Chains of Heaven: Aim at an enemy within 64 blocks. Bind them for 3 seconds, then deal 20 damage after the bind expires."),
                    new StringTextComponent("Chains of Heaven: Enkidu: Mark a 12-block radius ahead. Bind up to 8 enemies for 3 seconds, then deal 20 damage each after the bind expires."),
                    new StringTextComponent("Single: 0.25 second charge. Area: 0.5 second charge. All forms have a 30 second cooldown."),
                    new StringTextComponent("ZASSHU: Wait up to 4 seconds for an enemy attack. Negate the triggering hit and bind the attacker, then deal 20 damage after release."),
                    new StringTextComponent("Enkidu: Absolute Bind: Bind all hostile players, mobs and NPCs within 40 blocks of you for 3 seconds, then deal 20 damage each. 0.5 second charge, 30 second cooldown."),
                    new StringTextComponent("Seriousness selects the form automatically: Normal - ZASSHU; Stage 1 - Chains of Heaven; Stage 2 - Chains of Heaven: Enkidu; Stage 3 - Enkidu: Absolute Bind."),
                    new StringTextComponent("Once activated, the charge and counter window cannot be canceled."))
            .setUnlockCheck(SeriousnessAbility::isEligible)
            .build();

    private final AltModeComponent<Mode> modes = new EnkiduModes(this)
            .addChangeModeEvent((user, ability, mode) -> updateDisplay(mode));
    private final ChargeComponent charge = new CommittedChargeComponent(this,
            () -> this.pending, this::chargeTick).addEndEvent(this::release);
    private final AnimationComponent animation = new AnimationComponent(this);
    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final ContinuousComponent counter = new ContinuousComponent(this, true) {
        private long endAt;
        @Override public void startContinuity(LivingEntity user, float duration) {
            if (isContinuous()) return;
            endAt = user.level.getGameTime() + (long) duration;
            super.startContinuity(user, duration);
        }
        @Override protected void doTick(LivingEntity user) {
            if (isContinuous() && (!user.isAlive() || user.level.getGameTime() >= endAt)) stopContinuity(user);
        }
        @Override public void stopContinuity(LivingEntity user) {
            if (!user.level.isClientSide && isContinuous() && user.isAlive() && !EnkiduAbility.this.counterTriggered
                    && user.level.getGameTime() < endAt) return;
            super.stopContinuity(user);
        }
    }
            .addEndEvent((user, ability) -> {
                if (!user.level.isClientSide && !this.counterTriggered) cooldownComponent.startCooldown(user, Mode.COUNTER.cooldownTicks);
            });
    private final DamageTakenComponent counterDamage = new DamageTakenComponent(this,
            this::counterHit, DamageTakenComponent.DamageState.ATTACK);
    private EnkiduVfxEntity visual;
    private Mode activeMode = Mode.COUNTER;
    private boolean pending;
    private boolean counterTriggered;
    private Vector3d castCenter;
    private LivingEntity castTarget;

    public EnkiduAbility(AbilityCore<EnkiduAbility> core) {
        super(core);
        isNew = true;
        addComponents(modes, charge, animation, damage, counter, counterDamage);
        updateDisplay(Mode.COUNTER);
        addEquipEvent((user, ability) -> syncSeriousnessMode(user));
        addUseEvent(this::useChains);
        addTickEvent((user, ability) -> {
            if (user.level.isClientSide) return;
            syncSeriousnessMode(user);
            if (counter.isContinuous() && !user.isAlive()) counter.stopContinuity(user);
            if (charge.isCharging() && !pending) {
                pending = true; // A saved charge cannot restore its transient portals.
                cancel(user);
            } else if (pending && !user.isAlive()) cancel(user);
            else if (pending) restoreChargingVisual(user);
            if (visual != null && visual.isAlive() && !user.isAlive()) visual.fade();
        });
        addRemoveEvent((user, ability) -> {
            if (!user.level.isClientSide && counter.isContinuous()) counter.stopContinuity(user);
            cancel(user);
            if (visual != null && !user.level.isClientSide) visual.fade();
            visual = null;
        });
    }

    private void useChains(LivingEntity user, IAbility ability) {
        if (!SeriousnessAbility.isEligible(user)) return;
        if (user.level.isClientSide) return;
        if (counter.isContinuous() || pending || charge.isCharging()) return;
        if (visual != null && visual.isAlive() && !visual.isFading()) return;
        syncSeriousnessMode(user);
        activeMode = modes.getCurrentMode();
        if (activeMode == Mode.COUNTER) {
            counter.startContinuity(user, COUNTER_WINDOW_TICKS);
            return;
        }
        LivingEntity target = activeMode == Mode.SINGLE ? aimedTarget(user) : null;
        if (activeMode == Mode.SINGLE && target == null) {
            if (user instanceof PlayerEntity) ((PlayerEntity) user).displayClientMessage(
                    new StringTextComponent("Aim at an enemy within 64 blocks to summon the chains."), true);
            return;
        }
        Vector3d center = activeMode == Mode.ABSOLUTE ? user.position()
                : target == null ? aimedArea(user) : target.position();
        startChains(user, center, target);
    }

    private boolean startChains(LivingEntity user, Vector3d center, LivingEntity target) {
        castCenter = center;
        castTarget = target;
        visual = new EnkiduVfxEntity(KaziEntities.ENKIDU_VFX.get(), user.level);
        // Counter uses the identical single-target chain visuals and bind lifecycle.
        visual.begin(user, this, activeMode, center, target);
        if (!user.level.addFreshEntity(visual)) { visual = null; return false; }
        pending = true;
        charge.startCharging(user, activeMode.chargeTicks);
        animation.start(user, ModAnimations.POINT_LEFT_ARM, activeMode.chargeTicks + 5,
                entity -> !entity.isAlive() || !charge.isCharging());
        return true;
    }

    private float counterHit(LivingEntity user, IAbility ability, DamageSource source, float amount) {
        if (user.level.isClientSide || !counter.isContinuous() || amount <= 0) return amount;
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof LivingEntity)) return amount;
        LivingEntity attacker = (LivingEntity) sourceEntity;
        if (!isEnemy(user, attacker) || user.distanceToSqr(attacker) > RANGE * RANGE
                || AbilityHelper.isDodging(attacker)
                || !clearPath(user, user.getEyePosition(1), attacker.getEyePosition(1))) return amount;
        counterTriggered = true;
        try {
            counter.stopContinuity(user);
        } finally {
            counterTriggered = false;
        }
        activeMode = Mode.COUNTER;
        if (startChains(user, attacker.position(), attacker)) return 0;
        cooldownComponent.startCooldown(user, CANCEL_COOLDOWN);
        return amount;
    }

    private void chargeTick(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide || !pending) return;
        if (!user.isAlive()) {
            cancel(user); return;
        }
        restoreChargingVisual(user);
        visual.setChargeProgress(charge.getChargePercentage());
        if (charge.getChargeTime() >= charge.getMaxChargeTime()) charge.stopCharging(user);
    }

    private void release(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        if (!pending || !charge.isCharging() || charge.getMaxChargeTime() <= 0
                || charge.getChargeTime() < charge.getMaxChargeTime() || !user.isAlive()
                || visual == null || !visual.isAlive() || visual.isFading()) { cancel(user); return; }
        pending = false;
        animation.stop(user);
        visual.launch();
        cooldownComponent.startCooldown(user, activeMode.cooldownTicks);
    }

    private void cancel(LivingEntity user) {
        if (user.level.isClientSide || !pending) return;
        pending = false;
        if (charge.isCharging()) charge.forceStopCharging(user);
        animation.stop(user);
        if (visual != null) visual.fade();
        cooldownComponent.startCooldown(user, CANCEL_COOLDOWN);
    }

    public boolean ownsCast(EnkiduVfxEntity entity) {
        return visual == entity && (!entity.isCharging() || pending && charge.isCharging());
    }

    private void restoreChargingVisual(LivingEntity user) {
        if (visual != null && visual.isAlive() && !visual.isFading()) return;
        if (castCenter == null) return;
        visual = new EnkiduVfxEntity(KaziEntities.ENKIDU_VFX.get(), user.level);
        visual.begin(user, this, activeMode, castCenter, castTarget);
        user.level.addFreshEntity(visual);
        visual.setChargeProgress(charge.getChargePercentage());
    }

    public boolean strike(LivingEntity user, LivingEntity target, Mode mode) {
        // The delayed strike still respects the normal damage pipeline and dodges.
        return isEnemy(user, target) && !AbilityHelper.isDodging(target)
                && damage.hurtTarget(user, target, mode.damage) && !AbilityHelper.isDodging(target);
    }

    public static boolean isEnemy(LivingEntity user, LivingEntity target) {
        return user != null && user.isAlive() && target != null && user != target && target.isAlive()
                && target.level == user.level && !target.isSpectator()
                && !(target instanceof PlayerEntity && ((PlayerEntity) target).isCreative())
                && !user.isAlliedTo(target) && !target.isAlliedTo(user)
                && ModEntityPredicates.getEnemyFactions(user).test(target);
    }

    public static boolean clearPath(LivingEntity user, Vector3d from, Vector3d to) {
        return user.level.clip(new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, user)).getType() == RayTraceResult.Type.MISS;
    }

    private static LivingEntity aimedTarget(LivingEntity user) {
        Vector3d eye = user.getEyePosition(1), end = eye.add(user.getLookAngle().scale(RANGE));
        BlockRayTraceResult wall = user.level.clip(new RayTraceContext(eye, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        if (wall.getType() != RayTraceResult.Type.MISS) end = wall.getLocation();
        LivingEntity nearest = null;
        double distance = eye.distanceToSqr(end);
        for (LivingEntity target : user.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(eye, end).inflate(1), candidate -> isEnemy(user, candidate))) {
            AxisAlignedBB box = target.getBoundingBox().inflate(0.45);
            Optional<Vector3d> hit = box.clip(eye, end);
            double next = box.contains(eye) ? 0 : hit.isPresent() ? eye.distanceToSqr(hit.get()) : Double.MAX_VALUE;
            if (next < distance && clearPath(user, eye, target.getEyePosition(1))) {
                distance = next; nearest = target;
            }
        }
        return nearest;
    }

    private static Vector3d aimedArea(LivingEntity user) {
        LivingEntity target = aimedTarget(user);
        if (target != null) return target.position();
        Vector3d eye = user.getEyePosition(1), end = eye.add(user.getLookAngle().scale(RANGE));
        BlockRayTraceResult hit = user.level.clip(new RayTraceContext(eye, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        return hit.getType() == RayTraceResult.Type.MISS ? end : hit.getLocation().add(
                hit.getDirection().getStepX() * 0.15, hit.getDirection().getStepY() * 0.15,
                hit.getDirection().getStepZ() * 0.15);
    }

    public static List<LivingEntity> areaTargets(LivingEntity user, Vector3d center) {
        List<LivingEntity> targets = new ArrayList<>(user.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(center, center).inflate(AREA_RADIUS, AREA_HALF_HEIGHT, AREA_RADIUS), target -> {
                    return isEnemy(user, target) && insideArea(center, target)
                            && clearPath(user, user.getEyePosition(1), target.getEyePosition(1))
                            && clearPath(user, center.add(0, 0.25, 0), target.getEyePosition(1));
                }));
        targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)));
        return targets.subList(0, Math.min(MAX_AREA_TARGETS, targets.size()));
    }

    public static List<LivingEntity> absoluteTargets(LivingEntity user) {
        return new ArrayList<>(user.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(user.position(), user.position()).inflate(ABSOLUTE_RADIUS),
                target -> isAbsoluteTarget(user, target)));
    }

    public static boolean isAbsoluteTarget(LivingEntity user, LivingEntity target) {
        return isEnemy(user, target) && !target.removed
                && user.distanceToSqr(target) <= ABSOLUTE_RADIUS * ABSOLUTE_RADIUS;
    }

    public static boolean insideArea(Vector3d center, LivingEntity target) {
        double dx = target.getX() - center.x, dz = target.getZ() - center.z;
        AxisAlignedBB box = target.getBoundingBox();
        return dx * dx + dz * dz <= AREA_RADIUS * AREA_RADIUS
                && box.maxY >= center.y - AREA_HALF_HEIGHT && box.minY <= center.y + AREA_HALF_HEIGHT;
    }

    public static Mode modeForStage(SeriousnessStage stage) {
        if (stage == SeriousnessStage.STAGE_THREE) return Mode.ABSOLUTE;
        if (stage == SeriousnessStage.STAGE_TWO) return Mode.AREA;
        return stage == SeriousnessStage.STAGE_ONE ? Mode.SINGLE : Mode.COUNTER;
    }

    private void syncSeriousnessMode(LivingEntity user) {
        if (!user.level.isClientSide) {
            // activeMode and the VFX keep the form captured when the current cast began.
            modes.setMode(user, modeForStage(SeriousnessAbility.getStage(user)));
        }
    }

    @Override public void load(CompoundNBT tag) {
        super.load(tag);
        updateDisplay(modes.getCurrentMode());
        pending = charge.isCharging();
        visual = null;
    }

    private void updateDisplay(Mode mode) {
        setDisplayName(mode.title);
        String icon = mode == Mode.COUNTER ? "enkidu_zasshu"
                : mode == Mode.SINGLE ? "enkidu_single_bind"
                : mode == Mode.ABSOLUTE ? "enkidu_absolute_bind" : "enkidu_chains_of_heaven";
        setDisplayIcon(new ResourceLocation("kazimod", "textures/abilities/" + icon + ".png"));
    }

    private static final class EnkiduModes extends AltModeComponent<Mode> {
        EnkiduModes(EnkiduAbility ability) { super(ability, Mode.class, Mode.COUNTER, true); }
        @Override public void setNextInCycle(LivingEntity user) {
            ((EnkiduAbility)getAbility()).syncSeriousnessMode(user);
        }
        @Override public void load(CompoundNBT tag) {
            String mode = tag.getString("currentMode");
            // The base component saves enum.toString(), but loads enum.name().
            tag.putString("currentMode", Mode.ABSOLUTE.title.equals(mode) || "ABSOLUTE".equals(mode) ? "ABSOLUTE"
                    : Mode.COUNTER.title.equals(mode) || "COUNTER".equals(mode)
                    || "Chains of Heaven: Counter".equals(mode) ? "COUNTER"
                    : Mode.AREA.title.equals(mode) || "AREA".equals(mode) ? "AREA"
                    : Mode.SINGLE.title.equals(mode) || "SINGLE".equals(mode) ? "SINGLE" : "COUNTER");
            super.load(tag);
        }
    }
}
