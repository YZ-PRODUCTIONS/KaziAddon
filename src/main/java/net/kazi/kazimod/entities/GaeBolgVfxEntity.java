package net.kazi.kazimod.entities;

import net.kazi.kazimod.abilities.SupaRework.EnhancementAbility;
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
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

/** Charge and impact presentation only: no collision, damage, sounds or terrain edits. */
public final class GaeBolgVfxEntity extends Entity {
    public static final int IMPACT_TICKS = 42;
    private static final DataParameter<CompoundNBT> STATE =
            EntityDataManager.defineId(GaeBolgVfxEntity.class, DataSerializers.COMPOUND_TAG);
    private LivingEntity owner;

    public GaeBolgVfxEntity(EntityType<? extends GaeBolgVfxEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void beginCharge(LivingEntity caster) {
        this.owner = caster;
        CompoundNBT state = new CompoundNBT();
        state.putInt("owner", caster.getId());
        state.putInt("start", (int)this.level.getGameTime());
        this.entityData.set(STATE, state);
        followOwner();
    }

    public void beginImpact(Vector3d point, Vector3d direction) {
        setPos(point.x, point.y, point.z);
        CompoundNBT state = new CompoundNBT();
        state.putBoolean("impact", true);
        state.putInt("start", (int)this.level.getGameTime());
        Vector3d normal = direction.lengthSqr() < 0.0001 ? new Vector3d(0, -1, 0) : direction.normalize();
        state.putDouble("dx", normal.x);
        state.putDouble("dy", normal.y);
        state.putDouble("dz", normal.z);
        this.entityData.set(STATE, state);
    }

    public boolean isImpact() { return this.entityData.get(STATE).getBoolean("impact"); }

    public float getVisualAge(float partial) {
        return Math.max(0, (int)this.level.getGameTime() - this.entityData.get(STATE).getInt("start") + partial);
    }

    public LivingEntity getCaster() {
        if (!this.entityData.get(STATE).contains("owner")) return null;
        Entity caster = this.level.getEntity(this.entityData.get(STATE).getInt("owner"));
        return caster instanceof LivingEntity ? (LivingEntity)caster : null;
    }

    public Vector3d getImpactDirection() {
        CompoundNBT state = this.entityData.get(STATE);
        return new Vector3d(state.getDouble("dx"), state.getDouble("dy"), state.getDouble("dz"));
    }

    private void followOwner() {
        setPos(this.owner.getX(), this.owner.getY(), this.owner.getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;
        if (isImpact()) {
            if (getVisualAge(0) >= IMPACT_TICKS) remove();
        } else {
            if (this.owner == null || !this.owner.isAlive() || this.owner.level != this.level) {
                remove();
                return;
            }
            IAbilityData data = AbilityDataCapability.get(this.owner);
            EnhancementAbility ability = data == null ? null : data.getEquippedAbility(EnhancementAbility.INSTANCE);
            if (ability == null || !ability.isFourthChargingVfx(this) || getVisualAge(0) > 200) {
                remove();
                return;
            }
            followOwner();
        }
    }

    @Override protected void defineSynchedData() { this.entityData.define(STATE, new CompoundNBT()); }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 192 * 192; }
}
