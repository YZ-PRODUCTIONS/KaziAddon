package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.events.DomainClashManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class DomainClashTickHandler {
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) DomainClashManager.tick();
    }
}
