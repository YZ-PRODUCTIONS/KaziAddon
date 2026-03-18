package net.kazi.kazimod.init;

import net.kazi.kazimod.particles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.particles.SimpleParticle;

@Mod.EventBusSubscriber(
        modid = "kazimod",
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class KaziParticleTypesClient {

    @SubscribeEvent
    public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
        ParticleManager manager = Minecraft.getInstance().particleEngine;

        manager.register((ParticleType) KaziParticleTypes.DISMANTLE.get(),                  new SimpleParticle.Factory(KaziResources.Dismantle));
        manager.register((ParticleType) KaziParticleTypes.WIND.get(),                       new SimpleParticle.Factory(KaziResources.Wind));
        manager.register((ParticleType) KaziParticleTypes.SPIDERWEB_CLEAVE.get(),           new SimpleParticle.Factory(KaziResources.SpiderwebCleave));
        manager.register((ParticleType) KaziParticleTypes.BAKUGO.get(),                     new SimpleParticle.Factory(KaziResources.Bakugo));
        manager.register((ParticleType) KaziParticleTypes.FUGA.get(),                       new SimpleParticle.Factory(KaziResources.Fuga));
        manager.register((ParticleType) KaziParticleTypes.GREEN_SWEEP.get(),                new SimpleParticle.Factory(KaziResources.GreenSweep));
        manager.register((ParticleType) KaziParticleTypes.GREEN_TORNADO.get(),              new SimpleParticle.Factory(KaziResources.GreenTornado));

        // IParticleMetaFactory (sprite atlas) — same as GojoRed which works
        manager.register((ParticleType) KaziParticleTypes.GOJORED.get(),                    GojoRedParticle.Factory::new);
        manager.register((ParticleType) KaziParticleTypes.GOJO_BLUE.get(),                  GojoBlueParticle.Factory::new);
        manager.register((ParticleType) KaziParticleTypes.GOJO_BLUE_PROJECTILE.get(),       GojoBlueProjectileParticle.Factory::new);
        manager.register((ParticleType) KaziParticleTypes.GOJO_PURPLE.get(),                GojoPurpleParticle.Factory::new);
        manager.register((ParticleType) KaziParticleTypes.GOJO_PURPLE_GROWING.get(),        GojoPurpleGrowingParticle.Factory::new);
        manager.register((ParticleType) KaziParticleTypes.INFINITE_VOID.get(),              InfiniteVoidParticle.Factory::new);


    }
}