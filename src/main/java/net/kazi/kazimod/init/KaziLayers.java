package net.kazi.kazimod.init;

import net.kazi.kazimod.events.handlers.DemonWingsLayer;
import net.kazi.kazimod.events.handlers.GearFifthSmokeLayer;
import net.kazi.kazimod.events.handlers.GigantSmokeLayer;
import net.kazi.kazimod.events.handlers.PairedSwordsLayer;
import net.kazi.kazimod.events.handlers.ImagineBreakerAuraLayer;
import net.kazi.kazimod.events.handlers.InnerDragonsArmLayer;
import net.kazi.kazimod.events.handlers.AstralFlightLayer;
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
                renderer.addLayer(new DemonWingsLayer<>(renderer));
                renderer.addLayer(new ImagineBreakerAuraLayer(renderer));
                renderer.addLayer(new InnerDragonsArmLayer(renderer));
                renderer.addLayer(new AstralFlightLayer<>(renderer));
                PairedSwordsLayer.install(renderer);

            }
        });
    }
}
