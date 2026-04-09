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
    public static final RegistryObject<ParticleType<SimpleParticleData>> GOJORED =
            KaziRegistry.registerParticleType("gojored_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> GOJO_BLUE =
            KaziRegistry.registerParticleType("gojo_blue", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> GOJO_BLUE_PROJECTILE =
            KaziRegistry.registerParticleType("gojo_blue_projectile", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> GOJO_PURPLE =
            KaziRegistry.registerParticleType("gojo_purple", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> GOJO_PURPLE_GROWING =
            KaziRegistry.registerParticleType("gojo_purple_growing", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> INFINITE_VOID =
            KaziRegistry.registerParticleType("infinite_void", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> INFINITE_VOID_STREAK =
            KaziRegistry.registerParticleType("infinite_void_streak", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> INFINITE_VOID_STREAK_PINK =
            KaziRegistry.registerParticleType("infinite_void_streak_pink", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_0 = KaziRegistry.registerParticleType("lucky_slot_0_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_1 = KaziRegistry.registerParticleType("lucky_slot_1_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_2 = KaziRegistry.registerParticleType("lucky_slot_2_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_3 = KaziRegistry.registerParticleType("lucky_slot_3_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_4 = KaziRegistry.registerParticleType("lucky_slot_4_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_5 = KaziRegistry.registerParticleType("lucky_slot_5_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_6 = KaziRegistry.registerParticleType("lucky_slot_6_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_7 = KaziRegistry.registerParticleType("lucky_slot_7_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_8 = KaziRegistry.registerParticleType("lucky_slot_8_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> LUCKY_SLOT_9 = KaziRegistry.registerParticleType("lucky_slot_9_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> COIN =
            KaziRegistry.registerParticleType("coin_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> CASINO_CHIP =
            KaziRegistry.registerParticleType("casino_chip_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> PLAYING_CARD =
            KaziRegistry.registerParticleType("playing_card_particle", SimpleParticleData::new);
    public static final RegistryObject<ParticleType<SimpleParticleData>> PETAL_BLADE =
            KaziRegistry.registerParticleType("petal_blade_particle", SimpleParticleData::new);

    public static void register(IEventBus eventBus) {
        KaziRegistry.PARTICLE_TYPES.register(eventBus);
    }
}