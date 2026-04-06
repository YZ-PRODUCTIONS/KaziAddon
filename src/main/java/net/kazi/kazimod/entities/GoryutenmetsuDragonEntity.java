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

public class GoryutenmetsuDragonEntity extends Entity {
    private static final DataParameter<Integer> LIFE_TICKS =
            EntityDataManager.defineId(GoryutenmetsuDragonEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> SCALE =
            EntityDataManager.defineId(GoryutenmetsuDragonEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ALPHA =
            EntityDataManager.defineId(GoryutenmetsuDragonEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> RENDER_YAW =
            EntityDataManager.defineId(GoryutenmetsuDragonEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> RENDER_PITCH =
            EntityDataManager.defineId(GoryutenmetsuDragonEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> RENDER_ROLL =
            EntityDataManager.defineId(GoryutenmetsuDragonEntity.class, DataSerializers.FLOAT);

    public GoryutenmetsuDragonEntity(EntityType<? extends GoryutenmetsuDragonEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static GoryutenmetsuDragonEntity create(World world, double x, double y, double z, float scale, float alpha, int lifeTicks) {
        GoryutenmetsuDragonEntity dragon = new GoryutenmetsuDragonEntity(KaziEntities.GORYUTENMETSU_DRAGON.get(), world);
        dragon.setPos(x, y, z);
        dragon.entityData.set(SCALE, scale);
        dragon.entityData.set(ALPHA, alpha);
        dragon.entityData.set(LIFE_TICKS, lifeTicks);
        dragon.entityData.set(RENDER_YAW, 0.0F);
        dragon.entityData.set(RENDER_PITCH, 0.0F);
        dragon.entityData.set(RENDER_ROLL, 0.0F);
        return dragon;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFE_TICKS, 0);
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(ALPHA, 1.0F);
        this.entityData.define(RENDER_YAW, 0.0F);
        this.entityData.define(RENDER_PITCH, 0.0F);
        this.entityData.define(RENDER_ROLL, 0.0F);
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

        float syncedYaw = this.entityData.get(RENDER_YAW);
        float syncedPitch = this.entityData.get(RENDER_PITCH);
        this.yRotO = this.yRot;
        this.xRotO = this.xRot;
        this.yRot = syncedYaw;
        this.xRot = syncedPitch;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public float getAlpha() {
        return this.entityData.get(ALPHA);
    }

    public void setAlpha(float alpha) {
        this.entityData.set(ALPHA, alpha);
    }

    public float getRenderYaw() {
        return this.entityData.get(RENDER_YAW);
    }

    public void setRenderYaw(float yaw) {
        this.entityData.set(RENDER_YAW, yaw);
        this.yRot = yaw;
        this.yRotO = yaw;
    }

    public float getRenderPitch() {
        return this.entityData.get(RENDER_PITCH);
    }

    public void setRenderPitch(float pitch) {
        this.entityData.set(RENDER_PITCH, pitch);
        this.xRot = pitch;
        this.xRotO = pitch;
    }

    public float getRenderRoll() {
        return this.entityData.get(RENDER_ROLL);
    }

    public void setRenderRoll(float roll) {
        this.entityData.set(RENDER_ROLL, roll);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        this.entityData.set(LIFE_TICKS, nbt.getInt("LifeTicks"));
        this.entityData.set(SCALE, nbt.getFloat("Scale"));
        this.entityData.set(ALPHA, nbt.getFloat("Alpha"));
        this.setRenderYaw(nbt.getFloat("Yaw"));
        this.setRenderPitch(nbt.getFloat("Pitch"));
        this.setRenderRoll(nbt.getFloat("Roll"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putInt("LifeTicks", this.entityData.get(LIFE_TICKS));
        nbt.putFloat("Scale", this.entityData.get(SCALE));
        nbt.putFloat("Alpha", this.entityData.get(ALPHA));
        nbt.putFloat("Yaw", this.yRot);
        nbt.putFloat("Pitch", this.xRot);
        nbt.putFloat("Roll", this.getRenderRoll());
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
