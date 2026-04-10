package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.NetsuRework.HellfireBirdAbility;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.LiquidBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.SnowLayerBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class HellfireBirdProjectile extends AbilityProjectileEntity {

    private static final double MAX_TRAVEL_DISTANCE = 64.0D;
    private static final int MAX_LIFE = 80;
    private static final double TRACK_RANGE = 20.0D;
    private static final double TRACK_ANGLE_DOT = 0.3D;
    private static final double TRACK_STRENGTH = 0.085D;
    private static final float EXPLOSION_RADIUS = 3.5F;
    private static final float DAMAGE = 35.0F;

    private static final BlockProtectionRule GRIEF_RULE =
            new BlockProtectionRule.Builder(new BlockProtectionRule[]{
                    LiquidBlockProtectionRule.INSTANCE,
                    SnowLayerBlockProtectionRule.INSTANCE
            }).build();

    private UUID homingTarget;
    private double traveledDistance;
    private boolean exploded;

    public HellfireBirdProjectile(EntityType<? extends HellfireBirdProjectile> type, World world) {
        super(type, world);
    }

    public HellfireBirdProjectile(World world, LivingEntity thrower) {
        super(KaziEntities.HELLFIRE_BIRD.get(), world, thrower, HellfireBirdAbility.INSTANCE);
        this.setDamage(0.0F);
        this.setArmorPiercing(0.25F);
        this.setMaxLife(MAX_LIFE);
        this.setCanGetStuckInGround();
        this.setEntityCollisionSize(1.35D);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::explodeAt;
    }

    @Override
    public void tick() {
        Vector3d previousPos = this.position();

        if (!this.level.isClientSide && !this.exploded) {
            this.applyWeakTracking();
        }

        super.tick();
        this.noCulling = true;

        Vector3d motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            this.yRot = (float) (Math.atan2(motion.x, motion.z) * (180.0D / Math.PI));
            this.xRot = (float) (Math.atan2(motion.y, horizontal) * (180.0D / Math.PI));
        }

        if (!this.level.isClientSide && !this.exploded) {
            this.traveledDistance += this.position().distanceTo(previousPos);
            this.spawnFlightParticles();
            this.handleWater();

            if (this.traveledDistance >= MAX_TRAVEL_DISTANCE || this.tickCount >= MAX_LIFE - 1) {
                this.explodeAt(this.blockPosition());
            }
        }
    }

    private void applyWeakTracking() {
        Vector3d motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }

        LivingEntity target = this.resolveTarget(motion.normalize());
        if (target == null) {
            return;
        }

        this.homingTarget = target.getUUID();

        Vector3d targetPos = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D);
        Vector3d toTarget = targetPos.subtract(this.position());
        if (toTarget.lengthSqr() < 1.0E-4D) {
            return;
        }

        double speed = Math.max(0.8D, motion.length());
        Vector3d currentDir = motion.normalize();
        Vector3d desiredDir = toTarget.normalize();
        Vector3d newDir = currentDir.scale(1.0D - TRACK_STRENGTH).add(desiredDir.scale(TRACK_STRENGTH));
        if (newDir.lengthSqr() < 1.0E-6D) {
            return;
        }

        this.setDeltaMovement(newDir.normalize().scale(speed));
    }

    private LivingEntity resolveTarget(Vector3d forward) {
        LivingEntity thrower = this.getThrower();
        if (thrower == null) {
            return null;
        }

        AxisAlignedBB searchBox = this.getBoundingBox().inflate(TRACK_RANGE);
        List<LivingEntity> candidates = this.level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive()
                        && entity != thrower
                        && ModEntityPredicates.getEnemyFactions(thrower).test(entity)
        );

        if (candidates.isEmpty()) {
            return null;
        }

        if (this.homingTarget != null && this.level instanceof ServerWorld) {
            Entity tracked = ((ServerWorld) this.level).getEntity(this.homingTarget);
            if (tracked instanceof LivingEntity && tracked.isAlive()) {
                LivingEntity living = (LivingEntity) tracked;
                Vector3d toTracked = living.position().add(0.0D, living.getBbHeight() * 0.45D, 0.0D).subtract(this.position()).normalize();
                if (forward.dot(toTracked) >= TRACK_ANGLE_DOT) {
                    return living;
                }
            }
        }

        return candidates.stream()
                .filter(candidate -> {
                    Vector3d toCandidate = candidate.position()
                            .add(0.0D, candidate.getBbHeight() * 0.45D, 0.0D)
                            .subtract(this.position());
                    return toCandidate.lengthSqr() > 1.0E-4D && forward.dot(toCandidate.normalize()) >= TRACK_ANGLE_DOT;
                })
                .max(Comparator.comparingDouble(candidate -> this.getTargetScore(candidate, forward)))
                .orElse(null);
    }

    private double getTargetScore(LivingEntity candidate, Vector3d forward) {
        Vector3d toCandidate = candidate.position()
                .add(0.0D, candidate.getBbHeight() * 0.45D, 0.0D)
                .subtract(this.position());
        double distance = Math.sqrt(toCandidate.lengthSqr());
        double alignment = forward.dot(toCandidate.normalize());
        return alignment * 3.0D - distance * 0.05D;
    }

    private void onEntityImpactEvent(LivingEntity target) {
        this.explodeAt(target.blockPosition());
    }

    private void explodeAt(BlockPos hit) {
        if (this.exploded) {
            return;
        }

        this.exploded = true;
        ExplosionAbility explosion = this.createExplosion(
                this.getThrower(),
                this.level,
                hit.getX(),
                hit.getY(),
                hit.getZ(),
                EXPLOSION_RADIUS
        );
        explosion.setStaticDamage(DAMAGE);
        explosion.setHeightDifference(20);
        explosion.setFireAfterExplosion(true);
        explosion.disableExplosionKnockback();
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.doExplosion();
        this.remove();
    }

    private void handleWater() {
        if (!this.isEyeInFluid(FluidTags.WATER) || !CommonConfig.INSTANCE.getDestroyWater()) {
            return;
        }

        for (BlockPos blockPos : AbilityHelper.createFilledSphere(
                this.getCommandSenderWorld(),
                (int) this.getX(),
                (int) this.getY(),
                (int) this.getZ(),
                2,
                Blocks.AIR,
                GRIEF_RULE)) {
            SimpleParticleData data = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
            data.setLife(10);
            data.setSize(1.2F);
            WyHelper.spawnParticles(data, (ServerWorld) this.getCommandSenderWorld(),
                    blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D);
        }
    }

    private void spawnFlightParticles() {
        ServerWorld serverWorld = (ServerWorld) this.level;

        for (int i = 0; i < 8; i++) {
            SimpleParticleData mera = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MERA.get());
            mera.setLife(18);
            mera.setSize(2.0F);
            WyHelper.spawnParticles(mera, serverWorld,
                    this.getX() + (WyHelper.randomDouble() - 0.5D) * 0.9D,
                    this.getY() + (WyHelper.randomDouble() - 0.5D) * 0.9D,
                    this.getZ() + (WyHelper.randomDouble() - 0.5D) * 0.9D);
        }

        for (int i = 0; i < 4; i++) {
            SimpleParticleData smoke = new SimpleParticleData((ParticleType<?>) ModParticleTypes.MOKU.get());
            smoke.setLife(8);
            smoke.setSize(0.9F);
            WyHelper.spawnParticles(smoke, serverWorld,
                    this.getX() + (WyHelper.randomDouble() - 0.5D) * 0.6D,
                    this.getY() + (WyHelper.randomDouble() - 0.5D) * 0.6D,
                    this.getZ() + (WyHelper.randomDouble() - 0.5D) * 0.6D);
        }
    }
}
