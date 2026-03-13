package net.kazi.kazimod.init;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.kazi.kazimod.renderers.abilities.WeatherCloudReworkRenderer;
import net.kazi.kazimod.renderers.abilities.WhiteTornadoRenderer;
import net.kazi.kazimod.renderers.entities.MalevolentShrineRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KaziRenderers {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.MALEVOLENT_SHRINE.get(),
                new MalevolentShrineRenderer.Factory()
        );
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.WHITE_TORNADO.get(),
                new WhiteTornadoRenderer.Factory()
        );
        RenderingRegistry.registerEntityRenderingHandler(
                (net.minecraft.entity.EntityType<WeatherCloudReworkEntity>)
                        ForgeRegistries.ENTITIES.getValue(new ResourceLocation("cartaddon", "weather_cloud_rework")),
                new WeatherCloudReworkRenderer.Factory()
        );
    }
}