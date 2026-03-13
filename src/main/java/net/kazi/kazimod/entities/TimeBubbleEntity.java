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
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class TimeBubbleEntity extends Entity {

    // ── Synced data ───────────────────────────────────────────────────────────
    /** ID of the trapped entity — used to follow it each tick. */
    public static final DataParameter<Integer> TARGET_ID_PARAM =
            EntityDataManager.defineId(TimeBubbleEntity.class, DataSerializers.INT);
    public static final DataParameter<Integer> LIFE_TICKS_PARAM =
            EntityDataManager.defineId(TimeBubbleEntity.class, DataSerializers.INT);
    public static final DataParameter<Integer> SCALE_INT_PARAM =
            EntityDataManager.defineId(TimeBubbleEntity.class, DataSerializers.INT);
    /**
     * False while the entity is still rising to its float height.
     * The renderer should suppress (or skip) the bubble animation while this is false.
     * Flipped to true by ChronostasisAbility once the target has reached float height.
     */
    public static final DataParameter<Boolean> ACTIVE_PARAM =
            EntityDataManager.defineId(TimeBubbleEntity.class, DataSerializers.BOOLEAN);

    // Keep private aliases pointing to the same params for internal use.
    private static final DataParameter<Integer> TARGET_ID   = TARGET_ID_PARAM;
    private static final DataParameter<Integer> LIFE_TICKS  = LIFE_TICKS_PARAM;
    private static final DataParameter<Integer> SCALE_INT   = SCALE_INT_PARAM;
    private static final DataParameter<Boolean> ACTIVE      = ACTIVE_PARAM;

    // ── Constructor ───────────────────────────────────────────────────────────
    public TimeBubbleEntity(EntityType<? extends TimeBubbleEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true; // prevent frustum culling on the tiny bounding box
    }

    /**
     * Factory: creates a bubble locked onto {@code target} for {@code lifeTicks} ticks.
     * The bubble starts INACTIVE (animation suppressed) until
     * {@link #setActive(boolean)} is called with {@code true}.
     * Scale is derived from the target's bounding box so the bubble wraps it snugly.
     */
    public static TimeBubbleEntity create(World world, LivingEntity target, int lifeTicks) {
        TimeBubbleEntity bubble = new TimeBubbleEntity(KaziEntities.TIME_BUBBLE.get(), world);
        bubble.setPos(target.getX(), target.getY(), target.getZ());
        bubble.entityData.set(TARGET_ID,   target.getId());
        bubble.entityData.set(LIFE_TICKS,  lifeTicks);
        bubble.entityData.set(ACTIVE,      false); // inactive until target is airborne
        // Scale = max of width and height, clamped to a sensible range.
        float size = Math.max(target.getBbWidth(), target.getBbHeight());
        size = Math.max(0.5F, Math.min(size, 8.0F));
        bubble.entityData.set(SCALE_INT, (int)(size * 100));
        return bubble;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();

        // Follow the trapped entity.
        int targetId = this.entityData.get(TARGET_ID);
        Entity target = this.level.getEntity(targetId);
        if (target != null && target.isAlive()) {
            this.setPos(target.getX(), target.getY(), target.getZ());
        }

        // Count down and self-remove.
        int remaining = this.entityData.get(LIFE_TICKS) - 1;
        if (remaining <= 0) {
            this.remove();
        } else {
            this.entityData.set(LIFE_TICKS, remaining);
        }
    }

    // ── No save / no NBT ─────────────────────────────────────────────────────
    @Override protected void defineSynchedData() {
        this.entityData.define(TARGET_ID,  -1);
        this.entityData.define(LIFE_TICKS, 0);
        this.entityData.define(SCALE_INT,  100);
        this.entityData.define(ACTIVE,     false);
    }

    @Override protected void readAdditionalSaveData(CompoundNBT nbt) {}
    @Override protected void addAdditionalSaveData(CompoundNBT nbt) {}

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ── Accessors for the renderer / ability ──────────────────────────────────
    public float getScale() {
        return this.entityData.get(SCALE_INT) / 100.0F;
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    /** @return true once the target has reached its float height and the animation should play. */
    public boolean isActive() {
        return this.entityData.get(ACTIVE);
    }

    /** Called server-side by ChronostasisAbility when the target reaches float height. */
    public void setActive(boolean active) {
        this.entityData.set(ACTIVE, active);
    }
}