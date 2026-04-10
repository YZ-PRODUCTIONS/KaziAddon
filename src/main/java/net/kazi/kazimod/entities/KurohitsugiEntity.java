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

public class KurohitsugiEntity extends Entity {
    private static final DataParameter<Integer> LIFE_TICKS =
            EntityDataManager.defineId(KurohitsugiEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> OUTLINE_ONLY =
            EntityDataManager.defineId(KurohitsugiEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> PROGRESS =
            EntityDataManager.defineId(KurohitsugiEntity.class, DataSerializers.FLOAT);

    public KurohitsugiEntity(EntityType<? extends KurohitsugiEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static KurohitsugiEntity create(World world, double x, double y, double z, int lifeTicks) {
        KurohitsugiEntity coffin = new KurohitsugiEntity(KaziEntities.KUROHITSUGI.get(), world);
        coffin.setPos(x, y, z);
        coffin.entityData.set(LIFE_TICKS, lifeTicks);
        return coffin;
    }

    public static KurohitsugiEntity createOutline(World world, double x, double y, double z, int lifeTicks) {
        KurohitsugiEntity coffin = create(world, x, y, z, lifeTicks);
        coffin.entityData.set(OUTLINE_ONLY, true);
        coffin.entityData.set(PROGRESS, 0.0F);
        return coffin;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFE_TICKS, 0);
        this.entityData.define(OUTLINE_ONLY, false);
        this.entityData.define(PROGRESS, 1.0F);
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

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        this.entityData.set(LIFE_TICKS, nbt.getInt("LifeTicks"));
        this.entityData.set(OUTLINE_ONLY, nbt.getBoolean("OutlineOnly"));
        this.entityData.set(PROGRESS, nbt.getFloat("Progress"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putInt("LifeTicks", this.entityData.get(LIFE_TICKS));
        nbt.putBoolean("OutlineOnly", this.entityData.get(OUTLINE_ONLY));
        nbt.putFloat("Progress", this.entityData.get(PROGRESS));
    }

    public boolean isOutlineOnly() {
        return this.entityData.get(OUTLINE_ONLY);
    }

    public float getProgress() {
        return this.entityData.get(PROGRESS);
    }

    public void setProgress(float progress) {
        this.entityData.set(PROGRESS, progress);
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
