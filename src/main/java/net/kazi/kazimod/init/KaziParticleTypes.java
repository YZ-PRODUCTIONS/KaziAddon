package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;

public class KaziParticleTypes {

    public static final RegistryObject<ParticleType<SimpleParticleData>> DISMANTLE =
            KaziRegistry.registerParticleType("dismantle_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> WIND =
            KaziRegistry.registerParticleType("wind_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> SPIDERWEB_CLEAVE =
            KaziRegistry.registerParticleType("spiderweb_cleave_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> BAKUGO =
            KaziRegistry.registerParticleType("bakugo_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> FUGA =
            KaziRegistry.registerParticleType("fuga_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> GREEN_SWEEP =
            KaziRegistry.registerParticleType("green_sweep_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> GREEN_TORNADO =
            KaziRegistry.registerParticleType("green_tornado_particle", SimpleParticleData::new);

    public static void register(IEventBus eventBus) {
        KaziRegistry.PARTICLE_TYPES.register(eventBus);
    }
}