package net.kazi.kazimod.init;


import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "kazimod",
        bus = Mod.EventBusSubscriber.Bus.MOD
)

public class KaziParticleTypes {




    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
        ParticleManager manager = Minecraft.getInstance().particleEngine;





    }





    public static void register(IEventBus eventBus) {
        KaziRegistry.PARTICLE_TYPES.register(eventBus);
    }
}
