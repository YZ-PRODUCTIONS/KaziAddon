package net.kazi.kazimod.api.helpers;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;

public class KaziAbilityHelper {

    /**
     * Adds tired stacks to an entity, similar to how frostbite stacks work
     * @param target The entity to apply tired to
     * @param source The entity causing the tired effect (can be null)
     * @param stacks Number of stacks to add
     */
    public static void addTiredStacks(LivingEntity target, LivingEntity source, int stacks) {
        if (target == null) return;

        EffectInstance existingTired = target.getEffect((Effect) KaziEffects.TIRED.get());

        int newAmplifier;
        int duration = 400; // 20 seconds default

        if (existingTired != null) {
            // Add to existing stacks (amplifier + 1 = level)
            newAmplifier = existingTired.getAmplifier() + stacks;
            // Keep the remaining duration or extend it
            duration = Math.max(existingTired.getDuration(), 400);
        } else {
            // First application
            newAmplifier = stacks - 1; // stacks of 6 means amplifier 5 (level 6)
        }

        // Apply the effect with the new amplifier (no cap)
        target.addEffect(new EffectInstance((Effect) KaziEffects.TIRED.get(), duration, newAmplifier));
    }
}