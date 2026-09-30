package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class ZushiVfxEntity
extends Entity {
    public static final int GRAVITY = 0;
    public static final int IMPACT = 1;
    public static final int GRAVI_ZONE = 2;
    private static final DataParameter<Integer> MODE = EntityDataManager.defineId(ZushiVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> TARGET = EntityDataManager.defineId(ZushiVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> SIZE = EntityDataManager.defineId(ZushiVfxEntity.class, DataSerializers.FLOAT);
    private int refreshed;

    public ZushiVfxEntity(EntityType<? extends ZushiVfxEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static ZushiVfxEntity gravity(LivingEntity target, float size) {
        return ZushiVfxEntity.gravity(target, size, 0);
    }

    public static ZushiVfxEntity graviZone(LivingEntity caster, float range) {
        return ZushiVfxEntity.gravity(caster, range, 2);
    }

    private static ZushiVfxEntity gravity(LivingEntity target, float size, int mode) {
        ZushiVfxEntity effect = new ZushiVfxEntity((EntityType<? extends ZushiVfxEntity>)((EntityType)KaziEntities.ZUSHI_VFX.get()), target.level);
        effect.entityData.set(MODE, mode);
        effect.entityData.set(TARGET, target.getId());
        effect.setPos(target.getX(), target.getY(), target.getZ());
        effect.refresh(size);
        target.level.addFreshEntity((Entity)effect);
        return effect;
    }

    public static void impact(World world, Vector3d position, float size) {
        if (world.isClientSide) {
            return;
        }
        ZushiVfxEntity effect = new ZushiVfxEntity((EntityType<? extends ZushiVfxEntity>)((EntityType)KaziEntities.ZUSHI_VFX.get()), world);
        effect.entityData.set(MODE, 1);
        effect.entityData.set(SIZE, Float.valueOf(size));
        effect.setPos(position.x, position.y, position.z);
        world.addFreshEntity((Entity)effect);
    }

    public void refresh(float size) {
        this.refreshed = this.tickCount;
        this.entityData.set(SIZE, Float.valueOf(size));
    }

    public int getMode() {
        return (Integer)this.entityData.get(MODE);
    }

    public float getSize() {
        return ((Float)this.entityData.get(SIZE)).floatValue();
    }

    public Entity getTarget() {
        return this.level.getEntity(((Integer)this.entityData.get(TARGET)).intValue());
    }

    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            return;
        }
        if (this.getMode() == 0 || this.getMode() == 2) {
            Entity target = this.getTarget();
            if (target == null || !target.isAlive() || this.tickCount - this.refreshed > 4) {
                this.remove();
                return;
            }
            this.setPos(target.getX(), target.getY(), target.getZ());
        } else if (this.tickCount >= 50) {
            this.remove();
        }
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0;
    }

    protected void defineSynchedData() {
        this.entityData.define(MODE, 0);
        this.entityData.define(TARGET, -1);
        this.entityData.define(SIZE, Float.valueOf(3.0f));
    }

    protected void readAdditionalSaveData(CompoundNBT tag) {
    }

    protected void addAdditionalSaveData(CompoundNBT tag) {
    }

    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}
