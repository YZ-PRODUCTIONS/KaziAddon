//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.events.handlers;

import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ModifiedHumanHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.GeppoAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.SoruAbility;

@Mod.EventBusSubscriber
public class WeakenedMovementEventHandler {

    private static Effect weakenedMovementEffect = null;

    /**
     * Lazily get the effect from the registry to avoid initialization order issues
     */
    private static Effect getWeakenedMovementEffect() {
        if (weakenedMovementEffect == null) {
            weakenedMovementEffect = ForgeRegistries.POTIONS.getValue(
                    new net.minecraft.util.ResourceLocation("kazimod", "weakened_movement")
            );
        }
        return weakenedMovementEffect;
    }

    /**
     * Gets the movement multiplier based on the effect amplifier
     * @param amplifier The effect amplifier level
     * @return The movement multiplier (0.5 = 50% reduction at base level)
     */
    private static float getMovementMultiplier(int amplifier) {
        // Each amplifier level reduces movement by an additional 10%
        // Amplifier 0 = 50%, Amplifier 1 = 40%, etc.
        float reduction = 0.9F - (amplifier * 0.2F);
        return Math.max(0.1F, reduction); // Minimum 10% movement
    }

    /**
     * This event fires after an ability is used, allowing us to modify the entity's movement
     */
    @SubscribeEvent
    public static void onAbilityUse(AbilityUseEvent.Post event) {
        LivingEntity entity = event.getEntityLiving();
        IAbility ability = event.getAbility();

        // Get the effect from registry
        Effect effect = getWeakenedMovementEffect();
        if (effect == null) {
            return;
        }

        // Check if entity has the weakened movement effect
        EffectInstance effectInstance = entity.getEffect(effect);
        if (effectInstance == null) {
            return;
        }

        // Check if the ability is a momentum-based ability
        if (isMomentumAbility(ability)) {
            // Get the current movement
            Vector3d movement = entity.getDeltaMovement();

            // Apply the reduction multiplier
            float multiplier = getMovementMultiplier(effectInstance.getAmplifier());

            // Reduce the movement
            entity.setDeltaMovement(
                    movement.x * multiplier,
                    movement.y * multiplier,
                    movement.z * multiplier
            );
        }
    }

    /**
     * Checks if an ability is a momentum-based ability that should be affected
     */
    private static boolean isMomentumAbility(IAbility ability) {
        String abilityName = ability.getDisplayName().getString();

        // Check for Geppo-like abilities
        if (abilityName.equals("Geppo") ||
                abilityName.equals("Cheappo") ||
                abilityName.equals("Skywalk")) {
            return true;
        }

        // Check for Soru-like abilities
        if (abilityName.equals("Soru")) {
            return true;
        }

        // Check by class type as fallback
        if (ability instanceof GeppoAbility || ability instanceof SoruAbility) {
            return true;
        }

        // Check for abilities in the GEPPO_LIKE pool
        try {
            Class<?> abilityClass = ability.getClass();
            // Use reflection to check if it's a Geppo-like ability
            if (abilityClass.getSimpleName().contains("Geppo") ||
                    abilityClass.getSimpleName().contains("Skywalk") ||
                    abilityClass.getSimpleName().contains("Cheappo")) {
                return true;
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }

        return false;
    }

    /**
     * Alternative method: Apply movement reduction every tick if entity has the effect
     * This ensures the effect persists even after ability use
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();

        // Get the effect from registry
        Effect effect = getWeakenedMovementEffect();
        if (effect == null) {
            return;
        }

        if (!entity.level.isClientSide && ModifiedHumanHelper.hasSuitActive(entity)) {
            entity.addEffect(new EffectInstance(effect, 5, 1, false, false));
        }

        // Check if entity has the weakened movement effect
        EffectInstance effectInstance = entity.getEffect(effect);
        if (effectInstance == null) {
            return;
        }

        // Only apply if entity is moving with significant momentum
        Vector3d movement = entity.getDeltaMovement();
        double movementMagnitude = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        // If entity is moving fast (likely from an ability), apply reduction
        if (movementMagnitude > 0.8) {
            float multiplier = getMovementMultiplier(effectInstance.getAmplifier());

            entity.setDeltaMovement(
                    movement.x * multiplier,
                    movement.y * multiplier,
                    movement.z * multiplier
            );
        }
    }
}
