//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class WeakenedMovement extends Effect {
    public static final float MOVEMENT_REDUCTION = 0.8F; // 50% reduction

    public WeakenedMovement() {
        super(EffectType.HARMFUL, 0x4A4A4A); // Gray color for the effect
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is handled through event listeners
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    /**
     * Gets the movement multiplier based on the effect amplifier
     * @param amplifier The effect amplifier level
     * @return The movement multiplier (0.5 = 50% reduction at base level)
     */
    public static float getMovementMultiplier(int amplifier) {
        // Each amplifier level reduces movement by an additional 10%
        // Amplifier 0 = 50%, Amplifier 1 = 40%, etc.
        float reduction = MOVEMENT_REDUCTION - (amplifier * 0.1F);
        return Math.max(0.1F, reduction); // Minimum 10% movement
    }
}