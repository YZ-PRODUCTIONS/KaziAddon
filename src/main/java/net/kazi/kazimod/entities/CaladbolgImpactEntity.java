package net.kazi.kazimod.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** Visual-only blast. Its lifetime belongs to the impact, not the caster's hold. */
public class CaladbolgImpactEntity extends Entity {
    // All visual timings use animation ticks; half speed completes 90 ticks in 9 seconds.
    public static final float ANIMATION_SPEED = 0.5F;
    public static final int LIFETIME = 90;
    // Extend the afterglow without increasing the original blast's final footprint.
    public static final int GROWTH_TICKS = 60;
    public static final float BASE_RADIUS = 20.625F;
    public static final float EXPANSION_PER_TICK = 0.0375F;
    private static final DataParameter<Integer> START_TIME =
            EntityDataManager.defineId(CaladbolgImpactEntity.class, DataSerializers.INT);

    public CaladbolgImpactEntity(EntityType<? extends CaladbolgImpactEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
        if (!world.isClientSide) this.entityData.set(START_TIME, (int) world.getGameTime());
    }

    public float getVisualAge(float partialTick) {
        // Integer subtraction deliberately handles the game clock wrapping around.
        int elapsed = (int) this.level.getGameTime() - this.entityData.get(START_TIME);
        return Math.max(0.0F, elapsed + partialTick) * ANIMATION_SPEED;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && getVisualAge(0.0F) >= LIFETIME) this.remove();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(START_TIME, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) { }

    @Override
    public boolean isPickable() { return false; }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D * 256.0D;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
