package net.kazi.kazimod.abilities.MochiRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.mochi.AnemoneProjectile;
import net.MrMagicalCart.cartaddon.entities.projectiles.mochi.DonutProjectile;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

import java.util.ArrayList;
import java.util.List;

/** Local, independently editable clone of CartAddon's Mochi Ginchaku. */
public class MochiGinchakuClone extends Ability {
    private static final float COOLDOWN = 400.0F;
    private static final float HOLD_TIME = 100.0F;
    private static final float CHARGE_TIME = 10.0F;
    private static final int DONUT_COUNT = 15;
    private static final int FIRE_INTERVAL = 30;
    private static final double SPAWN_TARGET_RANGE = 80.0D;
    private static final double HEIGHT_ABOVE_AIM = 15.0D;
    private static final double DONUT_FORMATION_RADIUS = 15.0D;

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "mochi_ginchaku",
            new Pair[]{ImmutablePair.of(
                    "The user creates donuts all around themselves, which enable them to summon devastation upon their worst foe.",
                    null)});

    public static final AbilityCore<MochiGinchakuClone> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);
    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(100, this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final PoolComponent poolComponent =
            new PoolComponent(this, CartAbilityPools.MOCHI_ABILITY, new AbilityPool2[0]);
    private final List<DonutProjectile> donuts = new ArrayList<>();

    public MochiGinchakuClone(AbilityCore<MochiGinchakuClone> core) {
        super(core);
        this.isNew = true;
        this.addComponents(poolComponent, chargeComponent, continuousComponent, projectileComponent);
        this.addCanUseCheck(AbilityLimits::usingBrawler);
        this.addCanUseCheck((entity, ability) -> findSpawnTarget(entity) != null
                ? AbilityUseResult.success()
                : AbilityUseResult.fail(null));
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!continuousComponent.isContinuous() && !chargeComponent.isCharging()) {
            chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        clearDonuts();
        LivingEntity spawnTarget = findSpawnTarget(entity);
        if (spawnTarget == null) return;
        Vector3d spawnCenter = spawnTarget.position().add(0.0D, HEIGHT_ABOVE_AIM, 0.0D);
        for (int i = 0; i < DONUT_COUNT; i++) {
            DonutProjectile donut = new DonutProjectile(entity.level, entity, this);
            donut.setOwner(entity);
            double angle = Math.PI * 2.0D * i / DONUT_COUNT;
            double yOffset = ((i % 3) - 1) * 1.5D;
            donut.setPos(spawnCenter.x + Math.cos(angle) * DONUT_FORMATION_RADIUS,
                    spawnCenter.y + yOffset,
                    spawnCenter.z + Math.sin(angle) * DONUT_FORMATION_RADIUS);
            donut.setSetPos(true);
            entity.level.addFreshEntity(donut);
            donuts.add(donut);
        }
    }

    private LivingEntity findSpawnTarget(LivingEntity owner) {
        PlayerEntity playerTarget = owner.level.getEntitiesOfClass(PlayerEntity.class,
                        owner.getBoundingBox().inflate(SPAWN_TARGET_RANGE),
                        target -> target != owner
                                && target.isAlive()
                                && !target.isSpectator()
                                && !owner.isAlliedTo(target))
                .stream()
                .min((first, second) -> Double.compare(
                        owner.distanceToSqr(first), owner.distanceToSqr(second)))
                .orElse(null);
        if (playerTarget != null) return playerTarget;

        return owner.level.getEntitiesOfClass(LivingEntity.class,
                        owner.getBoundingBox().inflate(SPAWN_TARGET_RANGE),
                        target -> target != owner
                                && !(target instanceof PlayerEntity)
                                && target.isAlive()
                                && !owner.isAlliedTo(target))
                .stream()
                .min((first, second) -> Double.compare(
                        owner.distanceToSqr(first), owner.distanceToSqr(second)))
                .orElse(null);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (AbilityLimits.cancelBrawler(entity)) {
            chargeComponent.stopCharging(entity);
            return;
        }
        if (entity.level.isClientSide) return;

        entity.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
        LivingEntity target = findAutomaticTarget(entity);
        Vector3d targetPosition = target != null ? getTargetPosition(target) : null;
        for (DonutProjectile donut : donuts) {
            if (donut == null || !donut.isAlive()) continue;
            if (targetPosition != null) pointDonutAtTarget(donut, targetPosition);
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        continuousComponent.startContinuity(entity, HOLD_TIME);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (AbilityLimits.cancelBrawler(entity)) {
            continuousComponent.stopContinuity(entity);
            return;
        }
        if (entity.level.isClientSide) return;

        entity.addEffect(new EffectInstance(Effects.DIG_SPEED, 2, 2, false, false));
        boolean shouldFire = continuousComponent.getContinueTime() % FIRE_INTERVAL == 0.0F;
        LivingEntity target = findAutomaticTarget(entity);
        if (target == null) return;
        Vector3d targetPosition = getTargetPosition(target);
        for (DonutProjectile donut : donuts) {
            if (donut == null || !donut.isAlive()) continue;
            pointDonutAtTarget(donut, targetPosition);
            if (shouldFire) fireFromDonut(entity, donut, targetPosition);
        }
    }

    private LivingEntity findAutomaticTarget(LivingEntity owner) {
        return owner.level.getEntitiesOfClass(LivingEntity.class,
                        owner.getBoundingBox().inflate(64.0D),
                        target -> target != owner && target.isAlive() && !owner.isAlliedTo(target))
                .stream()
                .min((first, second) -> Double.compare(
                        targetPriority(owner, first), targetPriority(owner, second)))
                .orElse(null);
    }

    private double targetPriority(LivingEntity owner, LivingEntity target) {
        // Any eligible player outranks a non-player, with distance deciding
        // between targets in the same category.
        double categoryPenalty = target instanceof PlayerEntity ? 0.0D : 1000000.0D;
        return categoryPenalty + owner.distanceToSqr(target);
    }

    private Vector3d getTargetPosition(LivingEntity target) {
        return target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
    }

    private void pointDonutAtTarget(DonutProjectile donut, Vector3d targetPosition) {
        donut.lookAt(EntityAnchorArgument.Type.FEET, targetPosition);
        donut.xRot = -donut.xRot;
        donut.yRot = -donut.yRot;
    }

    private void fireFromDonut(LivingEntity entity, DonutProjectile donut, Vector3d targetPosition) {
        AnemoneProjectile projectile = new AnemoneProjectile(entity.level, entity, this);
        projectile.setOwner(entity);
        projectile.setPos(donut.getX(), donut.getY(), donut.getZ());
        projectile.lookAt(EntityAnchorArgument.Type.FEET, targetPosition);
        projectile.xRot = -projectile.xRot;
        projectile.yRot = -projectile.yRot;
        Vector3d shotDirection = targetPosition.subtract(projectile.position()).normalize();
        projectile.setDeltaMovement(shotDirection.scale(2.5D));
        entity.level.addFreshEntity(projectile);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) clearDonuts();
        cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void clearDonuts() {
        for (DonutProjectile donut : donuts) {
            if (donut != null && donut.isAlive()) donut.remove();
        }
        donuts.clear();
    }

    private AnemoneProjectile createProjectile(LivingEntity entity) {
        return new AnemoneProjectile(entity.level, entity, this);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Mochi Ginchaku", AbilityCategory.DEVIL_FRUITS, MochiGinchakuClone::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(HOLD_TIME),
                        CooldownComponent.getTooltip(COOLDOWN))
                .setIcon(new ResourceLocation("cartaddon", "textures/abilities/mochi_ginchaku.png"))
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .build();
    }
}
