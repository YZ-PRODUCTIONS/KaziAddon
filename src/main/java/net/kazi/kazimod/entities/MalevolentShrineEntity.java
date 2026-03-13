package net.kazi.kazimod.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class MalevolentShrineEntity extends MobEntity {

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

