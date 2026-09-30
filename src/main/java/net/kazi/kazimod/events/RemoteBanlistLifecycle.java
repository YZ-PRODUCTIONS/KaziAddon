package net.kazi.kazimod.events;

import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.security.RemoteUuidBanlist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

/** Starts and stops the remote banlist worker with each server instance. */
@Mod.EventBusSubscriber(modid = KaziMod.MODID)
public final class RemoteBanlistLifecycle {

    private RemoteBanlistLifecycle() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        RemoteUuidBanlist.start();
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        RemoteUuidBanlist.stop();
    }
}
