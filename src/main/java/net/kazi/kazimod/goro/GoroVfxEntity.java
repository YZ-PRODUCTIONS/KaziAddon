package net.kazi.kazimod.goro;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** One renewable visual per charge, not a stream of particle entities. */
public final class GoroVfxEntity extends Entity {
    public static final int HAND = 0, CAGE = 1, AIM = 2, BURST = 3, AMARU = 4, IMPACT = 5;
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(GoroVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(GoroVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> LIFE = EntityDataManager.defineId(GoroVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> RADIUS = EntityDataManager.defineId(GoroVfxEntity.class, DataSerializers.FLOAT);
    private int expiry;

    public GoroVfxEntity(EntityType<? extends GoroVfxEntity> type, World world) {
        super(type, world); noPhysics = true; setNoGravity(true); noCulling = true;
    }

    public static GoroVfxEntity spawn(World world, Vector3d position, int kind, float radius, int life) {
        if (world.isClientSide || !Float.isFinite(radius) || radius <= 0) return null;
        GoroVfxEntity fx = new GoroVfxEntity(KaziEntities.GORO_VFX.get(), world);
        fx.setPos(position.x, position.y, position.z);
        fx.entityData.set(KIND, kind); fx.entityData.set(RADIUS, Math.min(128, radius));
        fx.entityData.set(LIFE, Math.max(1, life)); fx.expiry = Math.max(1, life);
        world.addFreshEntity(fx);
        return fx;
    }

    public static GoroVfxEntity refresh(GoroVfxEntity fx, LivingEntity owner, Vector3d position,
                                         int kind, float radius, boolean attached) {
        if (owner.level.isClientSide) return fx;
        if (fx == null || !fx.isAlive() || fx.level != owner.level) {
            fx = spawn(owner.level, position, kind, radius, 12);
            if (fx == null) return null;
            if (attached) fx.entityData.set(OWNER, owner.getId());
        }
        fx.setPos(position.x, position.y, position.z);
        fx.entityData.set(RADIUS, Math.min(128, Math.max(.1F, radius)));
        fx.expiry = fx.tickCount + 12;
        return fx;
    }

    public static GoroVfxEntity stop(GoroVfxEntity fx) {
        if (fx != null && !fx.level.isClientSide) fx.remove();
        return null;
    }
    public int kind() { return entityData.get(KIND); }
    public float radius() { return entityData.get(RADIUS); }
    public int life() { return entityData.get(LIFE); }
    public Entity owner() { return level.getEntity(entityData.get(OWNER)); }
    @Override public void tick() {
        super.tick();
        Entity owner = owner();
        if (owner != null) setPos(owner.getX(), owner.getY(), owner.getZ());
        if (!level.isClientSide && (tickCount >= expiry || (entityData.get(OWNER) >= 0 && (owner == null || !owner.isAlive())))) remove();
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling() { return getBoundingBox().inflate(radius() * 2); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
    @Override protected void defineSynchedData() {
        entityData.define(KIND, BURST); entityData.define(OWNER, -1);
        entityData.define(LIFE, 24); entityData.define(RADIUS, 3F);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) {}
    @Override protected void addAdditionalSaveData(CompoundNBT tag) {}
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
