package net.kazi.kazimod.bomu;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class BomuVfxEntity extends Entity {
    public static final int BLAST = 0, HANDS = 1, FLASH = 2, JET = 3;
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(BomuVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> LIFE = EntityDataManager.defineId(BomuVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(BomuVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> RADIUS = EntityDataManager.defineId(BomuVfxEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> YAW = EntityDataManager.defineId(BomuVfxEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> PITCH = EntityDataManager.defineId(BomuVfxEntity.class, DataSerializers.FLOAT);

    public BomuVfxEntity(EntityType<? extends BomuVfxEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        setNoGravity(true);
    }

    public static BomuVfxEntity spawn(World world, double x, double y, double z, float radius, int kind, int life) {
        if (world.isClientSide || !Float.isFinite(radius) || radius <= 0) return null;
        BomuVfxEntity effect = new BomuVfxEntity(KaziEntities.BOMU_VFX.get(), world);
        effect.setPos(x, y, z);
        effect.entityData.set(RADIUS, Math.min(24, radius));
        effect.entityData.set(KIND, kind);
        effect.entityData.set(LIFE, Math.max(1, Math.min(1200, life)));
        world.addFreshEntity(effect);
        return effect;
    }

    public void attach(LivingEntity owner) { entityData.set(OWNER, owner.getId()); }
    public Entity owner() { return level.getEntity(entityData.get(OWNER)); }
    public int kind() { return entityData.get(KIND); }
    public int life() { return entityData.get(LIFE); }
    public float radius() { return entityData.get(RADIUS); }
    public float yaw() { return entityData.get(YAW); }
    public float pitch() { return entityData.get(PITCH); }

    public void direction(Vector3d direction) {
        entityData.set(YAW, (float) Math.toDegrees(Math.atan2(direction.x, direction.z)));
        entityData.set(PITCH, (float) -Math.toDegrees(Math.atan2(direction.y,
                Math.sqrt(direction.x * direction.x + direction.z * direction.z))));
    }

    @Override public void tick() {
        super.tick();
        if (kind() == HANDS) {
            Entity owner = owner();
            if (owner != null && owner.isAlive()) setPos(owner.getX(), owner.getY(), owner.getZ());
            else if (!level.isClientSide) remove();
        }
        if (tickCount >= life()) remove();
    }

    @Override public AxisAlignedBB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(kind() == HANDS ? 3 : radius() * (kind() == JET ? 4 : 3));
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 192 * 192; }
    @Override protected void defineSynchedData() {
        entityData.define(KIND, BLAST); entityData.define(LIFE, 32); entityData.define(OWNER, -1);
        entityData.define(RADIUS, 3F); entityData.define(YAW, 0F); entityData.define(PITCH, 0F);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) {}
    @Override protected void addAdditionalSaveData(CompoundNBT tag) {}
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
