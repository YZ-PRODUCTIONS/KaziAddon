package net.kazi.kazimod.entities.asriel;

import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;
import net.kazi.kazimod.abilities.YagiAwakening.AngelOfDeathPattern;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterAbility;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterPattern;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberAbility;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

/** Finite fruit projectiles: curved Angel of death beams and straight, delayed-fan Buster bolts. */
public final class AsrielChaosProjectile extends AbilityProjectileEntity {
    public static final int BULLET = 0;
    public static final int BUSTER = 1;
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(AsrielChaosProjectile.class, DataSerializers.INT);
    private static final DataParameter<Integer> TINT = EntityDataManager.defineId(AsrielChaosProjectile.class, DataSerializers.INT);
    private static final DataParameter<CompoundNBT> FLIGHT = EntityDataManager.defineId(AsrielChaosProjectile.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Boolean> DARK_PALETTE = EntityDataManager.defineId(AsrielChaosProjectile.class, DataSerializers.BOOLEAN);
    private UUID targetUuid;
    private boolean expiredAfterLoad;
    private boolean fanned;
    private boolean angelTargetChecked;
    private final AngelOfDeathPattern.Guidance angelGuidance = new AngelOfDeathPattern.Guidance();
    private final Deque<Vector3d> angelTrail = new ArrayDeque<>();

    public AsrielChaosProjectile(EntityType<? extends AsrielChaosProjectile> type, World world) {
        super(type, world);
        configure(BULLET);
    }

    public AsrielChaosProjectile(LivingEntity owner, Vector3d origin, Vector3d direction, int kind, int tint) {
        this(owner, origin, direction, kind, tint, 0.0D);
    }

    public AsrielChaosProjectile(LivingEntity owner, Vector3d origin, Vector3d direction, int kind, int tint, double fanAngle) {
        super(KaziEntities.ASRIEL_CHAOS_PROJECTILE.get(), owner.level, owner,
                kind == BUSTER ? ChaosBusterAbility.INSTANCE : ChaosSaberAbility.INSTANCE);
        configure(kind);
        entityData.set(KIND, kind);
        entityData.set(TINT, Math.floorMod(tint, 7));
        entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(owner));
        setPos(origin.x, origin.y, origin.z);
        setDamage(kind == BUSTER ? 4.0F : 8.0F);
        Vector3d forward = direction.normalize();
        setDeltaMovement(forward.scale(kind == BUSTER ? ChaosBusterPattern.BOLT_SPEED : AngelOfDeathPattern.SPEED));
        setMaxLife(kind == BUSTER ? 24 : AngelOfDeathPattern.MAX_LIFE);
        if (kind == BUSTER) {
            Vector3d right = new Vector3d(forward.z, 0.0D, -forward.x);
            if (right.lengthSqr() < 0.0001D) {
                double yaw = Math.toRadians(owner.yRot);
                right = new Vector3d(Math.cos(yaw), 0.0D, Math.sin(yaw));
            } else right = right.normalize();
            Vector3d fanDirection = forward.scale(Math.cos(fanAngle)).add(right.scale(Math.sin(fanAngle))).normalize();
            CompoundNBT flight = new CompoundNBT();
            flight.putFloat("angle", (float) fanAngle);
            flight.putDouble("x", fanDirection.x); flight.putDouble("y", fanDirection.y); flight.putDouble("z", fanDirection.z);
            flight.putLong("start", owner.level.getGameTime());
            entityData.set(FLIGHT, flight);
        }
        if (kind == BULLET) {
            setAngelAim(forward, origin.add(forward.scale(48)));
            angelTrail.addFirst(origin);
        }
    }

    /** The hand's aim is separate from the fan's outward initial velocity. */
    public void setAngelAim(Vector3d forward, Vector3d target) {
        if (getKind() != BULLET) return;
        Vector3d aim = forward.normalize();
        CompoundNBT flight = new CompoundNBT();
        flight.putLong("start", level.getGameTime());
        flight.putDouble("originX", getX()); flight.putDouble("originY", getY()); flight.putDouble("originZ", getZ());
        flight.putDouble("aimX", aim.x); flight.putDouble("aimY", aim.y); flight.putDouble("aimZ", aim.z);
        flight.putDouble("targetX", target.x); flight.putDouble("targetY", target.y); flight.putDouble("targetZ", target.z);
        entityData.set(FLIGHT, flight);
    }

    private void configure(int kind) {
        setGravity(0);
        setNoGravity(true);
        setMaxLife(80);
        setEntityCollisionSize(kind == BUSTER ? 1.1D : 0.55D * 2.0D);
        setBlockCollisionSize(kind == BUSTER ? 0.5D : 0.25D * 2.0D);
        setProjectile();
    }

    @Override public void tick() {
        if (getKind() == BUSTER && !fanned && getVisualAge(0) >= 2.0F) {
            CompoundNBT flight = entityData.get(FLIGHT);
            Vector3d direction = new Vector3d(flight.getDouble("x"), flight.getDouble("y"), flight.getDouble("z"));
            if (direction.lengthSqr() > 0.0001D) {
                setDeltaMovement(direction.normalize().scale(ChaosBusterPattern.BOLT_SPEED));
                if (!level.isClientSide) hasImpulse = true;
            }
            fanned = true;
        }
        if (!level.isClientSide) {
            LivingEntity owner = getThrower();
            if (expiredAfterLoad || owner == null || !owner.isAlive() || owner.removed || owner.level != level
                    || !level.hasChunkAt(new BlockPos(position().add(getDeltaMovement())))) {
                remove();
                return;
            }
            if (getKind() == BULLET) steerAngel(owner);
            entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(owner));
        }
        Vector3d flight = getDeltaMovement();
        super.tick();
        // Both attacks keep their authored speed; the Angel helper changes direction, never drag.
        if (!removed) {
            setDeltaMovement(flight);
            if (getKind() == BULLET) recordAngelTrail();
        }
    }

    private void steerAngel(LivingEntity owner) {
        float age = getVisualAge(0);
        if (age < AngelOfDeathPattern.ASSIST_DELAY_TICKS || angelGuidance.isComplete()) return;
        CompoundNBT flight = entityData.get(FLIGHT);
        Vector3d origin = new Vector3d(flight.getDouble("originX"), flight.getDouble("originY"), flight.getDouble("originZ"));
        Vector3d aim = new Vector3d(flight.getDouble("aimX"), flight.getDouble("aimY"), flight.getDouble("aimZ"));
        if (!angelTargetChecked) {
            angelTargetChecked = true;
            double bestAlignment = .55D, bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : YagiAwakeningSupport.targets(owner, 48.0D, 16)) {
                Vector3d point = candidate.getBoundingBox().getCenter();
                Vector3d offset = point.subtract(origin);
                double distance = offset.lengthSqr(), alignment = offset.normalize().dot(aim);
                if (distance > 48 * 48 || !YagiAwakeningSupport.clearPath(owner, position(), point)) continue;
                if (alignment > bestAlignment || alignment == bestAlignment && distance < bestDistance) {
                    bestAlignment = alignment;
                    bestDistance = distance;
                    targetUuid = candidate.getUUID();
                }
            }
        }
        Vector3d target = new Vector3d(flight.getDouble("targetX"), flight.getDouble("targetY"), flight.getDouble("targetZ"));
        if (targetUuid != null) {
            Entity entity = ((ServerWorld) level).getEntity(targetUuid);
            if (entity instanceof LivingEntity && YagiAwakeningSupport.isEnemy(owner, entity)
                    && distanceToSqr(entity) <= 64 * 64
                    && YagiAwakeningSupport.clearPath(owner, position(), entity.getBoundingBox().getCenter())) {
                target = entity.getBoundingBox().getCenter();
            } else targetUuid = null;
        }
        Vector3d velocity = getDeltaMovement(), offset = target.subtract(position());
        double[] steered = angelGuidance.steer(velocity.x, velocity.y, velocity.z,
                offset.x, offset.y, offset.z, age);
        setDeltaMovement(new Vector3d(steered[0], steered[1], steered[2]));
        hasImpulse = true;
    }

    private void recordAngelTrail() {
        Vector3d point = position();
        if (!angelTrail.isEmpty() && angelTrail.peekFirst().distanceToSqr(point) > 64) angelTrail.clear();
        if (angelTrail.isEmpty() || angelTrail.peekFirst().distanceToSqr(point) > .000001D) angelTrail.addFirst(point);
        while (angelTrail.size() > 14) angelTrail.removeLast();
    }

    /** World-space history, newest first, interpolated with the rendered projectile head. */
    public Vector3d[] getAngelTrail(float partial) {
        float blend = Math.max(0, Math.min(1, partial));
        Vector3d head = new Vector3d(xOld + (getX() - xOld) * blend,
                yOld + (getY() - yOld) * blend, zOld + (getZ() - zOld) * blend);
        if (tickCount == 0 || head.distanceToSqr(position()) > 64) head = position();
        Vector3d[] samples = angelTrail.toArray(new Vector3d[0]);
        if (samples.length < 2) return new Vector3d[]{head, head.subtract(getDeltaMovement().scale(.25D))};
        Vector3d[] trail = new Vector3d[samples.length];
        trail[0] = head;
        for (int i = 1; i < samples.length; i++) {
            Vector3d previous = samples[Math.min(i + 1, samples.length - 1)];
            trail[i] = previous.add(samples[i].subtract(previous).scale(blend));
        }
        return trail;
    }

    @Override public void onModHit(RayTraceResult result) {
        if (result instanceof EntityRayTraceResult) {
            Entity hit = ((EntityRayTraceResult) result).getEntity();
            LivingEntity owner = getThrower();
            if (hit instanceof LivingEntity && (owner == null || !YagiAwakeningSupport.isEnemy(owner, hit))) return;
        }
        super.onModHit(result);
    }

    @Override public void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(KIND, BULLET);
        entityData.define(TINT, 0);
        entityData.define(FLIGHT, new CompoundNBT());
        entityData.define(DARK_PALETTE, false);
    }
    public int getKind() { return entityData.get(KIND); }
    public boolean hasDarkPalette() { return entityData.get(DARK_PALETTE); }
    public int getTintIndex() { return entityData.get(TINT); }
    public float getFanAngle() { return entityData.get(FLIGHT).getFloat("angle"); }
    public float getVisualAge(float partial) {
        return entityData.get(FLIGHT).contains("start")
                ? Math.max(0, level.getGameTime() - entityData.get(FLIGHT).getLong("start")) + partial
                : tickCount + partial;
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128.0D * 128.0D; }

    @Override public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeInt(getKind());
        buffer.writeInt(getTintIndex());
        buffer.writeNbt(entityData.get(FLIGHT));
        buffer.writeBoolean(fanned);
        buffer.writeBoolean(hasDarkPalette());
    }
    @Override public void readSpawnData(PacketBuffer buffer) {
        super.readSpawnData(buffer);
        entityData.set(KIND, buffer.readInt());
        entityData.set(TINT, buffer.readInt());
        CompoundNBT flight = buffer.readNbt();
        entityData.set(FLIGHT, flight == null ? new CompoundNBT() : flight);
        fanned = buffer.readBoolean();
        entityData.set(DARK_PALETTE, buffer.readBoolean());
        setMaxLife(getKind() == BUSTER ? 24 : AngelOfDeathPattern.MAX_LIFE);
    }
    @Override public void addAdditionalSaveData(CompoundNBT tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChaosKind", getKind());
        tag.putInt("ChaosTint", getTintIndex());
        tag.put("ChaosFlight", entityData.get(FLIGHT).copy());
        if (targetUuid != null) tag.putUUID("ChaosTarget", targetUuid);
    }
    @Override public void readAdditionalSaveData(CompoundNBT tag) {
        super.readAdditionalSaveData(tag);
        // Short-lived attacks never resume and hit again after a chunk/server reload.
        expiredAfterLoad = true;
        entityData.set(KIND, tag.getInt("ChaosKind"));
        entityData.set(TINT, tag.getInt("ChaosTint"));
        entityData.set(FLIGHT, tag.getCompound("ChaosFlight"));
        if (tag.hasUUID("ChaosTarget")) targetUuid = tag.getUUID("ChaosTarget");
    }
}
