package net.kazi.kazimod.mahoraga;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class MahoragaClient {
    public static void init(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent e) -> {
            RenderingRegistry.registerEntityRenderingHandler(MahoragaFeatures.MAHORAGA.get(), MahoragaRenderer::new);
            RenderingRegistry.registerEntityRenderingHandler(MahoragaFeatures.PROJECTILE.get(), MahoragaProjectileRenderer::new);
        });
    }
    private MahoragaClient() {}
}
