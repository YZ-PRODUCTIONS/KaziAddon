package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.abilities.ServerUtility.BootBoost;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod")
public class BootBoostFallHandler {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!entity.getPersistentData().getBoolean(BootBoost.NO_FALL_TAG)) {
            return;
        }

        event.setCanceled(true);
        entity.fallDistance = 0.0F;
        entity.getPersistentData().remove(BootBoost.NO_FALL_TAG);
    }
}
