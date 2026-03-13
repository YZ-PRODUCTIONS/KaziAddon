package net.kazi.kazimod.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BouncyEffect extends Effect {

    private static final float BOUNCE_FALL_THRESHOLD = 12.0F;
    private static final float BOUNCE_DIVISOR = 30.0F;

    // IMPORTANT: there is only ONE Effect instance in the game, shared by all entities.
    // Per-entity state MUST be stored in a map keyed by UUID, not as plain fields.
    private final Map<UUID, Boolean> prevOnGround = new HashMap<>();
    private final Map<UUID, Float> prevFallDistance = new HashMap<>();
    private final Map<UUID, Boolean> armed = new HashMap<>();

    public BouncyEffect() {
        super(EffectType.BENEFICIAL, 0xFFFFFF);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        UUID id = entity.getUUID();

        boolean currentOnGround = entity.isOnGround();
        float currentFallDist = entity.fallDistance;

        boolean wasOnGround = prevOnGround.getOrDefault(id, true);
        float lastFallDist = prevFallDistance.getOrDefault(id, 0.0F);
        boolean isArmed = armed.getOrDefault(id, false);

        // Arm once entity has fallen far enough
        if (!currentOnGround && currentFallDist > BOUNCE_FALL_THRESHOLD) {
            isArmed = true;
        }

        // Landing transition: airborne last tick -> grounded this tick
        if (isArmed && !wasOnGround && currentOnGround) {
            // Use lastFallDist because vanilla resets fallDistance to 0 before this runs
            float bounceVal = lastFallDist > 0 ? lastFallDist : currentFallDist;
            double bounceY = bounceVal / BOUNCE_DIVISOR;

            if (bounceY > 0.0) {
                entity.setDeltaMovement(
                        entity.getDeltaMovement().x,
                        bounceY,
                        entity.getDeltaMovement().z
                );
                entity.hasImpulse = true;
                entity.fallDistance = 0.0F;
                isArmed = false;

                SoundEvent sfx = (SoundEvent) ModSounds.BOUNCE_1.get();
                if (entity.getRandom().nextBoolean()) {
                    sfx = (SoundEvent) ModSounds.BOUNCE_2.get();
                }
                entity.level.playSound(
                        (PlayerEntity) null,
                        entity.blockPosition(),
                        sfx,
                        SoundCategory.PLAYERS,
                        1.5F,
                        0.75F + entity.getRandom().nextFloat() / 2.0F
                );
            }
        }

        // Store current state for next tick.
        // If vanilla already zeroed fallDistance on landing, keep previous value.
        prevFallDistance.put(id, currentFallDist > 0.0F ? currentFallDist : lastFallDist);
        prevOnGround.put(id, currentOnGround);
        armed.put(id, isArmed);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    /**
     * Clean up state when the effect expires or is removed so the maps don't leak.
     */
    public void onEffectRemoved(LivingEntity entity) {
        UUID id = entity.getUUID();
        prevOnGround.remove(id);
        prevFallDistance.remove(id);
        armed.remove(id);
    }
}