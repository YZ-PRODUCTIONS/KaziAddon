package net.kazi.kazimod.entities.asriel;

import java.util.UUID;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport;
import net.kazi.kazimod.init.KaziEntities;
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
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

/** Short-lived visual aftermath. The falling star owns damage; these rings are purely cosmetic. */
public final class AsrielStarBurstEntity extends Entity {
    private static final DataParameter<Integer> START_TIME = EntityDataManager.defineId(AsrielStarBurstEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> ENHANCED = EntityDataManager.defineId(AsrielStarBurstEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FINISHER = EntityDataManager.defineId(AsrielStarBurstEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DARK_PALETTE = EntityDataManager.defineId(AsrielStarBurstEntity.class, DataSerializers.BOOLEAN);
    private UUID ownerId;

    public AsrielStarBurstEntity(EntityType<? extends AsrielStarBurstEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public static void spawn(LivingEntity owner, Vector3d point, boolean enhanced, boolean finisher) {
        if (!(owner.level instanceof ServerWorld)) return;
        AsrielStarBurstEntity burst = new AsrielStarBurstEntity(KaziEntities.ASRIEL_STAR_BURST.get(), owner.level);
        burst.ownerId = owner.getUUID();
        burst.setPos(point.x, point.y, point.z);
        burst.entityData.set(START_TIME, (int) owner.level.getGameTime());
        burst.entityData.set(ENHANCED, enhanced);
        burst.entityData.set(FINISHER, finisher);
        burst.entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(owner));
        owner.level.addFreshEntity(burst);
    }

    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerWorld)) return;
        Entity owner = ownerId == null ? null : ((ServerWorld) level).getEntity(ownerId);
        if (!(owner instanceof LivingEntity) || !owner.isAlive() || owner.removed || getVisualAge(0) >= getDuration()) {
            remove();
            return;
        }
        entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced((LivingEntity) owner));
    }

    public boolean isEnhanced() { return entityData.get(ENHANCED); }
    public boolean hasDarkPalette() { return entityData.get(DARK_PALETTE); }
    public boolean isFinisher() { return entityData.get(FINISHER); }
    public int getDuration() { return isFinisher() ? 28 : 16; }
    public float getVisualSize() { return (isEnhanced() ? 8.0F : 6.4F) * (isFinisher() ? 2.0F : 1.0F); }
    public float getVisualAge(float partialTick) {
        return Math.max(0, (int) level.getGameTime() - entityData.get(START_TIME)) + partialTick;
    }

    @Override protected void defineSynchedData() {
        entityData.define(START_TIME, 0);
        entityData.define(ENHANCED, false);
        entityData.define(FINISHER, false);
        entityData.define(DARK_PALETTE, false);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160.0D * 160.0D; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
