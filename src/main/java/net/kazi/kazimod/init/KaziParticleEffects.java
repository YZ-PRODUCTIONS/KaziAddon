package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import net.kazi.kazimod.particles.DismantleParticleEffect;
import net.kazi.kazimod.particles.WindParticleEffect;
import net.kazi.kazimod.particles.SpiderwebCleaveParticleEffect;
import net.kazi.kazimod.particles.BakugoParticleEffect;
import net.kazi.kazimod.particles.FugaParticleEffect;
import net.kazi.kazimod.particles.WindGustParticleEffect;
import net.kazi.kazimod.particles.GreenTornadoParticleEffect;

public class KaziParticleEffects {

    public static final RegistryObject<ParticleEffect<?>> DISMANTLE =
            KaziRegistry.registerParticleEffect("dismantle", DismantleParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> WIND =
            KaziRegistry.registerParticleEffect("wind", WindParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> SPIDERWEB_CLEAVE =
            KaziRegistry.registerParticleEffect("spiderweb_cleave", SpiderwebCleaveParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> BAKUGO =
            KaziRegistry.registerParticleEffect("bakugo", BakugoParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> FUGA =
            KaziRegistry.registerParticleEffect("fuga", FugaParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> WIND_GUST =
            KaziRegistry.registerParticleEffect("wind_gust", WindGustParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> GREEN_TORNADO =
            KaziRegistry.registerParticleEffect("green_tornado", GreenTornadoParticleEffect::new);

    public static void register(IEventBus eventBus) {
        KaziRegistry.PARTICLE_EFFECTS.register(eventBus);
    }
}