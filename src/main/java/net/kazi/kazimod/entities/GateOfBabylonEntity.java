package net.kazi.kazimod.entities;

import net.kazi.kazimod.abilities.GoruRework.GateOfBabylonAbility;
import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility;
import net.kazi.kazimod.entities.projectiles.BabylonWeaponEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

/** One anchored visual entity owns the variant's single-shot gates. */
public final class GateOfBabylonEntity extends Entity {
    public static final int CHARGING = 0, FIRING = 1, FADING = 2, FADE_TICKS = 10;
    private static final DataParameter<CompoundNBT> STATE = EntityDataManager.defineId(GateOfBabylonEntity.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Float> PROGRESS = EntityDataManager.defineId(GateOfBabylonEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.defineId(GateOfBabylonEntity.class, DataSerializers.INT);
    private LivingEntity owner;
    private PlayerEntity currentTarget;
    private GateOfBabylonAbility ability;
    private long cachedLayoutSeed;
    private double[][] portalLayout;
    private long visualAimTick = Long.MIN_VALUE;
    private Vector3d previousVisualAim, currentVisualAim;

    public GateOfBabylonEntity(EntityType<? extends GateOfBabylonEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        setNoGravity(true);
    }

    public void begin(LivingEntity owner, GateOfBabylonAbility ability, int portalCount) {
        this.owner = owner; this.ability = ability;
        setPos(owner.getX(), owner.getY(), owner.getZ());
        CompoundNBT state = new CompoundNBT();
        state.putInt("owner", owner.getId());
        state.putInt("portalCount", portalCount);
        state.putLong("start", level.getGameTime());
        state.putLong("birth", level.getGameTime());
        state.putFloat("yaw", owner.yRot);
        state.putFloat("pitch", owner.xRot);
        state.putLong("layoutSeed", owner.getRandom().nextLong());
        entityData.set(STATE, state);
        if (!level.isClientSide) refreshTarget();
        bounds();
        level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundCategory.PLAYERS, .8F, 1.25F);
        level.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, .8F, .8F);
    }

    public int getPhase() { return entityData.get(STATE).getInt("phase"); }
    public boolean isCharging() { return getPhase() == CHARGING; }
    public boolean isFading() { return getPhase() == FADING; }
    public int getShotsFired() { return entityData.get(STATE).getInt("shots"); }
    public int getPortalCount() {
        int count = entityData.get(STATE).getInt("portalCount");
        return count == 75 || count == 150 ? count : BabylonVolleyPattern.PORTALS;
    }
    public double getVisualRadius() { return BabylonVolleyPattern.columns(getPortalCount()) * 2 + 5; }
    public float getCastYaw() { return entityData.get(STATE).getFloat("yaw"); }
    public Vector3d getCastDirection() {
        return Vector3d.directionFromRotation(entityData.get(STATE).getFloat("pitch"), getCastYaw());
    }
    public float phaseAge(float partial) { return Math.max(0, level.getGameTime() - entityData.get(STATE).getLong("start") + partial); }
    public float chargeAge(float partial) { return Math.min(GateOfBabylonAbility.CHARGE_TICKS, entityData.get(PROGRESS) * GateOfBabylonAbility.CHARGE_TICKS + partial); }
    /** Keeps ripple motion continuous across launch and cancellation packets. */
    public float visualAge(float partial) {
        return Math.max(0, level.getGameTime() - entityData.get(STATE).getLong("birth") + partial);
    }
    public float visualCastAge(float partial) {
        if (isFading()) return entityData.get(STATE).getFloat("fadeCastAge");
        if (isCharging()) return chargeAge(partial);
        // The server establishes firing tick zero at the first scheduled shot.
        return BabylonVolleyPattern.PREPARATION_TICKS + (getShotsFired() == 0 ? 0 : phaseAge(partial));
    }
    public int visualSeed(int portal) {
        long seed = entityData.get(STATE).getLong("layoutSeed");
        return (int) (seed ^ (seed >>> 32)) + portal * 0x9E3779B9;
    }

    /** Loaded weapons track the server-selected player, with smooth visual movement. */
    public Vector3d visualAim(float partial) {
        if (isFading()) {
            return currentVisualAim == null ? idleAim() : currentVisualAim;
        }
        long now = level.getGameTime();
        if (visualAimTick != now) {
            Entity target = level.getEntity(entityData.get(TARGET_ID));
            Vector3d next = target instanceof PlayerEntity && target.isAlive() && !target.removed
                    ? target.getBoundingBox().getCenter() : idleAim();
            previousVisualAim = currentVisualAim == null ? next : currentVisualAim;
            currentVisualAim = next;
            visualAimTick = now;
        }
        return previousVisualAim.add(currentVisualAim.subtract(previousVisualAim).scale(partial));
    }
    private Vector3d idleAim() {
        LivingEntity caster = owner != null ? owner : getCaster();
        if (caster == null) return position().add(getCastDirection().scale(GateOfBabylonAbility.RANGE));
        Vector3d eye = caster.getEyePosition(1);
        Vector3d aim = eye.add(caster.getLookAngle().scale(GateOfBabylonAbility.RANGE));
        RayTraceResult wall = level.clip(new RayTraceContext(eye, aim,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, caster));
        if (wall.getType() != RayTraceResult.Type.MISS) aim = wall.getLocation();
        double nearest = eye.distanceToSqr(aim);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(eye, aim).inflate(1), candidate -> EnkiduAbility.isEnemy(caster, candidate))) {
            java.util.Optional<Vector3d> hit = target.getBoundingBox().inflate(.2).clip(eye, aim);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < nearest) {
                nearest = eye.distanceToSqr(hit.get());
                aim = hit.get();
            }
        }
        return aim;
    }
    public void setChargeProgress(float value) { entityData.set(PROGRESS, MathHelper.clamp(value, 0, 1)); }
    public LivingEntity getCaster() {
        Entity entity = level.getEntity(entityData.get(STATE).getInt("owner"));
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    public Vector3d portalOffset(int index) {
        double yaw = Math.toRadians(getCastYaw()), c = Math.cos(yaw), s = Math.sin(yaw);
        long seed = entityData.get(STATE).getLong("layoutSeed");
        if (portalLayout == null || seed != cachedLayoutSeed || portalLayout.length != getPortalCount()) {
            portalLayout = BabylonVolleyPattern.layout(seed, getPortalCount());
            cachedLayoutSeed = seed;
        }
        double[] point = portalLayout[index];
        double x = point[0], z = point[2];
        return new Vector3d(x * c - z * s, point[1], x * s + z * c);
    }

    public void launch() {
        if (level.isClientSide || !isCharging()) return;
        setPhase(FIRING);
    }
    public void fade() {
        if (level.isClientSide || isFading()) return;
        setPhase(FADING);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundCategory.PLAYERS, .5F, 1.5F);
    }
    private void setPhase(int phase) {
        CompoundNBT state = entityData.get(STATE).copy();
        if (phase == FADING) state.putFloat("fadeCastAge", visualCastAge(0));
        state.putInt("phase", phase);
        state.putLong("start", level.getGameTime());
        entityData.set(STATE, state);
    }

    @Override public void tick() {
        super.tick();
        if (level.isClientSide) return;
        if (isFading()) { if (phaseAge(0) >= FADE_TICKS) remove(); return; }
        if (owner == null || !owner.isAlive() || owner.removed || owner.level != level || ability == null
                || !ability.ownsCast(this) || AbilityDataCapability.get(owner).getEquippedAbility(GateOfBabylonAbility.INSTANCE) != ability) {
            fade(); return;
        }
        // Gates remain anchored to the original cast position while the caster moves.
        refreshTarget();
        if (getPhase() != FIRING) return;
        int shot = getShotsFired();
        if (shot >= getPortalCount()) {
            if (phaseAge(0) >= BabylonVolleyPattern.shotTick(getPortalCount()) + 4) fade();
            return;
        }
        if (phaseAge(0) < BabylonVolleyPattern.shotTick(shot)) return;
        fire(shot);
        CompoundNBT state = entityData.get(STATE).copy();
        if (shot == 0) state.putLong("start", level.getGameTime());
        state.putInt("shots", shot + 1);
        entityData.set(STATE, state);
    }

    private void fire(int shot) {
        // Prefer hostile players; otherwise converge on the caster's current crosshair.
        Vector3d origin = position().add(portalOffset(BabylonVolleyPattern.portalForShot(shot, getPortalCount())));
        Vector3d aim = currentTarget == null ? idleAim() : currentTarget.getBoundingBox().getCenter();
        // Gates inside walls cannot emit through those walls.
        if (!level.noCollision(new AxisAlignedBB(origin, origin).inflate(.18))) return;
        BabylonWeaponEntity weapon = new BabylonWeaponEntity(owner, origin, aim, BabylonVolleyPattern.weaponForShot(shot));
        if (level.addFreshEntity(weapon)) {
            level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.TRIDENT_THROW, SoundCategory.PLAYERS, .65F, 1.05F + (shot % 4) * .12F);
        }
    }

    private void refreshTarget() {
        PlayerEntity nearest = null;
        double nearestDistance = GateOfBabylonAbility.RANGE * GateOfBabylonAbility.RANGE;
        Vector3d casterPosition = owner.position();
        for (PlayerEntity candidate : level.players()) {
            if (candidate.removed || !EnkiduAbility.isEnemy(owner, candidate)) continue;
            double distance = candidate.position().distanceToSqr(casterPosition);
            if (distance > nearestDistance) continue;
            if (nearest == null || distance < nearestDistance || candidate.getId() < nearest.getId()) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        currentTarget = nearest;
        entityData.set(TARGET_ID, nearest == null ? -1 : nearest.getId());
    }

    private void bounds() { setBoundingBox(new AxisAlignedBB(position(), position()).inflate(getVisualRadius())); }
    @Override protected void defineSynchedData() {
        entityData.define(STATE, new CompoundNBT());
        entityData.define(PROGRESS, 0F);
        entityData.define(TARGET_ID, -1);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
}
