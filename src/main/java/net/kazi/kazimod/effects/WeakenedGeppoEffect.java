package xyz.pixelatedw.mineminenomi.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

/**
 * Effect that weakens Geppo ability movement by half.
 * When applied to an entity, this effect reduces the propulsion force
 * of Geppo jumps by 50%.
 */
public class WeakenedGeppoEffect extends Effect {

    public WeakenedGeppoEffect() {
        super(EffectType.HARMFUL, 0x8B7355); // Brown/gray color for weakened effect
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Passive effect - actual weakening is applied in GeppoAbility
    }

    /**
     * Gets the movement multiplier based on effect amplifier.
     * @param amplifier The effect amplifier level
     * @return The movement multiplier (0.5 for base level)
     */
    public static double getMovementMultiplier(int amplifier) {
        // Base effect: 50% movement (0.5x)
        // Each amplifier level reduces by additional 10%
        return Math.max(0.1, 0.5 - (amplifier * 0.1));
    }
}