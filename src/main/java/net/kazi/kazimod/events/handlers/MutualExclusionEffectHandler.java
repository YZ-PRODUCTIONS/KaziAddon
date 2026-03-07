package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MutualExclusionEffectHandler {

    @SubscribeEvent
    public static void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) event.getEntityLiving();
        Effect incoming = event.getPotionEffect().getEffect();

        // Block WEAKENED_MOVEMENT if MINIATURIZED is active
        if (incoming == KaziEffects.WEAKENED_MOVEMENT.get() && entity.hasEffect(KaziEffects.MINIATURIZED.get())) {
            event.setResult(Event.Result.DENY);
            return;
        }

        // Block MINIATURIZED if WEAKENED_MOVEMENT is active
        if (incoming == KaziEffects.MINIATURIZED.get() && entity.hasEffect(KaziEffects.WEAKENED_MOVEMENT.get())) {
            event.setResult(Event.Result.DENY);
        }
    }
}