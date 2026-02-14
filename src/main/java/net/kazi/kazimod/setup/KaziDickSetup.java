package net.kazi.kazimod.setup;

import net.kazi.kazimod.network.KaziNetwork;
import net.minecraft.advancements.CriteriaTriggers;
import net.kazi.kazimod.KaziMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.kazi.kazimod.client.ModKeybinds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = KaziMod.MODID, bus = Bus.MOD)
public class KaziDickSetup {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void enqueueIMC(InterModEnqueueEvent event) {
        LOGGER.info("piraterecruits IMC Enqueue");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onStitch(TextureStitchEvent.Pre event) {
        LOGGER.info("piraterecruits Texture Stitch (Pre)");
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("piraterecruits Common Setup Starting");

        event.enqueueWork(() -> {

        });
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("piraterecruits Client Setup Starting");
        event.enqueueWork(() -> {});


    }
}