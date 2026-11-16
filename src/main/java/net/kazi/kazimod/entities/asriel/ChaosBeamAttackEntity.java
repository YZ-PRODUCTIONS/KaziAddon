package net.kazi.kazimod.entities.asriel;

import net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegation;

import java.util.UUID;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamDamage;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamPattern;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerAbility;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

/** One finite, server-owned field: merging hand orbs followed by a widening rainbow cone. */
public final class ChaosBeamAttackEntity extends Entity {
    private static final String GRAVITY_LOCK_TAG = "kazimodChaosBeamGravityLock";
    private static final DataParameter<CompoundNBT> STATE = EntityDataManager.defineId(ChaosBeamAttackEntity.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Boolean> DARK = EntityDataManager.defineId(ChaosBeamAttackEntity.class, DataSerializers.BOOLEAN);
    private UUID ownerId;
    private LivingEntity lockedOwner;
    private boolean expiredAfterLoad;
    private int lastDamagePulse = -1;

    public ChaosBeamAttackEntity(EntityType<? extends ChaosBeamAttackEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        setNoGravity(true);
    }

    public static ChaosBeamAttackEntity spawn(LivingEntity owner) {
        if (!(owner.level instanceof ServerWorld)) return null;
        ChaosBeamAttackEntity field = new ChaosBeamAttackEntity(KaziEntities.CHAOS_BEAM.get(), owner.level);
        field.ownerId = owner.getUUID();
        CompoundNBT state = new CompoundNBT();
        state.putLong("start", owner.level.getGameTime());
        state.putInt("caster", owner.getId());
        state.putDouble("yaw", owner.yRot);
        state.putDouble("pitch", owner.xRot);
        state.putDouble("anchorX", owner.getX());
        state.putDouble("anchorY", owner.getY());
        state.putDouble("anchorZ", owner.getZ());
        Vector3d direction = owner.getLookAngle().normalize();
        state.putDouble("dx", direction.x); state.putDouble("dy", direction.y); state.putDouble("dz", direction.z);
        field.entityData.set(STATE, state);
        field.updatePose(owner);
        field.entityData.set(DARK, YagiAwakeningSupport.enhanced(owner));
        if (!owner.level.addFreshEntity(field)) return null;
        field.lockedOwner = owner;
        field.lockMovement(owner);
        return field;
    }

    private boolean finalePlayed;

    public void finishFiring(LivingEntity owner) {
        if (finalePlayed || removed || level.isClientSide || !owner.isAlive() || !isFiring()
                || getVisualAge(0) < ChaosBeamPattern.FIRE_TICKS - 1) return;
        finalePlayed = true;
        net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.start(owner, level.getGameTime(),
                net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType.CHAOS_BEAM_FINALE);
    }

    public void beginBeam() {
        if (level.isClientSide || removed || isFiring()) return;
        CompoundNBT state = entityData.get(STATE).copy();
        state.putBoolean("firing", true);
        state.putLong("fireStart", level.getGameTime());
        entityData.set(STATE, state);
    }

    public boolean isFiring() { return entityData.get(STATE).getBoolean("firing"); }
    public boolean hasDarkPalette() { return entityData.get(DARK); }
    public boolean isInitialized() { return entityData.get(STATE).contains("start"); }
    public int getCasterId() { return entityData.get(STATE).getInt("caster"); }
    public boolean hasActiveAimLock() {
        return !removed && isInitialized()
                && getVisualAge(0) < (isFiring() ? ChaosBeamPattern.FIRE_TICKS : ChaosBeamPattern.CHARGE_TICKS + 5);
    }
    /** Shared by the server and the caster's client, including interpolation between frames. */
    public void lockAim(LivingEntity owner) {
        if (!hasActiveAimLock() || owner.level != level || owner.getId() != getCasterId()) return;
        float yaw = (float) entityData.get(STATE).getDouble("yaw");
        owner.yRot = owner.yRotO = owner.yHeadRot = owner.yHeadRotO = owner.yBodyRot = owner.yBodyRotO = yaw;
        owner.xRot = owner.xRotO = (float) entityData.get(STATE).getDouble("pitch");
    }
    /** Pin the caster on both sides so local movement never fights server position corrections. */
    public void lockMovement(LivingEntity owner) {
        if (!hasActiveAimLock() || owner.level != level || owner.getId() != getCasterId()
                || !owner.isAlive() || owner.isSpectator()) return;
        if (owner.isPassenger()) owner.stopRiding();
        if (!level.isClientSide && !owner.isNoGravity()) {
            owner.getPersistentData().putInt(GRAVITY_LOCK_TAG, getId());
            owner.setNoGravity(true);
        }
        owner.setJumping(false);
        owner.setSprinting(false);
        owner.xxa = owner.yya = owner.zza = 0;
        if (!level.isClientSide && owner.getDeltaMovement().lengthSqr() > 0) owner.hurtMarked = true;
        owner.setDeltaMovement(Vector3d.ZERO);
        owner.fallDistance = 0;
        CompoundNBT state = entityData.get(STATE);
        double x = state.getDouble("anchorX"), y = state.getDouble("anchorY"), z = state.getDouble("anchorZ");
        if (level.isClientSide) {
            owner.setPos(x, y, z);
            owner.xo = owner.xOld = x;
            owner.yo = owner.yOld = y;
            owner.zo = owner.zOld = z;
        } else if (owner.position().distanceToSqr(x, y, z) > 0.000001D) {
            // Resync only when externally displaced, rather than teleporting every server tick.
            owner.teleportTo(x, y, z);
        }
    }

    /** Also used at login to recover a lock saved during an interrupted server shutdown. */
    public static void clearMovementLock(LivingEntity owner) {
        if (!owner.level.isClientSide && owner.getPersistentData().contains(GRAVITY_LOCK_TAG)) {
            owner.setNoGravity(false);
            owner.getPersistentData().remove(GRAVITY_LOCK_TAG);
        }
    }

    @Override public void remove(boolean keepData) {
        if (lockedOwner != null && lockedOwner.getPersistentData().getInt(GRAVITY_LOCK_TAG) == getId())
            clearMovementLock(lockedOwner);
        lockedOwner = null;
        super.remove(keepData);
    }
    public double getLength() { return entityData.get(STATE).getDouble("length"); }
    public float getVisualAge(float partial) {
        return Math.max(0, level.getGameTime() - entityData.get(STATE).getLong(isFiring() ? "fireStart" : "start")) + partial;
    }
    public Vector3d getAttackDirection() {
        CompoundNBT state = entityData.get(STATE);
        return new Vector3d(state.getDouble("dx"), state.getDouble("dy"), state.getDouble("dz"));
    }

    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerWorld)) return;
        Entity entity = ownerId == null ? null : ((ServerWorld) level).getEntity(ownerId);
        if (expiredAfterLoad || !(entity instanceof LivingEntity) || !entity.isAlive() || entity.removed
                || entity.isSpectator()) { remove(); return; }
        LivingEntity owner = (LivingEntity) entity;
        IAbilityData data = AbilityDataCapability.get(owner);
        ShockerBreakerAbility ability = data == null ? null : data.getEquippedAbility(ShockerBreakerAbility.INSTANCE);
        int age = (int) getVisualAge(0);
        if (ability == null || !ability.ownsChaosBeam(this)) { remove(); return; }
        if (age >= (isFiring() ? ChaosBeamPattern.FIRE_TICKS : ChaosBeamPattern.CHARGE_TICKS + 5)) {
            finishFiring(owner);
            remove();
            return;
        }
        lockAim(owner);
        lockMovement(owner);
        updatePose(owner);
        entityData.set(DARK, YagiAwakeningSupport.enhanced(owner));
        if (isFiring()) {
            double length = getLength();
            AxisAlignedBB beamBounds = new AxisAlignedBB(position(),
                    position().add(getAttackDirection().scale(length)))
                    .inflate(ChaosBeamPattern.radiusAt(length, age));
            if (ImagineBreakerNegation.negateBeam(this, owner, ShockerBreakerAbility.INSTANCE,
                    beamBounds, point -> closestBeamPoint(point, length, age))) return;
        }
        int pulse = age / ChaosBeamPattern.DAMAGE_INTERVAL;
        if (isFiring() && pulse > lastDamagePulse) {
            lastDamagePulse = pulse;
            damage(owner, age);
        }
    }

    /** Project onto the finite cone, rather than using its much wider end radius at the muzzle. */
    private Vector3d closestBeamPoint(Vector3d point, double length, float age) {
        if (length <= 0) return null;
        Vector3d direction = getAttackDirection().normalize();
        Vector3d relative = point.subtract(position());
        double along = relative.dot(direction);
        Vector3d radial = relative.subtract(direction.scale(along));
        double distance = radial.length();
        double startRadius = ChaosBeamPattern.CORE_RADIUS;
        double endRadius = ChaosBeamPattern.radiusAt(length, age);
        if (along >= 0 && along <= length && distance <= ChaosBeamPattern.radiusAt(along, age)) return point;
        // In the axial cross-section, the side is a sloped segment and both caps are vertical segments.
        double bestAlong = 0, bestRadius = Math.min(distance, startRadius);
        double best = along * along + (distance - bestRadius) * (distance - bestRadius);
        double endCapRadius = Math.min(distance, endRadius);
        double endDistance = (along - length) * (along - length)
                + (distance - endCapRadius) * (distance - endCapRadius);
        if (endDistance < best) {
            best = endDistance;
            bestAlong = length;
            bestRadius = endCapRadius;
        }
        double slope = endRadius - startRadius;
        double denominator = length * length + slope * slope;
        double t = denominator <= 1.0E-20D ? 0
                : Math.max(0, Math.min(1, (along * length + (distance - startRadius) * slope) / denominator));
        double sideAlong = t * length, sideRadius = startRadius + t * slope;
        double sideDistance = (along - sideAlong) * (along - sideAlong)
                + (distance - sideRadius) * (distance - sideRadius);
        if (sideDistance < best) {
            bestAlong = sideAlong;
            bestRadius = sideRadius;
        }
        return position().add(direction.scale(bestAlong))
                .add(distance > 1.0E-10D ? radial.scale(bestRadius / distance) : Vector3d.ZERO);
    }

    private void updatePose(LivingEntity owner) {
        // Keep the cast direction and anchored caster position through charge and firing.
        Vector3d direction = getAttackDirection();
        Vector3d eye = owner.getEyePosition(1);
        double reach = 1.8D + 4.2D * (isFiring() ? 1D : ChaosBeamPattern.smooth((getVisualAge(0) - 12F) / 23F));
        Vector3d origin = eye.add(direction.scale(reach)).add(0, -.35D, 0);
        RayTraceResult muzzle = level.clip(new RayTraceContext(eye, origin,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, owner));
        if (muzzle.getType() != RayTraceResult.Type.MISS) origin = muzzle.getLocation().subtract(direction.scale(.05D));
        setPos(origin.x, origin.y, origin.z);
        double length = ChaosBeamPattern.RANGE;
        // Stop before unloaded terrain; this cosmetic attack never loads chunks or changes blocks.
        for (double step = 0; step <= length; step += 2D) {
            if (!level.hasChunkAt(new BlockPos(origin.add(direction.scale(step))))) { length = Math.max(0, step - 2D); break; }
        }
        RayTraceResult hit = level.clip(new RayTraceContext(origin, origin.add(direction.scale(length)),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, owner));
        if (hit.getType() != RayTraceResult.Type.MISS) length = origin.distanceTo(hit.getLocation());
        CompoundNBT state = entityData.get(STATE).copy();
        state.putDouble("length", length);
        if (!state.equals(entityData.get(STATE))) entityData.set(STATE, state);
    }

    private void damage(LivingEntity owner, int age) {
        Vector3d direction = getAttackDirection(), end = position().add(direction.scale(getLength()));
        double radius = ChaosBeamPattern.radiusAt(getLength(), age);
        AxisAlignedBB bounds = new AxisAlignedBB(position(), end).inflate(radius + 1D);
        int checked = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (++checked > 128) break;
            if (!YagiAwakeningSupport.isEnemy(owner, target)) continue;
            Vector3d center = target.getBoundingBox().getCenter(), relative = center.subtract(position());
            double along = relative.dot(direction);
            double radial = Math.max(0D, relative.lengthSqr() - along * along);
            if (!ChaosBeamPattern.contains(along, radial, target.getBbWidth() * .5D, getLength(), age)
                    || !YagiAwakeningSupport.clearPath(owner, position(), center)) continue;
            ChaosBeamDamage.hurt(owner, target);
        }
    }

    @Override protected void defineSynchedData() { entityData.define(STATE, new CompoundNBT()); entityData.define(DARK, false); }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { expiredAfterLoad = true; }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        double renderRange = ChaosBeamPattern.RANGE + ChaosBeamPattern.MAX_RADIUS;
        return distance < renderRange * renderRange;
    }
}
