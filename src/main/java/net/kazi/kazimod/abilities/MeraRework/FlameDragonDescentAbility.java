package net.kazi.kazimod.abilities.MeraRework;

import net.kazi.kazimod.entities.GoryutenmetsuDragonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.List;

public class FlameDragonDescentAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "flame_dragon_descent",
            new Pair[]{ImmutablePair.of("Launches the user skyward onto a fire dragon. Reuse the move to fire the dragon ahead before it detonates.", null)}
    );
    private static final float COOLDOWN = 500.0F;
    private static final float DAMAGE = 30.0F;
    private static final float HOLD_TIME = 200.0F;
    private static final int ASCENT_TICKS = 20;
    private static final double RIDE_SPEED = 1.35D;
    private static final double PROJECTILE_SPEED = 1.75D;
    private static final double IMPACT_RADIUS = 5.25D;
    private static final float DRAGON_SCALE = 2.45F;
    private static final int AUTO_RELEASE_TICKS = 20;

    public static final AbilityCore<FlameDragonDescentAbility> INSTANCE =
            new AbilityCore.Builder<>("Flame Dragon Descent", AbilityCategory.DEVIL_FRUITS, FlameDragonDescentAbility::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            ContinuousComponent.getTooltip(HOLD_TIME),
                            CooldownComponent.getTooltip(COOLDOWN),
                            DealDamageComponent.getTooltip(DAMAGE)
                    )
                    .setSourceElement(SourceElement.FIRE)
                    .setSourceHakiNature(SourceHakiNature.SPECIAL)
                    .setSourceType(new SourceType[]{SourceType.INTERNAL, SourceType.INDIRECT})
                    .setUnlockCheck(FlameDragonDescentAbility::canUnlock)
                    .build();

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final Interval particleInterval = new Interval(2);

    private GoryutenmetsuDragonEntity dragonEntity;
    private Vector3d projectileDirection = Vector3d.ZERO;
    private float projectileYaw;
    private boolean projectileReleased;
    private boolean impacted;
    private boolean mounted;

    public FlameDragonDescentAbility(AbilityCore<FlameDragonDescentAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.dealDamageComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            if (!this.projectileReleased && this.dragonEntity != null) {
                this.projectileReleased = true;
                this.projectileYaw = this.getLockedYaw(entity);
                this.projectileDirection = this.directionFromYaw(this.projectileYaw);
                entity.stopRiding();
                this.mounted = false;
                entity.level.playSound(null, entity.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.3F);
            }
            return;
        }

        this.continuousComponent.startContinuity(entity, HOLD_TIME);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.cleanupDragon(entity);
        this.projectileReleased = false;
        this.impacted = false;
        this.mounted = false;
        this.projectileDirection = Vector3d.ZERO;
        this.projectileYaw = 0.0F;
        this.particleInterval.restartIntervalToZero();
        entity.level.playSound(null, entity.blockPosition(), ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 2.5F, 0.9F);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }

        entity.fallDistance = 0.0F;
        float time = this.continuousComponent.getContinueTime();
        if (time <= ASCENT_TICKS) {
            AbilityHelper.setDeltaMovement(entity, 0.0D, 1.35D, 0.0D);
            if (time == ASCENT_TICKS) {
                this.spawnDragon(entity, entity.position().add(0.0D, 3.25D, 0.0D), HOLD_TIME - ASCENT_TICKS + 20.0F);
                entity.startRiding(this.dragonEntity, true);
                this.mounted = true;
                entity.level.playSound(null, entity.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.9F, 1.45F);
            }
            this.spawnFlightParticles(entity.position(), entity.level);
            return;
        }

        if (this.dragonEntity == null || !this.dragonEntity.isAlive()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        if (!this.projectileReleased && time >= HOLD_TIME - AUTO_RELEASE_TICKS) {
            this.projectileReleased = true;
            this.projectileYaw = this.getLockedYaw(entity);
            this.projectileDirection = this.directionFromYaw(this.projectileYaw);
            entity.stopRiding();
            this.mounted = false;
        }

        Vector3d current = this.dragonEntity.position();
        float lockedYaw = this.projectileReleased ? this.projectileYaw : this.getLockedYaw(entity);
        Vector3d motion = this.projectileReleased
                ? this.directionFromYaw(lockedYaw).scale(PROJECTILE_SPEED)
                : this.directionFromYaw(lockedYaw).scale(RIDE_SPEED);

        BlockRayTraceResult blockHit = entity.level.clip(new RayTraceContext(
                current,
                current.add(motion),
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.ANY,
                entity
        ));
        if (blockHit.getType() == net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
            this.resolveImpact(entity, blockHit.getLocation());
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        Entity hitEntity = this.findCollisionTarget(entity, motion);
        if (hitEntity instanceof LivingEntity) {
            this.resolveImpact(entity, hitEntity.position().add(0.0D, hitEntity.getBbHeight() * 0.35D, 0.0D));
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        Vector3d next = current.add(motion);
        this.dragonEntity.setDeltaMovement(motion);
        this.dragonEntity.setPos(next.x, next.y, next.z);
        this.updateDragonPose(lockedYaw);
        if (this.particleInterval.canTick()) {
            this.spawnFlightParticles(next, entity.level);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        entity.stopRiding();
        entity.fallDistance = 0.0F;
        this.cleanupDragon(entity);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
        this.projectileReleased = false;
        this.projectileDirection = Vector3d.ZERO;
        this.projectileYaw = 0.0F;
        this.mounted = false;
        this.impacted = false;
    }

    private void spawnDragon(LivingEntity entity, Vector3d pos, float lifeTicks) {
        this.dragonEntity = GoryutenmetsuDragonEntity.create(entity.level, pos.x, pos.y, pos.z, DRAGON_SCALE, 0.58F, Math.round(lifeTicks));
        this.dragonEntity.setColor(1.0F, 0.36F, 0.12F);
        this.projectileYaw = this.getLockedYaw(entity);
        this.updateDragonPose(this.projectileYaw);
        entity.level.addFreshEntity(this.dragonEntity);
    }

    private void updateDragonPose(float yaw) {
        if (this.dragonEntity == null) {
            return;
        }

        float pitch = 0.0F;
        this.dragonEntity.setRenderYaw(yaw);
        this.dragonEntity.setRenderPitch(pitch);
        this.dragonEntity.setRenderRoll(0.0F);
        this.dragonEntity.yRot = yaw;
        this.dragonEntity.yRotO = yaw;
        this.dragonEntity.xRot = pitch;
        this.dragonEntity.xRotO = pitch;
        this.dragonEntity.setScale(DRAGON_SCALE);
        this.dragonEntity.setAlpha(0.58F);
    }

    private float getLockedYaw(LivingEntity entity) {
        return entity.yHeadRot;
    }

    private Vector3d directionFromYaw(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vector3d(-Math.sin(radians), 0.0D, Math.cos(radians));
    }

    private Entity findCollisionTarget(LivingEntity owner, Vector3d motion) {
        AxisAlignedBB box = this.dragonEntity.getBoundingBox().expandTowards(motion).inflate(1.8D);
        List<Entity> hits = owner.level.getEntities(owner, box, target ->
                target.isAlive() && target != owner && target != this.dragonEntity && (!target.isPassenger() || target.getVehicle() != this.dragonEntity));
        return hits.isEmpty() ? null : hits.get(0);
    }

    private void resolveImpact(LivingEntity entity, Vector3d center) {
        if (this.impacted) {
            return;
        }

        this.impacted = true;
        AxisAlignedBB box = new AxisAlignedBB(
                center.x - IMPACT_RADIUS, center.y - IMPACT_RADIUS, center.z - IMPACT_RADIUS,
                center.x + IMPACT_RADIUS, center.y + IMPACT_RADIUS, center.z + IMPACT_RADIUS
        );
        List<LivingEntity> targets = entity.level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> target != entity && target.isAlive() && xyz.pixelatedw.mineminenomi.init.ModEntityPredicates.getEnemyFactions(entity).test(target)
        );
        for (LivingEntity target : targets) {
            if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE)) {
                target.setSecondsOnFire(8);
            }
        }

        entity.level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.1F, 0.8F);
        entity.level.playSound(null, entity.blockPosition(), ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 2.8F, 0.75F);
        this.spawnBurstParticles(center, entity.level);
    }

    private void spawnFlightParticles(Vector3d center, net.minecraft.world.World world) {
        for (int i = 0; i < 8; i++) {
            SimpleParticleData mera = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
            mera.setLife(16);
            mera.setSize(1.8F);
            WyHelper.spawnParticles(mera, (net.minecraft.world.server.ServerWorld) world,
                    center.x + (WyHelper.randomDouble() - 0.5D) * 1.3D,
                    center.y + (WyHelper.randomDouble() - 0.5D) * 1.0D,
                    center.z + (WyHelper.randomDouble() - 0.5D) * 1.3D);
        }
    }

    private void spawnBurstParticles(Vector3d center, net.minecraft.world.World world) {
        for (int i = 0; i < 50; i++) {
            SimpleParticleData mera = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
            mera.setLife(18);
            mera.setSize(2.3F);
            WyHelper.spawnParticles(mera, (net.minecraft.world.server.ServerWorld) world,
                    center.x + (WyHelper.randomDouble() - 0.5D) * 3.2D,
                    center.y + WyHelper.randomDouble() * 2.4D,
                    center.z + (WyHelper.randomDouble() - 0.5D) * 3.2D);
        }
    }

    private Vector3d safeDirection(Vector3d direction) {
        return direction.lengthSqr() < 0.001D ? new Vector3d(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    private void cleanupDragon(LivingEntity entity) {
        if (this.dragonEntity != null) {
            this.dragonEntity.remove();
            this.dragonEntity = null;
        }
        if (entity != null) {
            entity.stopRiding();
        }
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }
}
