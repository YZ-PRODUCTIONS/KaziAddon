package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
        bus = Bus.MOD
)
public class BomuProjectiles {

    public BomuProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.CLUSTER.get(),
                manager -> new net.kazi.kazimod.bomu.BomuProjectileRenderer<>(manager, 1.2F)
        );
    }
}
