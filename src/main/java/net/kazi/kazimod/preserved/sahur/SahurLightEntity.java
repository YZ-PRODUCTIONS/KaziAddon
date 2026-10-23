package net.kazi.kazimod.preserved.sahur;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class SahurLightEntity extends Entity {
    private Entity gameplayBeam;
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(SahurLightEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.defineId(SahurLightEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> AGE = EntityDataManager.defineId(SahurLightEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> LIFE = EntityDataManager.defineId(SahurLightEntity.class, DataSerializers.INT);
    public SahurLightEntity(EntityType<?> type, World level) { super(type, level); noPhysics = true; setNoGravity(true); }
    protected void defineSynchedData() { entityData.define(KIND, 0); entityData.define(OWNER, -1); entityData.define(AGE, 0); entityData.define(LIFE, 40); }
    public int kind() { return entityData.get(KIND); }
    public int age() { return entityData.get(AGE); }
    public int life() { return entityData.get(LIFE); }
    public LivingEntity owner() { Entity e = level.getEntity(entityData.get(OWNER)); return e instanceof LivingEntity ? (LivingEntity) e : null; }
    public static SahurLightEntity spawn(LivingEntity owner, int kind, Vector3d pos, int life) {
        if (owner.level.isClientSide) return null;
        SahurLightEntity e = new SahurLightEntity(SahurEffects.LIGHT.get(), owner.level);
        e.entityData.set(OWNER, owner.getId()); e.entityData.set(KIND, kind); e.entityData.set(LIFE, life);
        e.setPos(pos.x, pos.y, pos.z); owner.level.addFreshEntity(e); return e;
    }
    public void tick() {
        super.tick();
        LivingEntity owner = owner();
        if (kind() == 2 && owner != null) setPos(owner.getX(), owner.getY(), owner.getZ());
        if (level.isClientSide) return;
        entityData.set(AGE, age() + 1);
        if (age() >= life() || owner == null || !owner.isAlive() || (kind() == 2 && !HeavenlyShield.active(owner))) { remove(); return; }
        if (kind() == 1 && age() == 6)
            gameplayBeam = net.kazi.kazimod.abilities.TripelT.HomeRunSwingAbility.INSTANCE.createAbility()
                    .fireDivineGameplay(owner, position());
    }
    public AxisAlignedBB getBoundingBoxForCulling() { return kind() >= 2 ? getBoundingBox().inflate(5) : getBoundingBox().inflate(36, 60, 36); }
    public boolean shouldRenderAtSqrDistance(double d) { return d < 256 * 256; }
    public void remove() { if (gameplayBeam != null && gameplayBeam.isAlive()) gameplayBeam.remove(); super.remove(); }
    protected void readAdditionalSaveData(CompoundNBT n) { remove(); }
    protected void addAdditionalSaveData(CompoundNBT n) { }
    public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
