package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber // registers this class on the FORGE event bus automatically
public class MiniaturizedEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!entity.hasEffect(KaziEffects.MINIATURIZED.get())) return;

        event.setAmount(event.getAmount() * 1.5F);
    }
}