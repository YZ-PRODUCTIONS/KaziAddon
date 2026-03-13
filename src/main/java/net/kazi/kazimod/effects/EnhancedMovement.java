package net.kazi.kazimod.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class EnhancedMovement extends Effect {
    public static final float MOVEMENT_BOOST = 0.9F; // Base boost amount (mirrors WeakenedMovement's reduction)

    public EnhancedMovement() {
        super(EffectType.BENEFICIAL, 0x00CFFF); // Light blue color for the effect
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
     * Gets the movement multiplier based on the effect amplifier.
     * Mirrors WeakenedMovement's reduction as a buff instead:
     * Amplifier 0 = 1.9x (equivalent to WeakenedMovement Amp 0's 0.9x nerf, but added instead)
     * Amplifier 1 = 2.1x, Amplifier 2 = 2.3x, etc.
     *
     * @param amplifier The effect amplifier level
     * @return The movement multiplier (values > 1.0 indicate a boost)
     */
    public static float getMovementMultiplier(int amplifier) {
        // WeakenedMovement: multiplier = 0.9 - (amplifier * 0.2), min 0.2
        // The reduction from baseline (1.0) at each level:
        //   Amp 0: 1.0 - 0.9 = 0.1 reduction  → boost: 1.0 + 0.9 = 1.9
        //   Amp 1: 1.0 - 0.7 = 0.3 reduction  → boost: 1.0 + 0.7 = 2.1  (but WeakenedMovement caps at 0.2 min)
        //   Amp 4+: capped at 0.2             → boost capped at 1.0 + 0.8 = 1.8
        float boost = MOVEMENT_BOOST + (amplifier * 0.2F);
        return Math.min(1.0F + boost, 2.8F); // Cap mirrors WeakenedMovement's floor of 0.2 (i.e. 1.0 + 0.8 max boost from nerf = 1.8, uncapped here at 2.8 for higher amps)
    }
}