package net.kazi.kazimod.init;

import net.kazi.kazimod.entities.*;
import net.kazi.kazimod.entities.boss.bakugo.BakugoBossEntity;
import net.kazi.kazimod.entities.boss.aizen.AizenBossEntity;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.kazi.kazimod.entities.boss.law.LawBossEntity;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.kazi.kazimod.entities.boss.sukuna.SukunaBossEntity;
import net.kazi.kazimod.entities.projectiles.*;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class KaziEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");

    public static final RegistryObject<EntityType<MalevolentShrineEntity>> MALEVOLENT_SHRINE =
            ENTITY_TYPES.register("malevolent_shrine", () ->
                    EntityType.Builder.<MalevolentShrineEntity>of(MalevolentShrineEntity::new, EntityClassification.MISC)
                            .sized(3.0f, 4.0f)
                            .clientTrackingRange(10)
                            .build("malevolent_shrine"));

    public static final RegistryObject<EntityType<FugaProjectile>> FUGA =
            ENTITY_TYPES.register("fuga", () ->
                    EntityType.Builder.<FugaProjectile>of(FugaProjectile::new, EntityClassification.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .build("fuga"));

    public static final RegistryObject<EntityType<ShockWilleProjectile>> SHOCK_WILLE =
            ENTITY_TYPES.register("shock_wille", () ->
                    EntityType.Builder.<ShockWilleProjectile>of(ShockWilleProjectile::new, EntityClassification.MISC)
                            .sized(2.5f, 2.5f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("shock_wille"));

    public static final RegistryObject<EntityType<PunctureWilleProjectile>> PUNCTURE_WILLE =
            ENTITY_TYPES.register("puncture_wille", () ->
                    EntityType.Builder.<PunctureWilleProjectile>of(PunctureWilleProjectile::new, EntityClassification.MISC)
                            .sized(2.75f, 2.75f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("puncture_wille"));

    public static final RegistryObject<EntityType<TimeBubbleEntity>> TIME_BUBBLE =
            ENTITY_TYPES.register("time_bubble", () ->
                    EntityType.Builder.<TimeBubbleEntity>of(TimeBubbleEntity::new, EntityClassification.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .build("time_bubble"));

    public static final RegistryObject<EntityType<KurohitsugiEntity>> KUROHITSUGI =
            ENTITY_TYPES.register("kurohitsugi", () ->
                    EntityType.Builder.<KurohitsugiEntity>of(KurohitsugiEntity::new, EntityClassification.MISC)
                            .sized(2.5f, 5.0f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .build("kurohitsugi"));

    public static final RegistryObject<EntityType<KurohitsugiSpikeEntity>> KUROHITSUGI_SPIKE =
            ENTITY_TYPES.register("kurohitsugi_spike", () ->
                    EntityType.Builder.<KurohitsugiSpikeEntity>of(KurohitsugiSpikeEntity::new, EntityClassification.MISC)
                            .sized(1.0f, 4.0f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .build("kurohitsugi_spike"));

    public static final RegistryObject<EntityType<GoryutenmetsuDragonEntity>> GORYUTENMETSU_DRAGON =
            ENTITY_TYPES.register("goryutenmetsu_dragon", () ->
                    EntityType.Builder.<GoryutenmetsuDragonEntity>of(GoryutenmetsuDragonEntity::new, EntityClassification.MISC)
                            .sized(3.5f, 3.5f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("goryutenmetsu_dragon"));

    public static final RegistryObject<EntityType<GoryutenmetsuDragonEntity>> MERA_FLAME_DRAGON =
            ENTITY_TYPES.register("mera_flame_dragon", () ->
                    EntityType.Builder.<GoryutenmetsuDragonEntity>of(GoryutenmetsuDragonEntity::new, EntityClassification.MISC)
                            .sized(5.0f, 5.0f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("mera_flame_dragon"));

    public static final RegistryObject<EntityType<WhiteTornadoEntity>> WHITE_TORNADO =
            ENTITY_TYPES.register("white_tornado", () ->
                    EntityType.Builder.<WhiteTornadoEntity>of(WhiteTornadoEntity::new, EntityClassification.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("white_tornado"));

    public static final RegistryObject<EntityType<InfiniteVoidBarrierEntity>> INFINITE_VOID_BARRIER =
            ENTITY_TYPES.register("infinite_void_barrier", () ->
                    EntityType.Builder.<InfiniteVoidBarrierEntity>of(InfiniteVoidBarrierEntity::new, EntityClassification.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(64)
                            .noSummon()
                            .noSave()
                            .build("infinite_void_barrier"));

    public static final RegistryObject<EntityType<InfiniteVoidBarrierEntity>> AWAKENING_BARRIER =
            ENTITY_TYPES.register("awakening_barrier", () ->
                    EntityType.Builder.<InfiniteVoidBarrierEntity>of(InfiniteVoidBarrierEntity::new, EntityClassification.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(256)
                            .noSummon()
                            .noSave()
                            .build("awakening_barrier"));

    public static final RegistryObject<EntityType<CoinProjectile>> CASINO_COIN =
            ENTITY_TYPES.register("casino_coin", () ->
                    EntityType.Builder.<CoinProjectile>of(CoinProjectile::new, EntityClassification.MISC)
                            .sized(0.5f, 0.5f)
                            .build("casino_coin"));

    public static final RegistryObject<EntityType<DiceProjectile>> CASINO_DICE =
            ENTITY_TYPES.register("casino_dice", () ->
                    EntityType.Builder.<DiceProjectile>of(DiceProjectile::new, EntityClassification.MISC)
                            .sized(0.6f, 0.6f)
                            .build("casino_dice"));

    public static final RegistryObject<EntityType<PlayingCardProjectile>> CASINO_PLAYING_CARD =
            ENTITY_TYPES.register("casino_playing_card", () ->
                    EntityType.Builder.<PlayingCardProjectile>of(PlayingCardProjectile::new, EntityClassification.MISC)
                            .sized(0.8f, 0.05f)
                            .build("casino_playing_card"));

    public static final RegistryObject<EntityType<ClusterProjectile>> CLUSTER =
            ENTITY_TYPES.register("cluster", () ->
                    EntityType.Builder.<ClusterProjectile>of(ClusterProjectile::new, EntityClassification.MISC)
                            .sized(0.5f, 0.5f)
                            .fireImmune()
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("cluster"));

    public static final RegistryObject<EntityType<ReworkedHikenProjectile>> REWORKED_HIKEN =
            ENTITY_TYPES.register("reworked_hiken", () ->
                    EntityType.Builder.<ReworkedHikenProjectile>of(ReworkedHikenProjectile::new, EntityClassification.MISC)
                            .sized(8.0f, 8.0f)
                            .fireImmune()
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("reworked_hiken"));

    public static final RegistryObject<EntityType<HellfireBirdProjectile>> HELLFIRE_BIRD =
            ENTITY_TYPES.register("hellfire_bird", () ->
                    EntityType.Builder.<HellfireBirdProjectile>of(HellfireBirdProjectile::new, EntityClassification.MISC)
                            .sized(1.6f, 1.6f)
                            .fireImmune()
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("hellfire_bird"));

    public static final RegistryObject<EntityType<GiantDiceEntity>> GIANT_DICE =
            ENTITY_TYPES.register("giant_dice", () ->
                    EntityType.Builder.<GiantDiceEntity>of(GiantDiceEntity::new, EntityClassification.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(3)
                            .build("giant_dice"));

    public static final RegistryObject<EntityType<GojoBossEntity>> GOJO_BOSS =
            ENTITY_TYPES.register("gojo_boss", () ->
                    EntityType.Builder.<GojoBossEntity>of(GojoBossEntity::new, EntityClassification.MONSTER)
                            .sized(1.0f, 2.5f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("gojo_boss"));

    public static final RegistryObject<EntityType<SukunaBossEntity>> SUKUNA_BOSS =
            ENTITY_TYPES.register("sukuna_boss", () ->
                    EntityType.Builder.<SukunaBossEntity>of(SukunaBossEntity::new, EntityClassification.MONSTER)
                            .sized(1.0f, 2.5f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("sukuna_boss"));

    public static final RegistryObject<EntityType<LuffyBossEntity>> LUFFY_BOSS =
            ENTITY_TYPES.register("luffy_boss", () ->
                    EntityType.Builder.<LuffyBossEntity>of(LuffyBossEntity::new, EntityClassification.MONSTER)
                            .sized(1.0f, 2.5f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("luffy_boss"));

    public static final RegistryObject<EntityType<BakugoBossEntity>> BAKUGO_BOSS =
            ENTITY_TYPES.register("bakugo_boss", () ->
                    EntityType.Builder.<BakugoBossEntity>of(BakugoBossEntity::new, EntityClassification.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("bakugo_boss"));

    public static final RegistryObject<EntityType<LawBossEntity>> LAW_BOSS =
            ENTITY_TYPES.register("law_boss", () ->
                    EntityType.Builder.<LawBossEntity>of(LawBossEntity::new, EntityClassification.MONSTER)
                            .sized(1.0f, 2.2f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("law_boss"));

    public static final RegistryObject<EntityType<AizenBossEntity>> AIZEN_BOSS =
            ENTITY_TYPES.register("aizen_boss", () ->
                    EntityType.Builder.<AizenBossEntity>of(AizenBossEntity::new, EntityClassification.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("aizen_boss"));

    public static final RegistryObject<EntityType<VegapunkTraderEntity>> VEGAPUNK_TRADER =
            ENTITY_TYPES.register("vegapunk_trader", () ->
                    EntityType.Builder.<VegapunkTraderEntity>of(VegapunkTraderEntity::new, EntityClassification.MISC)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .build("vegapunk_trader"));

    public static final RegistryObject<EntityType<ShadowDoppelmanEntity>> SHADOW_DOPPELMAN =
            ENTITY_TYPES.register("shadow_doppelman", () ->
                    EntityType.Builder.<ShadowDoppelmanEntity>of(ShadowDoppelmanEntity::new, EntityClassification.MONSTER)
                            .sized(0.8f, 2.2f)
                            .clientTrackingRange(64)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("shadow_doppelman"));
}
