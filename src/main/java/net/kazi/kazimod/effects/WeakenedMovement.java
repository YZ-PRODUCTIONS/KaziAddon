package net.kazi.kazimod.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.kazi.kazimod.init.KaziEffects;

public class WeakenedMovement extends Effect {
    public static final float MOVEMENT_REDUCTION = 0.9F;

    public WeakenedMovement() {
        super(EffectType.HARMFUL, 0x4A4A4A);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is handled through event listeners
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public static float getMovementMultiplier(int amplifier) {
        float reduction = MOVEMENT_REDUCTION - (amplifier * 0.2F);
        return Math.max(0.2F, reduction);
    }

    /** Returns true if this effect should be blocked due to mutual exclusivity */
    public static boolean isBlocked(LivingEntity entity) {
        return entity.hasEffect(KaziEffects.MINIATURIZED.get());
    }
}