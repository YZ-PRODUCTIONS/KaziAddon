package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** One tiny, short-lived visual event per weapon contact. Never deals damage. */
public final class BabylonImpactEntity extends Entity {
    public static final int LIFETIME_TICKS = 12;
    private static final DataParameter<CompoundNBT> STATE =
            EntityDataManager.defineId(BabylonImpactEntity.class, DataSerializers.COMPOUND_TAG);

    public BabylonImpactEntity(EntityType<? extends BabylonImpactEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        setNoGravity(true);
    }

    public static void spawn(World world, Vector3d contact, Vector3d outward, boolean block, int seed) {
        if (world.isClientSide) return;
        BabylonImpactEntity impact = new BabylonImpactEntity(KaziEntities.BABYLON_IMPACT.get(), world);
        if (outward.lengthSqr() < .0001) outward = new Vector3d(0, 1, 0);
        outward = outward.normalize();
        // Lift the flash off the contacted face to avoid z-fighting with the block.
        impact.setPos(contact.x + outward.x * .035, contact.y + outward.y * .035, contact.z + outward.z * .035);
        CompoundNBT state = new CompoundNBT();
        state.putLong("start", world.getGameTime());
        state.putFloat("nx", (float) outward.x);
        state.putFloat("ny", (float) outward.y);
        state.putFloat("nz", (float) outward.z);
        state.putBoolean("block", block);
        state.putInt("seed", seed);
        impact.entityData.set(STATE, state);
        world.addFreshEntity(impact);
    }

    public float getVisualAge(float partial) {
        return Math.max(0, level.getGameTime() - entityData.get(STATE).getLong("start") + partial);
    }
    public Vector3d getOutwardDirection() {
        CompoundNBT state = entityData.get(STATE);
        return new Vector3d(state.getFloat("nx"), state.getFloat("ny"), state.getFloat("nz"));
    }
    public boolean isBlockImpact() { return entityData.get(STATE).getBoolean("block"); }
    public int getVisualSeed() { return entityData.get(STATE).getInt("seed"); }

    @Override public void tick() {
        super.tick();
        // The server owns removal; a client can briefly receive the spawn before
        // synchronized metadata and must not discard the event during that gap.
        if (!level.isClientSide && getVisualAge(0) >= LIFETIME_TICKS) remove();
    }
    @Override protected void defineSynchedData() { entityData.define(STATE, new CompoundNBT()); }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
}
