package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.GeppoAbility;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.SoruAbility;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber
public class EnhancedMovementEventHandler {

    // Entities queued for a one-time boost on the next tick
    private static final Set<LivingEntity> pendingBoost = Collections.newSetFromMap(new WeakHashMap<>());

    private static Effect getEnhancedMovementEffect() {
        return KaziEffects.ENHANCED_MOVEMENT.get();
    }

    private static float getMovementMultiplier(int amplifier) {
        float boost = 0.2F + (amplifier * 0.1F);
        return 1.0F + boost;
    }

    @SubscribeEvent
    public static void onAbilityUse(AbilityUseEvent.Post event) {
        LivingEntity entity = event.getEntityLiving();
        IAbility ability = event.getAbility();

        if (entity == null || ability == null) return;

        Effect effect = getEnhancedMovementEffect();
        if (effect == null) return;
        if (entity.getEffect(effect) == null) return;
        if (!isMomentumAbility(ability)) return;

        pendingBoost.add(entity);
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();

        if (!pendingBoost.remove(entity)) return;

        Effect effect = getEnhancedMovementEffect();
        if (effect == null) return;

        EffectInstance effectInstance = entity.getEffect(effect);
        if (effectInstance == null) return;

        Vector3d movement = entity.getDeltaMovement();
        double magnitude = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        if (magnitude < 0.01) return;

        float multiplier = getMovementMultiplier(effectInstance.getAmplifier());
        entity.setDeltaMovement(
                movement.x * multiplier,
                movement.y * multiplier,
                movement.z * multiplier
        );
    }

    private static boolean isMomentumAbility(IAbility ability) {
        if (ability.getDisplayName() == null) return false;
        String abilityName = ability.getDisplayName().getString();

        if (abilityName.equals("Geppo") ||
                abilityName.equals("Cheappo") ||
                abilityName.equals("Skywalk") ||
                abilityName.equals("Soru")) {
            return true;
        }

        if (ability instanceof GeppoAbility || ability instanceof SoruAbility) {
            return true;
        }

        try {
            String simpleName = ability.getClass().getSimpleName();
            if (simpleName.contains("Geppo") ||
                    simpleName.contains("Skywalk") ||
                    simpleName.contains("Cheappo")) {
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }

        return false;
    }
}