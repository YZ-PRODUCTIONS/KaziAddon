package net.kazi.kazimod.init;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.MOD)
public class KaziMorphs {


    @SubscribeEvent
    public static void registerMorphs(RegistryEvent.Register<MorphInfo> event) {
        event.getRegistry().registerAll(

        );
    }

    public static void register(IEventBus eventBus) {
    }
}
