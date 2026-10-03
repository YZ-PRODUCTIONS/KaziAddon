package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class GuraVfxEntity extends Entity {
    public static final int AIR = 0, GROUND = 1, CHARGE = 2, ORIENTED_CRACK = 3;
    private static final DataParameter<Integer> MODE = EntityDataManager.defineId(GuraVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(GuraVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> SIZE = EntityDataManager.defineId(GuraVfxEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> LIFE = EntityDataManager.defineId(GuraVfxEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> PITCH = EntityDataManager.defineId(GuraVfxEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> YAW = EntityDataManager.defineId(GuraVfxEntity.class, DataSerializers.FLOAT);
    public GuraVfxEntity(EntityType<? extends GuraVfxEntity> type, World world) {
        super(type, world); noPhysics = true; noCulling = true; setNoGravity(true);
    }
    public static GuraVfxEntity spawn(World world, double x, double y, double z, float radius, int mode) {
        if (world.isClientSide) return null;
        GuraVfxEntity effect = new GuraVfxEntity(KaziEntities.GURA_VFX.get(), world);
        effect.setPos(x, y, z); effect.entityData.set(SIZE, radius); effect.entityData.set(MODE, mode);
        world.addFreshEntity(effect); return effect;
    }
    public static GuraVfxEntity charge(Entity caster) {
        GuraVfxEntity e = spawn(caster.level, caster.getX(), caster.getY(), caster.getZ(), .55F, CHARGE);
        if (e != null) { e.entityData.set(OWNER, caster.getId()); e.entityData.set(LIFE, 100); }
        return e;
    }
    public static void fracture(World world, double x, double y, double z, float radius, float pitch, float yaw) {
        GuraVfxEntity e = spawn(world, x, y, z, Math.max(.5F, Math.min(72, radius)), ORIENTED_CRACK);
        if (e != null) { e.entityData.set(PITCH, pitch); e.entityData.set(YAW, yaw); }
    }
    public float getCrackPitch() { return entityData.get(PITCH); }
    public float getCrackYaw() { return entityData.get(YAW); }
    public int getMode() { return entityData.get(MODE); }
    public float getSize() { return entityData.get(SIZE); }
    public int getLife() { return entityData.get(LIFE); }
    public Entity getOwner() { return level.getEntity(entityData.get(OWNER)); }
    public static Vector3d handOrigin(Entity caster) {
        Vector3d look = caster.getLookAngle();
        return caster.position().add(look.scale(.8)).add(-look.z * .45, caster.getBbHeight() * .72, look.x * .45);
    }
    @Override public void tick() {
        super.tick();
        if (getMode() == CHARGE) {
            Entity owner = getOwner();
            if (owner != null && owner.isAlive()) { Vector3d p = handOrigin(owner); setPos(p.x, p.y, p.z); }
            else if (!level.isClientSide) remove();
        }
        if (tickCount >= getLife()) remove();
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
    @Override protected void defineSynchedData() {
        entityData.define(MODE, AIR); entityData.define(OWNER, -1); entityData.define(SIZE, 6F); entityData.define(LIFE, 84);
        entityData.define(PITCH, 0F); entityData.define(YAW, 0F);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) {}
    @Override protected void addAdditionalSaveData(CompoundNBT tag) {}
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
