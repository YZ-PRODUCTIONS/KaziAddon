package net.kazi.kazimod.tusk;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** Short-lived mesh effects; neither collision nor per-particle network traffic. */
public final class TuskEffect extends Entity {
    public static final int PORTAL = 0, IMPACT = 1, BURST = 2, ROTATION = 3, MUZZLE = 4;
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(TuskEffect.class, DataSerializers.INT),
            LIFE = EntityDataManager.defineId(TuskEffect.class, DataSerializers.INT),
            TARGET = EntityDataManager.defineId(TuskEffect.class, DataSerializers.INT);
    private static final DataParameter<Integer> START = EntityDataManager.defineId(TuskEffect.class, DataSerializers.INT);
    private static final DataParameter<Float> SIZE = EntityDataManager.defineId(TuskEffect.class, DataSerializers.FLOAT);
    public TuskEffect(EntityType<?> type, World world) { super(type, world); noPhysics = true; setNoGravity(true); }
    @Override protected void defineSynchedData() {
        entityData.define(KIND, 0); entityData.define(LIFE, 20); entityData.define(TARGET, -1);
        entityData.define(START, 0); entityData.define(SIZE, 1F);
    }
    public static TuskEffect spawn(LivingEntity user, int kind, Vector3d position, int life, float size) {
        TuskEffect effect = new TuskEffect(TuskFeatures.EFFECT.get(), user.level);
        effect.entityData.set(KIND, kind); effect.entityData.set(START, (int) user.level.getGameTime());
        effect.entityData.set(LIFE, life); effect.entityData.set(SIZE, size);
        effect.setPos(position.x, position.y, position.z); user.level.addFreshEntity(effect); return effect;
    }
    public TuskEffect attach(LivingEntity target) { entityData.set(TARGET, target.getId()); return this; }
    public int kind() { return entityData.get(KIND); }
    public int life() { return entityData.get(LIFE); }
    public float size() { return entityData.get(SIZE); }
    public float age(float partial) { return Math.max(0, (int) level.getGameTime() - entityData.get(START) + partial); }
    @Override public void tick() {
        super.tick();
        if (entityData.get(TARGET) >= 0) {
            Entity target = level.getEntity(entityData.get(TARGET));
            if (target != null && target.isAlive()) setPos(target.getX(), target.getY(), target.getZ());
            else if (!level.isClientSide) remove();
        }
        if (!level.isClientSide && age(0) >= life()) remove();
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling() { return getBoundingBox().inflate(size() * 3 + 2); }
    @Override protected void readAdditionalSaveData(CompoundNBT data) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT data) {}
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
