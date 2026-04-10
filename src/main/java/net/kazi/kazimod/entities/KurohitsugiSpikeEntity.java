package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class KurohitsugiSpikeEntity extends Entity {
    private static final DataParameter<Integer> LIFE_TICKS =
            EntityDataManager.defineId(KurohitsugiSpikeEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> INITIAL_LIFE_TICKS =
            EntityDataManager.defineId(KurohitsugiSpikeEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> SCALE =
            EntityDataManager.defineId(KurohitsugiSpikeEntity.class, DataSerializers.FLOAT);

    public KurohitsugiSpikeEntity(EntityType<? extends KurohitsugiSpikeEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static KurohitsugiSpikeEntity create(World world, double x, double y, double z, float yaw, float pitch, float scale, int lifeTicks) {
        KurohitsugiSpikeEntity spike = new KurohitsugiSpikeEntity(KaziEntities.KUROHITSUGI_SPIKE.get(), world);
        spike.moveTo(x, y, z, yaw, pitch);
        spike.yRot = yaw;
        spike.xRot = pitch;
        spike.yRotO = yaw;
        spike.xRotO = pitch;
        spike.entityData.set(SCALE, scale);
        spike.entityData.set(LIFE_TICKS, lifeTicks);
        spike.entityData.set(INITIAL_LIFE_TICKS, lifeTicks);
        return spike;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFE_TICKS, 0);
        this.entityData.define(INITIAL_LIFE_TICKS, 1);
        this.entityData.define(SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        int remaining = this.entityData.get(LIFE_TICKS) - 1;
        if (remaining <= 0) {
            this.remove();
        } else {
            this.entityData.set(LIFE_TICKS, remaining);
        }
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public float getGrowthProgress() {
        int initialLife = Math.max(1, this.entityData.get(INITIAL_LIFE_TICKS));
        int remainingLife = Math.max(0, this.entityData.get(LIFE_TICKS));
        int elapsedLife = initialLife - remainingLife;
        float progress = elapsedLife / 12.0F;
        return Math.min(1.0F, Math.max(0.0F, progress));
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        this.entityData.set(LIFE_TICKS, nbt.getInt("LifeTicks"));
        this.entityData.set(INITIAL_LIFE_TICKS, Math.max(1, nbt.getInt("InitialLifeTicks")));
        this.entityData.set(SCALE, nbt.getFloat("Scale"));
        this.yRot = nbt.getFloat("Yaw");
        this.xRot = nbt.getFloat("Pitch");
        this.yRotO = this.yRot;
        this.xRotO = this.xRot;
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putInt("LifeTicks", this.entityData.get(LIFE_TICKS));
        nbt.putInt("InitialLifeTicks", this.entityData.get(INITIAL_LIFE_TICKS));
        nbt.putFloat("Scale", this.entityData.get(SCALE));
        nbt.putFloat("Yaw", this.yRot);
        nbt.putFloat("Pitch", this.xRot);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
