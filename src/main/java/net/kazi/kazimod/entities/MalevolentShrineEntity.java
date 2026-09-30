package net.kazi.kazimod.entities;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class MalevolentShrineEntity extends MobEntity {

    // Presentation metadata only. Domain damage, duration, and destruction stay in the local ability.
    private static final DataParameter<Float> DOMAIN_RADIUS = EntityDataManager.defineId(MalevolentShrineEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> VISUAL_START = EntityDataManager.defineId(MalevolentShrineEntity.class, DataSerializers.INT);
    private static final DataParameter<CompoundNBT> DOMAIN_ORIGIN = EntityDataManager.defineId(MalevolentShrineEntity.class, DataSerializers.COMPOUND_TAG);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DOMAIN_RADIUS, 0.0F);
        this.entityData.define(VISUAL_START, 0);
        this.entityData.define(DOMAIN_ORIGIN, new CompoundNBT());
    }

    public void activateDomainVisual(float radius, Vector3d origin) {
        CompoundNBT position = new CompoundNBT();
        position.putDouble("X", origin.x);
        position.putDouble("Y", origin.y);
        position.putDouble("Z", origin.z);
        this.entityData.set(DOMAIN_ORIGIN, position);
        this.entityData.set(DOMAIN_RADIUS, radius);
        this.entityData.set(VISUAL_START, (int) this.level.getGameTime());
    }

    public Vector3d getDomainOrigin() {
        CompoundNBT position = this.entityData.get(DOMAIN_ORIGIN);
        return position.isEmpty() ? this.position() : new Vector3d(position.getDouble("X"), position.getDouble("Y"), position.getDouble("Z"));
    }

    public float getDomainRadius() { return this.entityData.get(DOMAIN_RADIUS); }
    public float getConstruction() { return 1.0F; }

    public float getVfxAge(float partial) {
        int elapsed = (int) this.level.getGameTime() - this.entityData.get(VISUAL_START);
        return Math.max(0.0F, elapsed + partial);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        if (getDomainRadius() <= 0.0F) return super.shouldRenderAtSqrDistance(distance);
        double range = getDomainRadius() + 96.0D + position().distanceTo(getDomainOrigin());
        return distance <= range * range;
    }

    public MalevolentShrineEntity(EntityType<? extends MalevolentShrineEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;   // no gravity, no block collision
        this.noCulling = true;   // always render regardless of frustum
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 10.0D);
    }

    // ── Intangibility overrides ───────────────────────────────────────────────

    /** Entities and players walk through the shrine without being pushed. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /** Prevent other entities from being pushed by this one. */
    @Override
    protected void doPush(net.minecraft.entity.Entity entity) {}

    /** Skip the standard entity-collision sweep entirely. */
    @Override
    protected void pushEntities() {}

    // ── Persistence / despawn ─────────────────────────────────────────────────

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void checkDespawn() {
        // prevent natural despawn
    }

    @Override
    public boolean canBeLeashed(PlayerEntity player) {
        return false;
    }

    @Override
    public void knockback(float strength, double ratioX, double ratioZ) {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}

