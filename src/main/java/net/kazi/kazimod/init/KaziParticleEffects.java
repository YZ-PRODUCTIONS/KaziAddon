package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.particles.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;


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
    public static final RegistryObject<ParticleEffect<?>> GOJO_RED =
            KaziRegistry.registerParticleEffect("gojo_red", GojoRedParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> GOJO_RED_CHARGE =
            KaziRegistry.registerParticleEffect("gojo_red_charge", GojoRedChargeParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> GOJO_BLUE =
            KaziRegistry.registerParticleEffect("gojo_blue", GojoBlueParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> GOJO_BLUE_PROJECTILE =
            KaziRegistry.registerParticleEffect("gojo_blue_projectile", GojoBlueProjectileParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> GOJO_PURPLE =
            KaziRegistry.registerParticleEffect("gojo_purple", GojoPurpleParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> GOJO_PURPLE_GROWING =
            KaziRegistry.registerParticleEffect("gojo_purple_growing", GojoPurpleGrowingParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> INFINITE_VOID =
            KaziRegistry.registerParticleEffect("infinite_void", InfiniteVoidParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> INFINITE_VOID_STREAK =
            KaziRegistry.registerParticleEffect("infinite_void_streak", InfiniteVoidStreakParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_0 = KaziRegistry.registerParticleEffect("lucky_slot_0", () -> new LuckySlotParticleEffect(0));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_1 = KaziRegistry.registerParticleEffect("lucky_slot_1", () -> new LuckySlotParticleEffect(1));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_2 = KaziRegistry.registerParticleEffect("lucky_slot_2", () -> new LuckySlotParticleEffect(2));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_3 = KaziRegistry.registerParticleEffect("lucky_slot_3", () -> new LuckySlotParticleEffect(3));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_4 = KaziRegistry.registerParticleEffect("lucky_slot_4", () -> new LuckySlotParticleEffect(4));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_5 = KaziRegistry.registerParticleEffect("lucky_slot_5", () -> new LuckySlotParticleEffect(5));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_6 = KaziRegistry.registerParticleEffect("lucky_slot_6", () -> new LuckySlotParticleEffect(6));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_7 = KaziRegistry.registerParticleEffect("lucky_slot_7", () -> new LuckySlotParticleEffect(7));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_8 = KaziRegistry.registerParticleEffect("lucky_slot_8", () -> new LuckySlotParticleEffect(8));
    public static final RegistryObject<ParticleEffect<?>> LUCKY_SLOT_9 = KaziRegistry.registerParticleEffect("lucky_slot_9", () -> new LuckySlotParticleEffect(9));
    public static final RegistryObject<ParticleEffect<?>> COIN =
            KaziRegistry.registerParticleEffect("coin", CoinParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> CASINO_CHIP =
            KaziRegistry.registerParticleEffect("casino_chip", CasinoChipParticleEffect::new);
    public static final RegistryObject<ParticleEffect<?>> PLAYING_CARD =
            KaziRegistry.registerParticleEffect("playing_card", PlayingCardParticleEffect::new);

    public static void register(IEventBus eventBus) {
        KaziRegistry.PARTICLE_EFFECTS.register(eventBus);
    }
}