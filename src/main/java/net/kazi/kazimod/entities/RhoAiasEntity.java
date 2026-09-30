package net.kazi.kazimod.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * The persistent seven-petal shield used by Projection's Rho Aias mode. It follows
 * the caster's facing direction, but has no collision or damage behavior.
 */
public class RhoAiasEntity extends Entity {
    private static final int MAX_LIFETIME = 125;
    private static final int RELEASE_FADE_TICKS = 6;
    private static final DataParameter<Boolean> RELEASED =
            EntityDataManager.defineId(RhoAiasEntity.class, DataSerializers.BOOLEAN);
    private LivingEntity owner;
    private int releaseTicks;

    public RhoAiasEntity(EntityType<? extends RhoAiasEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        updateFromOwner();
    }

    /** Lets the alternate VFX fade out over six ticks instead of popping away. */
    public void release() {
        if (!this.level.isClientSide) {
            this.entityData.set(RELEASED, true);
        }
    }

    public float getOpacity(float partialTick) {
        if (!this.entityData.get(RELEASED)) return 1.0F;
        return Math.max(0.0F, 1.0F - (this.releaseTicks + partialTick) / RELEASE_FADE_TICKS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(RELEASED, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vector3d.ZERO);
        this.setNoGravity(true);
        if (this.entityData.get(RELEASED)) {
            if (++this.releaseTicks >= RELEASE_FADE_TICKS && !this.level.isClientSide) {
                this.remove();
            }
            return;
        }
        if (!this.level.isClientSide) {
            if (this.owner == null || !this.owner.isAlive() || this.tickCount >= MAX_LIFETIME) {
                this.remove();
                return;
            }
            updateFromOwner();
        }
    }

    private void updateFromOwner() {
        if (this.owner == null) return;
        float yawRadians = this.owner.yRot * ((float) Math.PI / 180.0F);
        Vector3d shieldCenter = this.owner.getEyePosition(1.0F)
                .add(-Math.cos(yawRadians) * 0.3D, -0.25D, -Math.sin(yawRadians) * 0.3D)
                .add(this.owner.getLookAngle().scale(2.0D));
        this.moveTo(shieldCenter.x, shieldCenter.y, shieldCenter.z, this.owner.yRot, this.owner.xRot);
        this.yRot = this.owner.yRot;
        this.yRotO = this.owner.yRot;
        this.xRot = this.owner.xRot;
        this.xRotO = this.owner.xRot;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        // Effect entities never persist across saves.
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        // Effect entities never persist across saves.
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
