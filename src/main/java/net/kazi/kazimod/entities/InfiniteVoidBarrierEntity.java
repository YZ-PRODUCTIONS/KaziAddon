package net.kazi.kazimod.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InfiniteVoidBarrierEntity extends Entity implements IEntityAdditionalSpawnData {

    private static final DataParameter<Float> RADIUS =
            EntityDataManager.defineId(InfiniteVoidBarrierEntity.class, DataSerializers.FLOAT);

    private static final double WALL_THICKNESS = 0.6;
    private static final double PUSH_EPS = 0.12;
    private static final double SOFT_PUSH_BASE = 0.025;
    private static final double SOFT_PUSH_SCALE = 0.18;
    private static final double SOFT_PUSH_MAX = 0.085;
    private static final double SEPARATION_IMPULSE = 0.15;
    private static final int DAMAGE_INTERVAL = 4;

    private final Map<UUID, Double> lastMeasure = new HashMap<>();
    private double lastRadiusTick = -1.0;

    @Nullable
    private LivingEntity spawner;
    @Nullable
    private UUID spawnerUUID;
    private boolean hasSpawner;

    public InfiniteVoidBarrierEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public void setSpawner(LivingEntity spawner) {
        this.spawner = spawner;
        this.spawnerUUID = spawner.getUUID();
        this.hasSpawner = true;
    }

    public LivingEntity getSpawner() {
        return spawner;
    }

    public void setRadius(float r) {
        this.getEntityData().set(RADIUS, r);
    }

    public float getRadius() {
        return this.getEntityData().get(RADIUS);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(RADIUS, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide && this.tickCount % 20 == 0 && this.hasSpawner) {
            if (this.spawner == null && this.spawnerUUID != null && this.level instanceof ServerWorld) {
                Entity e = ((ServerWorld) this.level).getEntity(this.spawnerUUID);
                if (e instanceof LivingEntity) this.spawner = (LivingEntity) e;
            }
        }

        if (!this.level.isClientSide) {
            tickBarrierCollision();
        }
    }

    private void tickBarrierCollision() {
        float rF = this.getRadius();
        if (rF <= 1.0F) {
            lastMeasure.clear();
            lastRadiusTick = rF;
            return;
        }

        double r = rF;
        double prevR = lastRadiusTick > 0 ? lastRadiusTick : r;
        Vector3d c = this.position();

        AxisAlignedBB box = new AxisAlignedBB(
                c.x - (r + 3), c.y - (r + 3), c.z - (r + 3),
                c.x + (r + 3), c.y + (r + 3), c.z + (r + 3)
        );

        List<Entity> entities = this.level.getEntities(this, box,
                e -> e.isAlive() && e instanceof LivingEntity);

        for (Entity e : entities) {
            double pad = e.getBbWidth() * 0.5 + 0.02;
            Vector3d p = e.position();
            double dx = p.x - c.x;
            double dy = p.y - c.y;
            double dz = p.z - c.z;
            double measure = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (measure < 1e-6) measure = 1e-6;

            double prev = lastMeasure.getOrDefault(e.getUUID(), measure);
            // Treat all entities as inside at start if they're within radius
            boolean wasInside = prev <= prevR - pad;
            boolean isNearWall = Math.abs(measure - r) <= WALL_THICKNESS + pad;

            if (isNearWall && this.tickCount % DAMAGE_INTERVAL == 0) {
                e.hurt(DamageSource.CACTUS, 1.0F);
            }

            double insideLimit = r - PUSH_EPS - pad;
            double outsideLimit = r + PUSH_EPS + pad;
            Vector3d n = wallNormal(dx, dy, dz);

            if (wasInside) {
                if (measure > insideLimit) {
                    // Push back inside — no exceptions, including spawner
                    hardCorrect(e, true, insideLimit, n);
                    measure = insideLimit;
                } else if (isNearWall) {
                    applySoftPush(e, n, true, r, measure);
                }
            } else {
                if (measure < outsideLimit) {
                    hardCorrect(e, false, outsideLimit, n);
                    measure = outsideLimit;
                } else if (isNearWall) {
                    applySoftPush(e, n, false, r, measure);
                }
            }

            lastMeasure.put(e.getUUID(), measure);
        }

        lastRadiusTick = r;
    }

    private void hardCorrect(Entity e, boolean inside, double boundary, Vector3d outwardNormal) {
        double currentMeasure = measureAt(e.position());
        double penetration = inside
                ? Math.min(0.65, currentMeasure - boundary + 0.002)
                : Math.min(0.65, boundary - currentMeasure + 0.002);
        if (penetration < 0) penetration = 0;

        Vector3d delta = outwardNormal.scale(inside ? -penetration : penetration);
        e.move(MoverType.SELF, delta);
        e.hasImpulse = true;

        cancelIntoWallVelocity(e, outwardNormal, inside);
        addSeparationImpulse(e, outwardNormal, inside);

        // Teleport players directly to prevent clip-through
        if (e instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) e;
            Vector3d corrected = e.position();
            // Force a strong push away from wall for players
            Vector3d pushDir = inside ? outwardNormal.scale(-0.5) : outwardNormal.scale(0.5);
            Vector3d newPos = corrected.add(pushDir);
            sp.connection.teleport(newPos.x, newPos.y, newPos.z, sp.yRot, sp.xRot);
        }
    }

    private double measureAt(Vector3d pos) {
        Vector3d c = this.position();
        double dx = pos.x - c.x;
        double dy = pos.y - c.y;
        double dz = pos.z - c.z;
        double m = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return m < 1e-6 ? 1e-6 : m;
    }

    private Vector3d wallNormal(double dx, double dy, double dz) {
        Vector3d v = new Vector3d(dx, dy, dz);
        return v.lengthSqr() < 1e-8 ? new Vector3d(1, 0, 0) : v.normalize();
    }

    private void cancelIntoWallVelocity(Entity e, Vector3d outwardNormal, boolean inside) {
        Vector3d v = e.getDeltaMovement();
        double radial = v.x * outwardNormal.x + v.y * outwardNormal.y + v.z * outwardNormal.z;
        if (inside && radial > 0) v = v.subtract(outwardNormal.scale(radial));
        else if (!inside && radial < 0) v = v.subtract(outwardNormal.scale(radial));
        e.setDeltaMovement(v);
        e.hasImpulse = true;
    }

    private void addSeparationImpulse(Entity e, Vector3d outwardNormal, boolean inside) {
        Vector3d v = e.getDeltaMovement();
        Vector3d impulse = outwardNormal.scale(inside ? -SEPARATION_IMPULSE : SEPARATION_IMPULSE);
        e.setDeltaMovement(v.add(impulse));
        e.hasImpulse = true;
    }

    private void applySoftPush(Entity e, Vector3d n, boolean inside, double r, double measure) {
        Vector3d v = e.getDeltaMovement();
        double radial = v.x * n.x + v.y * n.y + v.z * n.z;
        double penetration = inside
                ? Math.max(0, measure - (r - PUSH_EPS))
                : Math.max(0, (r + PUSH_EPS) - measure);
        boolean pressing = inside ? radial > 0 : radial < 0;
        if (pressing) v = v.subtract(n.scale(radial));
        double push = Math.min(SOFT_PUSH_MAX, SOFT_PUSH_BASE + penetration * SOFT_PUSH_SCALE);
        v = v.add(n.scale(inside ? -push : push));
        e.setDeltaMovement(v);
        e.hasImpulse = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        tag.putFloat("radius", getRadius());
        tag.putBoolean("hasSpawner", hasSpawner);
        if (spawnerUUID != null) tag.putUUID("spawnerUUID", spawnerUUID);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        setRadius(tag.getFloat("radius"));
        hasSpawner = tag.getBoolean("hasSpawner");
        if (tag.hasUUID("spawnerUUID")) spawnerUUID = tag.getUUID("spawnerUUID");
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeFloat(getRadius());
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        setRadius(buffer.readFloat());
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}