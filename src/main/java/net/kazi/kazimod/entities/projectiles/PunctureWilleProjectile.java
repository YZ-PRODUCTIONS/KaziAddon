package net.kazi.kazimod.entities.projectiles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.Set;
import java.util.UUID;
import net.kazi.kazimod.abilities.OpeRework.PunctureWilleRework;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.protection.block.RestrictedBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class PunctureWilleProjectile extends AbilityProjectileEntity {

    private static final DataParameter<Float> LENGTH =
            EntityDataManager.defineId(PunctureWilleProjectile.class, DataSerializers.FLOAT);

    private static final float IMPACT_DAMAGE = 40.0F;
    private static final double HOLD_VERTICAL = 0.35D;
    private static final double HOLD_FORWARD = 1.8D;
    private static final double HOLD_SPACING = 1.1D;
    private static final double FLING_POWER = 3.3D;
    private static final double FLING_UP = 0.95D;
    private static final double MIN_HOLD_Y = 1.0D;

    private final Set<UUID> impaledTargets = new LinkedHashSet<>();
    private boolean launchedTargets;
    private LightningDischargeEntity discharge;
    private int nextBlockExplosionTick;
    private Vector3d lastCutPosition;

    public PunctureWilleProjectile(EntityType<? extends PunctureWilleProjectile> type, World world) {
        super(type, world);
    }

    public PunctureWilleProjectile(World world, LivingEntity thrower) {
        super((EntityType<? extends AbilityProjectileEntity>) KaziEntities.PUNCTURE_WILLE.get(), world, thrower, PunctureWilleRework.INSTANCE);
        this.setDamage(IMPACT_DAMAGE);
        this.setMaxLife(40);
        this.setBlocksAffectedLimit(100000);
        this.setHurtTime(0);
        this.setNoGravity(true);
        this.setPassThroughBlocks();
        this.setPassThroughEntities();
        this.setLength(8.5F);
        this.setUnavoidable();
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;

        if (!world.isClientSide && thrower != null) {
            this.discharge = new LightningDischargeEntity(thrower, thrower.getX(), thrower.getY() + 1.0D, thrower.getZ(), thrower.yRot, thrower.xRot);
            this.discharge.setAliveTicks(-1);
            this.discharge.setUpdateRate(4);
            this.discharge.setLightningLength(5.0F);
            this.discharge.setColor(new Color(0, 0, 0, 100));
            this.discharge.setOutlineColor(new Color(120, 220, 255));
            this.discharge.setRenderTransparent();
            this.discharge.setDetails(16);
            this.discharge.setDensity(14);
            this.discharge.setSize(0.8F);
            this.discharge.setSkipSegments(1);
            world.addFreshEntity(this.discharge);
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LENGTH, 8.5F);
    }

    @Override
    public void tick() {
        super.tick();
        this.noCulling = true;
        if (!this.level.isClientSide && this.discharge != null) {
            this.discharge.setPos(this.getX(), this.getY(), this.getZ());
        }

        steerTowardOwnerAim();

        Vector3d motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            float yaw = (float) (MathHelper.atan2(motion.x, motion.z) * (180.0D / Math.PI));
            float pitch = (float) (MathHelper.atan2(motion.y, horizontal) * (180.0D / Math.PI));
            this.yRotO = this.yRot;
            this.xRotO = this.xRot;
            this.yRot = yaw;
            this.xRot = pitch;
        }

        if (!this.level.isClientSide) {
            dragTargets();
            cutBlocksAroundProjectile();

            if (this.tickCount % 2 == 0 && this.level instanceof ServerWorld) {
                ServerWorld world = (ServerWorld) this.level;
                for (int i = 0; i < 8; i++) {
                    double ox = (this.random.nextDouble() - 0.5D) * 0.45D;
                    double oy = (this.random.nextDouble() - 0.5D) * 0.75D;
                    double oz = (this.random.nextDouble() - 0.5D) * 0.45D;
                    WyHelper.spawnParticles(ParticleTypes.END_ROD, world, this.getX() + ox, this.getY() + 0.5D + oy, this.getZ() + oz);
                }
            }

            this.setLength((float) Math.max(8.5D, Math.min(16.0D, this.tickCount * 0.22D + 8.5D)));
        }
    }

    private void steerTowardOwnerAim() {
        LivingEntity thrower = this.getThrower();
        if (thrower == null) {
            return;
        }

        Vector3d current = this.getDeltaMovement();
        double speed = current.length();
        if (speed <= 1.0E-6D) {
            return;
        }

        Vector3d desired = thrower.getLookAngle();
        if (desired.lengthSqr() <= 1.0E-6D) {
            return;
        }

        this.setDeltaMovement(desired.normalize().scale(speed));
        this.hurtMarked = true;
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        if (hitEntity == null || !hitEntity.isAlive() || hitEntity == this.getThrower()) {
            return;
        }

        Predicate<LivingEntity> validTarget = entity -> entity.isAlive() && entity != this.getThrower();
        if (ModEntityPredicates.getEnemyFactions(this.getThrower()) != null) {
            validTarget = validTarget.and(entity -> ModEntityPredicates.getEnemyFactions(this.getThrower()).test(entity));
        }

        if (!validTarget.test(hitEntity)) {
            return;
        }

        if (this.impaledTargets.add(hitEntity.getUUID())) {
            hitEntity.hurtTime = 0;
            hitEntity.invulnerableTime = 0;
            hitEntity.hurt(this.getDamageSource(), IMPACT_DAMAGE);
            this.level.playSound(null, this.blockPosition(), net.minecraft.util.SoundEvents.TRIDENT_HIT, SoundCategory.PLAYERS, 1.1F, 0.8F);
        }
    }

    private void onBlockImpactEvent(BlockPos hitPos) {
        if (!canDestroyAt(hitPos)) {
            this.remove();
            return;
        }

        if (this.tickCount < this.nextBlockExplosionTick) {
            return;
        }
        this.nextBlockExplosionTick = this.tickCount + 5;
        ExplosionAbility explosion = super.createExplosion(
                this.getThrower(),
                this.level,
                hitPos.getX(),
                hitPos.getY(),
                hitPos.getZ(),
                3.0F
        );
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.doExplosion();

        Vector3d motion = this.getDeltaMovement();
        Vector3d direction = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : this.getLookAngle().normalize();
        this.setPos(this.getX() + direction.x * 0.8D, this.getY() + direction.y * 0.8D, this.getZ() + direction.z * 0.8D);
    }

    private void cutBlocksAroundProjectile() {
        if (this.level.isClientSide) {
            return;
        }

        Vector3d start = this.lastCutPosition != null ? this.lastCutPosition : this.position();
        Vector3d end = this.position();
        Vector3d diff = end.subtract(start);
        double distance = diff.length();
        int steps = Math.max(1, (int) Math.ceil(distance / 0.5D));
        boolean destroyedAny = false;

        for (int step = 0; step <= steps; step++) {
            double t = steps == 0 ? 0.0D : (double) step / (double) steps;
            Vector3d sample = start.add(diff.scale(t));
            BlockPos center = new BlockPos(sample.x, sample.y, sample.z);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        if (!canDestroyAt(pos)) {
                            continue;
                        }

                        BlockState state = this.level.getBlockState(pos);
                        if (state.isAir() || state.getBlock() == Blocks.BEDROCK) {
                            continue;
                        }

                        this.level.destroyBlock(pos, false, this);
                        destroyedAny = true;
                    }
                }
            }
        }

        if (!destroyedAny) {
            BlockPos center = this.blockPosition();
            BlockState stateAtProjectile = this.level.getBlockState(center);
            if (!stateAtProjectile.isAir() && !canDestroyAt(center)) {
                this.remove();
            }
        }

        this.lastCutPosition = end;
    }

    private boolean canDestroyAt(BlockPos pos) {
        if (!CommonConfig.INSTANCE.isAbilityGriefingEnabled()) {
            return false;
        }

        ProtectedAreasData worldData = ProtectedAreasData.get(this.level);
        ProtectedArea area = worldData.getProtectedArea(pos.getX(), pos.getY(), pos.getZ());
        if (area != null && !area.canDestroyBlocks()) {
            return false;
        }

        BlockState state = this.level.getBlockState(pos);
        return !RestrictedBlockProtectionRule.INSTANCE.isBanned(state);
    }

    private void dragTargets() {
        Vector3d motion = this.getDeltaMovement();
        Vector3d dragDirection = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : this.getLookAngle().normalize();
        int index = 0;
        for (UUID targetId : new ArrayList<>(this.impaledTargets)) {
            LivingEntity target = findTarget(targetId);
            if (target == null || !target.isAlive()) {
                this.impaledTargets.remove(targetId);
                continue;
            }

            Vector3d holdPos = this.position()
                    .add(0.0D, HOLD_VERTICAL, 0.0D)
                    .add(dragDirection.scale(HOLD_FORWARD + index * HOLD_SPACING));
            target.setPos(holdPos.x, Math.max(MIN_HOLD_Y, holdPos.y), holdPos.z);
            target.setDeltaMovement(Vector3d.ZERO);
            target.hurtMarked = true;
            target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 4, 0, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.ANTI_KNOCKBACK.get(), 4, 0, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.GRABBED.get(), 4, 3, false, false));
            index++;
        }
    }

    private void flingTargets() {
        if (this.launchedTargets) {
            return;
        }
        this.launchedTargets = true;
        Vector3d motion = this.getDeltaMovement();
        Vector3d launchDirection = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : this.getLookAngle().normalize();
        for (UUID targetId : this.impaledTargets) {
            LivingEntity target = findTarget(targetId);
            if (target == null || !target.isAlive()) {
                continue;
            }

            Vector3d knockback = launchDirection.scale(FLING_POWER);
            AbilityHelper.setDeltaMovement(target, knockback.x, FLING_UP, knockback.z);
        }
    }

    private LivingEntity findTarget(UUID targetId) {
        AxisAlignedBB area = this.getBoundingBox().inflate(24.0D);
        for (LivingEntity target : this.level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.getUUID().equals(targetId))) {
            return target;
        }
        return null;
    }

    public float getLength() {
        return this.entityData.get(LENGTH);
    }

    private void setLength(float value) {
        this.entityData.set(LENGTH, value);
    }

    @Override
    public void remove() {
        LivingEntity thrower = this.getThrower();
        if (thrower != null) {
            PunctureWilleRework ability = getOwnerAbility(thrower);
            if (ability != null) {
                ability.clearProjectileState(thrower, this.getUUID());
            }
        }
        if (this.discharge != null) {
            this.discharge.setAliveTicks(0);
            this.discharge.remove();
            this.discharge = null;
        }
        flingTargets();
        super.remove();
    }

    private PunctureWilleRework getOwnerAbility(LivingEntity thrower) {
        if (thrower == null) {
            return null;
        }

        xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData props =
                xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability.get(thrower);
        if (props == null) {
            return null;
        }

        return (PunctureWilleRework) props.getEquippedAbility(PunctureWilleRework.INSTANCE);
    }
}
