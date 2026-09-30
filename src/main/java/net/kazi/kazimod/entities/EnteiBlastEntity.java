package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.entities.projectiles.mera.DaiEnkaiEnteiProjectile;

/** Visual-only copy of the custom Entei blast. The base projectile still owns the actual explosion. */
public class EnteiBlastEntity extends Entity {
    private static final int LIFE = EnteiBlastTimeline.SWELL_TICKS + EnteiBlastTimeline.EXPAND_TICKS + EnteiBlastTimeline.FADE_TICKS;
    private static final DataParameter<Float> INITIAL_RADIUS = EntityDataManager.defineId(EnteiBlastEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> MAX_RADIUS = EntityDataManager.defineId(EnteiBlastEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> START = EntityDataManager.defineId(EnteiBlastEntity.class, DataSerializers.INT);

    public EnteiBlastEntity(EntityType<? extends EnteiBlastEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static void spawn(DaiEnkaiEnteiProjectile projectile, BlockPos impact) {
        if (projectile.level.isClientSide) return;
        EnteiBlastEntity blast = new EnteiBlastEntity(KaziEntities.ENTEI_BLAST.get(), projectile.level);
        blast.setPos(impact.getX() + 0.5D, impact.getY() + 0.5D, impact.getZ() + 0.5D);
        float initial = EnteiGeometry.fireballRadius(projectile.getSize());
        blast.entityData.set(INITIAL_RADIUS, initial);
        blast.entityData.set(MAX_RADIUS, Math.max(initial, MathHelper.clamp(projectile.getSize() * 0.6F, 1.0F, 64.0F)));
        blast.entityData.set(START, (int) projectile.level.getGameTime());
        projectile.level.addFreshEntity(blast);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && this.tickCount >= LIFE) this.remove();
    }

    public float getAge(float partial) {
        return Math.max(0.0F, (int) this.level.getGameTime() - this.entityData.get(START) + partial);
    }

    public float getInitialRadius() { return this.entityData.get(INITIAL_RADIUS); }
    public float getMaximumRadius() { return this.entityData.get(MAX_RADIUS); }

    public float getWaveRadius(float partial) {
        return EnteiBlastTimeline.radius(getAge(partial), getInitialRadius(), getMaximumRadius());
    }

    public float getOpacity(float partial) {
        float fade = getAge(partial) - EnteiBlastTimeline.SWELL_TICKS - EnteiBlastTimeline.EXPAND_TICKS;
        return MathHelper.clamp(1.0F - Math.max(0.0F, fade) / EnteiBlastTimeline.FADE_TICKS, 0.0F, 1.0F);
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256.0D * 256.0D; }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(INITIAL_RADIUS, 1.0F);
        this.entityData.define(MAX_RADIUS, 1.0F);
        this.entityData.define(START, 0);
    }

    @Override protected void readAdditionalSaveData(CompoundNBT tag) { }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
