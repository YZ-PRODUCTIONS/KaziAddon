package net.kazi.kazimod.abilities.Kyoka;

import java.util.List;
import java.util.UUID;
import net.kazi.kazimod.entities.GoryutenmetsuDragonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GoryutenmetsuAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "goryutenmetsu",
            new Pair[]{ImmutablePair.of("A supreme kido that conjures a spectral dragon, launches it straight into the sky, and then bends it down onto the aimed target in a catastrophic violet impact.", null)}
    );

    private static final float RANGE = 40.0F;
    private static final float CHARGE_TIME = 75.0F;
    private static final float COOLDOWN = 1000.0F;
    private static final float FAIL_COOLDOWN = 100.0F;
    private static final float DIVE_TIME = 60.0F;
    private static final float TOTAL_TIME = DIVE_TIME;
    private static final float IMPACT_DAMAGE = 150.0F;
    private static final float SPLASH_DAMAGE = 120.0F;
    private static final float IMPACT_RADIUS = 12.0F;
    private static final float EXPLOSION_SIZE = 10.0F;
    private static final double ASCENT_HEIGHT = 200.0D;
    private static final float DRAGON_SCALE = 1.4F;
    public static final AbilityCore<GoryutenmetsuAbility> INSTANCE;

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final Interval chargeVisualInterval = new Interval(2);
    private final Interval flightVisualInterval = new Interval(2);
    private UUID targetId;
    private Vector3d lockPos;
    private Vector3d launchPos;
    private Vector3d castFacing;
    private float castYaw;
    private GoryutenmetsuDragonEntity dragonEntity;
    private boolean impactResolved;
    private boolean diveStarted;

    public GoryutenmetsuAbility(AbilityCore<GoryutenmetsuAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging() || this.continuousComponent.isContinuous()) {
            return;
        }

        LivingEntity target = this.findTarget(entity);
        if (target == null) {
            return;
        }

        this.targetId = target.getUUID();
        this.lockPos = this.getTargetAnchor(target);
        this.chargeComponent.startCharging(entity, CHARGE_TIME);
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.impactResolved = false;
        this.diveStarted = false;
        this.launchPos = entity.position().add(0.0D, entity.getBbHeight() * 0.9D, 0.0D);
        this.castFacing = entity.getLookAngle();
        if (this.castFacing.lengthSqr() < 0.001D) {
            this.castFacing = new Vector3d(0.0D, 0.0D, 1.0D);
        }
        this.castYaw = this.computeYaw(this.castFacing);
        this.spawnDragon(entity, CHARGE_TIME + TOTAL_TIME + 30.0F);
        this.updateDragonPositionWithAngles(this.launchPos, this.castYaw, 90.0F, this.computeAscentRoll(), DRAGON_SCALE, 0.82F);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.8F, 1.25F);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !(entity.level instanceof ServerWorld)) {
            return;
        }

        LivingEntity target = this.getTarget(entity);
        if (target == null) {
            this.stopDragon();
            this.chargeComponent.stopCharging(entity);
            return;
        }

        ServerWorld world = (ServerWorld) entity.level;
        this.lockPos = this.getTargetAnchor(target);
        this.launchPos = entity.position().add(0.0D, entity.getBbHeight() * 0.9D, 0.0D);
        Vector3d desired = this.launchPos.add(0.0D, ASCENT_HEIGHT * this.chargeComponent.getChargePercentage(), 0.0D);
        this.updateDragonPositionWithAngles(desired, this.castYaw, 90.0F, this.computeAscentRoll(), DRAGON_SCALE + this.chargeComponent.getChargePercentage() * 0.45F, 0.78F + this.chargeComponent.getChargePercentage() * 0.18F);

        if (this.chargeVisualInterval.canTick()) {
            this.spawnChargeParticles(world, entity, this.chargeComponent.getChargePercentage());
            if (this.dragonEntity != null) {
                this.spawnAscentParticles(world, this.dragonEntity.position());
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.getTarget(entity) == null) {
                this.stopDragon();
                this.cooldownComponent.startCooldown(entity, FAIL_COOLDOWN);
                this.clearState();
                return;
            }

            entity.level.playSound((PlayerEntity) null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.3F, 0.65F);
            this.continuousComponent.startContinuity(entity, TOTAL_TIME);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            Vector3d startPos = this.dragonEntity != null ? this.dragonEntity.position() : entity.position().add(0.0D, entity.getBbHeight() * 0.9D + ASCENT_HEIGHT, 0.0D);
            this.stopDragon();
            this.dragonEntity = GoryutenmetsuDragonEntity.create(
                    entity.level,
                    startPos.x,
                    startPos.y,
                    startPos.z,
                    DRAGON_SCALE + 0.6F,
                    0.96F,
                    MathHelper.ceil(TOTAL_TIME + 20.0F)
            );
            entity.level.addFreshEntity(this.dragonEntity);
            Vector3d initialDiveFacing = this.lockPos != null ? this.lockPos.subtract(startPos) : new Vector3d(0.0D, -1.0D, 0.0D);
            if (initialDiveFacing.lengthSqr() < 0.001D) {
                initialDiveFacing = new Vector3d(0.0D, -1.0D, 0.0D);
            }
            this.updateDragonLookAt(startPos, this.lockPos != null ? this.lockPos : startPos.add(0.0D, -1.0D, 0.0D), DRAGON_SCALE + 0.6F, 0.96F);
            this.diveStarted = true;
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 1.2F, 1.35F);
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !(entity.level instanceof ServerWorld)) {
            return;
        }

        LivingEntity target = this.getTarget(entity);
        if (target == null) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        ServerWorld world = (ServerWorld) entity.level;
        this.lockPos = this.getTargetAnchor(target);
        float time = this.continuousComponent.getContinueTime();
        if (this.dragonEntity == null || !this.dragonEntity.isAlive()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        Vector3d desired = this.lockPos.add(0.0D, target.getBbHeight() * 0.25D, 0.0D);
        Vector3d motion = desired.subtract(this.dragonEntity.position());
        motion = motion.lengthSqr() > 0.001D ? motion.normalize().scale(Math.min(6.8D, Math.max(4.6D, motion.length()))) : Vector3d.ZERO;
        if (!this.diveStarted) {
            this.updateDragonLookAt(this.dragonEntity.position(), desired, DRAGON_SCALE + 0.7F, 1.0F);
            this.diveStarted = true;
        }
        this.moveDragonByMotion(motion);
        this.updateDragonLookAt(this.dragonEntity.position(), desired, DRAGON_SCALE + 0.7F, 1.0F);
        if (this.flightVisualInterval.canTick()) {
            this.spawnDiveParticles(world, this.dragonEntity.position(), motion);
        }

        if (!this.impactResolved && this.dragonEntity.distanceToSqr(desired.x, desired.y, desired.z) <= 9.0D) {
            this.resolveImpact(entity, world, this.dragonEntity.position());
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.stopDragon();
        this.cooldownComponent.startCooldown(entity, this.impactResolved ? COOLDOWN : FAIL_COOLDOWN);
        this.clearState();
    }

    private LivingEntity findTarget(LivingEntity entity) {
        RayTraceResult trace = WyHelper.rayTraceBlocksAndEntities(entity, RANGE);
        if (trace instanceof EntityRayTraceResult) {
            Entity hit = ((EntityRayTraceResult) trace).getEntity();
            if (hit instanceof LivingEntity && hit != entity) {
                return (LivingEntity) hit;
            }
        }

        return null;
    }

    private LivingEntity getTarget(LivingEntity entity) {
        if (!(entity.level instanceof ServerWorld) || this.targetId == null) {
            return null;
        }

        Entity found = ((ServerWorld) entity.level).getEntity(this.targetId);
        if (!(found instanceof LivingEntity)) {
            return null;
        }

        LivingEntity target = (LivingEntity) found;
        return target.isAlive() ? target : null;
    }

    private Vector3d getTargetAnchor(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private void spawnDragon(LivingEntity entity, float lifeTicks) {
        if (entity.level.isClientSide) {
            return;
        }

        this.stopDragon();
        this.dragonEntity = GoryutenmetsuDragonEntity.create(
                entity.level,
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.8D,
                entity.getZ(),
                DRAGON_SCALE,
                0.8F,
                MathHelper.ceil(lifeTicks)
        );
        entity.level.addFreshEntity(this.dragonEntity);
    }

    private Vector3d moveDragonToward(Vector3d desired, double speed) {
        if (this.dragonEntity == null) {
            return Vector3d.ZERO;
        }

        Vector3d current = this.dragonEntity.position();
        Vector3d delta = desired.subtract(current);
        if (delta.lengthSqr() < 0.001D) {
            this.dragonEntity.setDeltaMovement(Vector3d.ZERO);
            return Vector3d.ZERO;
        }

        Vector3d motion = delta.normalize().scale(Math.min(speed, delta.length()));
        this.moveDragonByMotion(motion);
        return motion;
    }

    private void moveDragonByMotion(Vector3d motion) {
        if (this.dragonEntity == null) {
            return;
        }

        Vector3d current = this.dragonEntity.position();
        this.dragonEntity.setDeltaMovement(motion);
        this.dragonEntity.setPos(current.x + motion.x, current.y + motion.y, current.z + motion.z);
    }

    private void updateDragonFlight(Vector3d motion, float scale, float alpha) {
        if (this.dragonEntity == null) {
            return;
        }

        Vector3d facing = motion.lengthSqr() > 0.001D ? motion : new Vector3d(0.0D, 1.0D, 0.0D);
        this.updateDragonPosition(this.dragonEntity.position(), facing, scale, alpha);
    }

    private void updateDragonLookAt(Vector3d pos, Vector3d target, float scale, float alpha) {
        if (this.dragonEntity == null) {
            return;
        }

        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(-dx, dz) * 57.29577951308232);
        float pitch = (float) (Math.atan2(dy, horiz) * 57.29577951308232);
        this.updateDragonPositionWithAngles(pos, yaw, pitch, 0.0F, scale, alpha);
    }

    private void updateDragonPosition(Vector3d pos, Vector3d facing, float scale, float alpha) {
        if (this.dragonEntity == null) {
            return;
        }

        float yaw = this.computeYaw(facing);
        float pitch = this.computePitch(facing);
        this.updateDragonPositionWithAngles(pos, yaw, pitch, 0.0F, scale, alpha);
    }

    private float computeYaw(Vector3d facing) {
        return (float) (Math.atan2(-facing.x, facing.z) * 57.29577951308232);
    }

    private float computePitch(Vector3d facing) {
        double horiz = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
        return (float) (Math.atan2(facing.y, horiz) * 57.29577951308232);
    }

    private float computeAscentRoll() {
        if (this.castFacing == null) {
            return 0.0F;
        }

        if (Math.abs(this.castFacing.x) > Math.abs(this.castFacing.z)) {
            return this.castFacing.x > 0.0D ? 90.0F : -90.0F;
        }

        return 0.0F;
    }

    private void updateDragonPositionWithAngles(Vector3d pos, float yaw, float pitch, float roll, float scale, float alpha) {
        if (this.dragonEntity == null) {
            return;
        }

        this.dragonEntity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
        this.dragonEntity.setRenderYaw(yaw);
        this.dragonEntity.setRenderPitch(pitch);
        this.dragonEntity.setRenderRoll(roll);
        this.dragonEntity.setScale(scale);
        this.dragonEntity.setAlpha(alpha);
    }

    private void stopDragon() {
        if (this.dragonEntity != null) {
            this.dragonEntity.remove();
            this.dragonEntity = null;
        }
    }

    private void resolveImpact(LivingEntity entity, ServerWorld world, Vector3d center) {
        this.impactResolved = true;
        if (this.dragonEntity != null) {
            this.dragonEntity.setPos(center.x, center.y, center.z);
        }

        world.explode(entity, center.x, center.y, center.z, EXPLOSION_SIZE, Explosion.Mode.BREAK);

        AxisAlignedBB area = new AxisAlignedBB(
                center.x - IMPACT_RADIUS, center.y - IMPACT_RADIUS, center.z - IMPACT_RADIUS,
                center.x + IMPACT_RADIUS, center.y + IMPACT_RADIUS, center.z + IMPACT_RADIUS
        );
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area, living -> living != entity && living.isAlive());
        LivingEntity primary = this.getTarget(entity);
        AbilityDamageSource source = (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
        source.setInternal();
        source.setUnavoidable();
        source.markIndirectDamage();

        for (LivingEntity target : targets) {
            double distance = target.position().distanceTo(center);
            if (distance > IMPACT_RADIUS + target.getBbWidth()) {
                continue;
            }

            float damage = target == primary ? IMPACT_DAMAGE : SPLASH_DAMAGE * (float) Math.max(0.35D, 1.0D - distance / (IMPACT_RADIUS + 0.5D));
            this.dealDamageComponent.hurtTarget(entity, target, damage, source);
            target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 20, 1, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 30, 0, false, false));
            Vector3d knockback = target.position().subtract(center).normalize().scale(1.25D);
            AbilityHelper.setDeltaMovement(target, knockback.x, 0.55D, knockback.z);
        }

        world.playSound(null, entity.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 1.6F, 0.72F);
        world.playSound(null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.8F, 0.88F);
        world.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.4F, 0.65F);
        this.spawnImpactParticles(world, center);
    }

    private void spawnChargeParticles(ServerWorld world, LivingEntity entity, float progress) {
        Vector3d center = entity.position().add(0.0D, entity.getBbHeight() * 0.6D, 0.0D);
        double radius = 1.2D + progress * 2.8D;
        int points = 18;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points + entity.tickCount * 0.12D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + Math.sin(angle * 1.6D + entity.tickCount * 0.08D) * 0.9D + progress * 2.5D;
            world.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.01D);
        }

        world.sendParticles(ParticleTypes.WITCH, center.x, center.y + 1.0D + progress * 2.0D, center.z, 6, 0.5D + progress, 0.8D, 0.5D + progress, 0.02D);
        world.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.4D, center.z, 2, 0.25D, 0.25D, 0.25D, 0.0D);
    }

    private void spawnAscentParticles(ServerWorld world, Vector3d center) {
        world.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 20, 1.2D, 1.8D, 1.2D, 0.04D);
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y - 1.2D, center.z, 14, 0.9D, 0.4D, 0.9D, 0.01D);
    }

    private void spawnDiveParticles(ServerWorld world, Vector3d center, Vector3d motion) {
        Vector3d backward = motion.lengthSqr() > 0.01D ? motion.normalize().scale(-1.3D) : new Vector3d(0.0D, 0.0D, 0.0D);
        world.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 12, 0.8D, 1.0D, 0.8D, 0.02D);
        world.sendParticles(ParticleTypes.END_ROD, center.x + backward.x, center.y + backward.y, center.z + backward.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
    }

    private void spawnImpactParticles(ServerWorld world, Vector3d center) {
        world.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y + 1.0D, center.z, 120, 2.8D, 3.4D, 2.8D, 0.04D);
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.6D, center.z, 70, 2.2D, 1.2D, 2.2D, 0.03D);
        world.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.4D, center.z, 40, 1.6D, 1.6D, 1.6D, 0.06D);
    }

    private void clearState() {
        this.targetId = null;
        this.lockPos = null;
        this.launchPos = null;
        this.castFacing = null;
        this.castYaw = 0.0F;
        this.impactResolved = false;
        this.diveStarted = false;
        this.stopDragon();
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Hado 99: Goryutenmetsu", AbilityCategory.DEVIL_FRUITS, GoryutenmetsuAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(TOTAL_TIME),
                        CooldownComponent.getTooltip(COOLDOWN),
                        RangeComponent.getTooltip(IMPACT_RADIUS, RangeType.AOE),
                        DealDamageComponent.getTooltip(IMPACT_DAMAGE)
                })
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INDIRECT, SourceType.INTERNAL})
                .setUnlockCheck(GoryutenmetsuAbility::canUnlock)
                .build();
    }
}
