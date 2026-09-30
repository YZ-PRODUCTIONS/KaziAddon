package net.kazi.kazimod.entities;

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

/** A visual-only, stationary gear for Reality Marble effects. */
public class RealityMarbleGearEntity extends Entity {
    private static final DataParameter<Float> SPIN_OFFSET =
            EntityDataManager.defineId(RealityMarbleGearEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> MUGEN_VARIANT =
            EntityDataManager.defineId(RealityMarbleGearEntity.class, DataSerializers.BOOLEAN);

    public RealityMarbleGearEntity(EntityType<? extends RealityMarbleGearEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setDeltaMovement(Vector3d.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(SPIN_OFFSET, 0.0F);
        this.getEntityData().define(MUGEN_VARIANT, false);
    }

    public void setSpinOffset(float spinOffset) {
        this.getEntityData().set(SPIN_OFFSET, spinOffset);
    }

    public float getSpinOffset() {
        return this.getEntityData().get(SPIN_OFFSET);
    }

    public void setMugenVariant(boolean mugenVariant) {
        this.getEntityData().set(MUGEN_VARIANT, mugenVariant);
    }

    public boolean isMugenVariant() {
        return this.getEntityData().get(MUGEN_VARIANT);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        // Visual-only entity; intentionally has no persistent state.
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        // Visual-only entity; intentionally has no persistent state.
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vector3d.ZERO);
        this.setNoGravity(true);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
