package net.kazi.kazimod.init;

import net.kazi.kazimod.events.handlers.GearFifthSmokeLayer;
import net.kazi.kazimod.events.handlers.GigantSmokeLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KaziLayers {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // getSkinMap() returns both "default" and "slim" PlayerRenderers
            for (PlayerRenderer renderer : Minecraft.getInstance()
                    .getEntityRenderDispatcher()
                    .getSkinMap()
                    .values()) {
                renderer.addLayer(new GearFifthSmokeLayer<>(renderer));
                renderer.addLayer(new GigantSmokeLayer<>(renderer));

            }
        });
    }
}