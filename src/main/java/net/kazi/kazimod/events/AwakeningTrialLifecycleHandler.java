package net.kazi.kazimod.events;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AwakeningTrialLifecycleHandler {

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        AwakeningEssenceDeathHandler.cleanupAllAwakeningTrials(event.getServer());
    }
}
