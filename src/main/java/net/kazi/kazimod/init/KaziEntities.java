package net.kazi.kazimod.init;

import net.kazi.kazimod.entities.boss.rebelus.TrueFormRebelusBossEntity;

import net.kazi.kazimod.entities.*;
import net.kazi.kazimod.entities.boss.bakugo.BakugoBossEntity;
import net.kazi.kazimod.entities.boss.aizen.AizenBossEntity;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.kazi.kazimod.entities.boss.law.LawBossEntity;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.kazi.kazimod.entities.boss.sunjinwoo.SunJinWooBossEntity;
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

    public static final RegistryObject<EntityType<GateOfBabylonEntity>> GATE_OF_BABYLON =
            ENTITY_TYPES.register("gate_of_babylon", () ->
                    EntityType.Builder.<GateOfBabylonEntity>of(GateOfBabylonEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(10).setUpdateInterval(1)
                            .noSummon().noSave().build("gate_of_babylon"));

    public static final RegistryObject<EntityType<BabylonWeaponEntity>> BABYLON_WEAPON =
            ENTITY_TYPES.register("babylon_weapon", () ->
                    EntityType.Builder.<BabylonWeaponEntity>of(BabylonWeaponEntity::new, EntityClassification.MISC)
                            .sized(0.6F, 0.6F).clientTrackingRange(8).setUpdateInterval(1)
                            .noSummon().noSave().build("babylon_weapon"));

    public static final RegistryObject<EntityType<BabylonImpactEntity>> BABYLON_IMPACT =
            ENTITY_TYPES.register("babylon_impact", () ->
                    EntityType.Builder.<BabylonImpactEntity>of(BabylonImpactEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(8).setUpdateInterval(20)
                            .noSummon().noSave().build("babylon_impact"));

    public static final RegistryObject<EntityType<EnkiduVfxEntity>> ENKIDU_VFX =
            ENTITY_TYPES.register("enkidu_vfx", () ->
                    EntityType.Builder.<EnkiduVfxEntity>of(EnkiduVfxEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(10).setUpdateInterval(2)
                            .noSummon().noSave().build("enkidu_vfx"));

    public static final RegistryObject<EntityType<EaVfxEntity>> EA_VFX =
            ENTITY_TYPES.register("ea_vfx", () ->
                    EntityType.Builder.<EaVfxEntity>of(EaVfxEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(12).setUpdateInterval(1)
                            .noSummon().noSave().build("ea_vfx"));

    public static final RegistryObject<EntityType<ReplicatedSwordEntity>> REPLICATED_SWORD =
            ENTITY_TYPES.register("replicated_sword", () ->
                    EntityType.Builder.<ReplicatedSwordEntity>of(ReplicatedSwordEntity::new, EntityClassification.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(96).setUpdateInterval(1)
                            .noSummon().noSave().build("replicated_sword"));

    public static final RegistryObject<EntityType<GaeBolgVfxEntity>> GAE_BOLG_VFX =
            ENTITY_TYPES.register("gae_bolg_vfx", () ->
                    EntityType.Builder.<GaeBolgVfxEntity>of(GaeBolgVfxEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(128).setUpdateInterval(1)
                            .noSummon().noSave().build("gae_bolg_vfx"));

    public static final RegistryObject<EntityType<KamaVfxEntity>> KAMA_VFX =
            ENTITY_TYPES.register("kama_vfx", () ->
                    EntityType.Builder.<KamaVfxEntity>of(KamaVfxEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).setUpdateInterval(1)
                            .noSummon().noSave().build("kama_vfx"));

    public static final RegistryObject<EntityType<EnteiBlastEntity>> ENTEI_BLAST =
            ENTITY_TYPES.register("entei_blast", () ->
                    EntityType.Builder.<EnteiBlastEntity>of(EnteiBlastEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).setUpdateInterval(1)
                            .noSummon().noSave().build("entei_blast"));

    public static final RegistryObject<EntityType<KokuVfxEntity>> KOKU_VFX =
            ENTITY_TYPES.register("koku_vfx", () ->
                    EntityType.Builder.<KokuVfxEntity>of(KokuVfxEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).setUpdateInterval(1)
                            .noSummon().noSave().build("koku_vfx"));

    public static final RegistryObject<EntityType<ZushiVfxEntity>> ZUSHI_VFX =
            ENTITY_TYPES.register("zushi_vfx", () ->
                    EntityType.Builder.<ZushiVfxEntity>of(ZushiVfxEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(16).setUpdateInterval(1)
                            .noSummon().noSave().build("zushi_vfx"));

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

    public static final RegistryObject<EntityType<CaladbolgProjectile>> CALADBOLG =
            ENTITY_TYPES.register("caladbolg", () ->
                    EntityType.Builder.<CaladbolgProjectile>of(CaladbolgProjectile::new, EntityClassification.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(128)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("caladbolg"));

    public static final RegistryObject<EntityType<CaladbolgImpactEntity>> CALADBOLG_IMPACT =
            ENTITY_TYPES.register("caladbolg_impact", () ->
                    EntityType.Builder.<CaladbolgImpactEntity>of(CaladbolgImpactEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(128)
                            .setUpdateInterval(20)
                            .noSummon()
                            .noSave()
                            .build("caladbolg_impact"));

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

    public static final RegistryObject<EntityType<EmbeddedItemDisplayEntity>> EMBEDDED_ITEM_DISPLAY =
            ENTITY_TYPES.register("embedded_item_display", () ->
                    EntityType.Builder.<EmbeddedItemDisplayEntity>of(EmbeddedItemDisplayEntity::new,
                                    EntityClassification.MISC)
                            .sized(0.01F, 0.01F)
                            .clientTrackingRange(128)
                            .setUpdateInterval(20)
                            .noSummon()
                            .noSave()
                            .build("embedded_item_display"));

    public static final RegistryObject<EntityType<RealityMarbleWeaponEntity>> REALITY_MARBLE_WEAPON =
            ENTITY_TYPES.register("reality_marble_weapon", () ->
                    EntityType.Builder.<RealityMarbleWeaponEntity>of(RealityMarbleWeaponEntity::new,
                                    EntityClassification.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(128)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("reality_marble_weapon"));

    public static final RegistryObject<EntityType<RealityMarbleGearEntity>> REALITY_MARBLE_GEAR =
            ENTITY_TYPES.register("reality_marble_gear", () ->
                    EntityType.Builder.<RealityMarbleGearEntity>of(RealityMarbleGearEntity::new,
                                    EntityClassification.MISC)
                            .sized(2.0F, 2.0F)
                            .clientTrackingRange(128)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("reality_marble_gear"));

    public static final RegistryObject<EntityType<UnlimitedLostWorksEntity>> UNLIMITED_LOST_WORKS =
            ENTITY_TYPES.register("unlimited_lost_works", () ->
                    EntityType.Builder.<UnlimitedLostWorksEntity>of(UnlimitedLostWorksEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(64).setUpdateInterval(1)
                            .noSummon().noSave().build("unlimited_lost_works"));

    public static final RegistryObject<EntityType<EnhancementLightEntity>> ENHANCEMENT_LIGHT =
            ENTITY_TYPES.register("enhancement_light", () ->
                    EntityType.Builder.<EnhancementLightEntity>of(EnhancementLightEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(128)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("enhancement_light"));

    public static final RegistryObject<EntityType<RhoAiasEntity>> RHO_AIAS =
            ENTITY_TYPES.register("rho_aias", () ->
                    EntityType.Builder.<RhoAiasEntity>of(RhoAiasEntity::new, EntityClassification.MISC)
                            .sized(5.0F, 5.0F)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .noSave()
                            .build("rho_aias"));

    public static final RegistryObject<EntityType<CoinProjectile>> CASINO_COIN =
            ENTITY_TYPES.register("casino_coin", () ->
                    EntityType.Builder.<CoinProjectile>of(CoinProjectile::new, EntityClassification.MISC)
                            .sized(0.5f, 0.5f)
                            .build("casino_coin"));

    public static final RegistryObject<EntityType<CasinoChipProjectile>> CASINO_CHIP =
            ENTITY_TYPES.register("casino_chip",()->EntityType.Builder.<CasinoChipProjectile>of(CasinoChipProjectile::new,EntityClassification.MISC)
                    .sized(.5F,.5F).build("casino_chip"));

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

    public static final RegistryObject<EntityType<SunJinWooBossEntity>> SUN_JIN_WOO_BOSS =
            ENTITY_TYPES.register("sun_jin_woo_boss", () ->
                    EntityType.Builder.<SunJinWooBossEntity>of(SunJinWooBossEntity::new, EntityClassification.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("sun_jin_woo_boss"));

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

    // ── Saku Saku no Mi projectiles ───────────────────────────────────────
    public static final RegistryObject<EntityType<PetalBladeProjectile>> PETAL_BLADE =
            ENTITY_TYPES.register("petal_blade", () ->
                    EntityType.Builder.<PetalBladeProjectile>of(PetalBladeProjectile::new, EntityClassification.MISC)
                            .sized(0.3f, 0.3f)
                            .clientTrackingRange(64)
                            .build("petal_blade"));

    // ── Akuma Akuma no Mi projectiles ─────────────────────────────────────
    public static final RegistryObject<EntityType<HellblazeProjectile>> HELLBLAZE_PROJECTILE =
            ENTITY_TYPES.register("hellblaze_projectile", () ->
                    EntityType.Builder.<HellblazeProjectile>of(HellblazeProjectile::new, EntityClassification.MISC)
                            .sized(4.0f, 4.0f)
                            .clientTrackingRange(64)
                            .build("hellblaze_projectile"));

    public static final RegistryObject<EntityType<SlashWaveProjectile>> SLASH_WAVE =
            ENTITY_TYPES.register("slash_wave", () ->
                    EntityType.Builder.<SlashWaveProjectile>of(SlashWaveProjectile::new, EntityClassification.MISC)
                            .sized(10.0f, 1.5f)
                            .clientTrackingRange(64)
                            .build("slash_wave"));

    public static final RegistryObject<EntityType<DarkSpearProjectile>> DARK_SPEAR =
            ENTITY_TYPES.register("dark_spear", () ->
                    EntityType.Builder.<DarkSpearProjectile>of(DarkSpearProjectile::new, EntityClassification.MISC)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(64)
                            .build("dark_spear"));

    // ── Nagi Nagi no Mi projectiles ──────────────────────────────────────
    public static final RegistryObject<EntityType<BloodRiverProjectile>> BLOOD_RIVER_PROJECTILE =
            ENTITY_TYPES.register("blood_river_projectile", () ->
                    EntityType.Builder.<BloodRiverProjectile>of(BloodRiverProjectile::new, EntityClassification.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("blood_river_projectile"));

    public static final RegistryObject<EntityType<SilentSliceProjectile>> SILENT_SLICE =
            ENTITY_TYPES.register("silent_slice", () ->
                    EntityType.Builder.<SilentSliceProjectile>of(SilentSliceProjectile::new, EntityClassification.MISC)
                            .sized(6.0f, 1.0f)
                            .clientTrackingRange(64)
                            .build("silent_slice"));

    public static final RegistryObject<EntityType<SilentDeathProjectile>> SILENT_DEATH =
            ENTITY_TYPES.register("silent_death", () ->
                    EntityType.Builder.<SilentDeathProjectile>of(SilentDeathProjectile::new, EntityClassification.MISC)
                            .sized(2.0f, 2.0f)
                            .clientTrackingRange(64)
                            .build("silent_death"));

    // ── Fuwa Fuwa no Mi projectiles ─────────────────────────────────────
    public static final RegistryObject<EntityType<ItemKaitenReworkedProjectile>> ITEM_KAITEN =
            ENTITY_TYPES.register("item_kaiten", () ->
                    EntityType.Builder.<ItemKaitenReworkedProjectile>of(ItemKaitenReworkedProjectile::new, EntityClassification.MISC)
                            .sized(ItemKaitenReworkedProjectile.ENTITY_WIDTH, ItemKaitenReworkedProjectile.ENTITY_HEIGHT)
                            .clientTrackingRange(10)
                            .setUpdateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("kazimod:item_kaiten"));

    // ── Yami Yami no Mi projectiles ──────────────────────────────────────
    public static final RegistryObject<EntityType<DarkMatterReworkProjectile>> DARK_MATTER_PROJECTILE =
            ENTITY_TYPES.register("dark_matter_projectile", () ->
                    EntityType.Builder.<DarkMatterReworkProjectile>of(DarkMatterReworkProjectile::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("dark_matter_projectile"));
    public static final RegistryObject<EntityType<net.kazi.kazimod.entities.GuraVfxEntity>> GURA_VFX =
            ENTITY_TYPES.register("gura_vfx", () -> EntityType.Builder.<net.kazi.kazimod.entities.GuraVfxEntity>of(
                    net.kazi.kazimod.entities.GuraVfxEntity::new, EntityClassification.MISC)
                    .sized(1, 1).clientTrackingRange(16).setUpdateInterval(2).noSave().noSummon().build("gura_vfx"));
    public static final RegistryObject<EntityType<TrueFormRebelusBossEntity>> TRUE_FORM_REBELUS_BOSS =
            ENTITY_TYPES.register("true_form_rebelus_boss", () ->
                    EntityType.Builder.<TrueFormRebelusBossEntity>of(TrueFormRebelusBossEntity::new, EntityClassification.MONSTER)
                            .sized(1.1f, 3.25f)
                            .clientTrackingRange(80)
                            .noSave()
                            .build("true_form_rebelus_boss"));
    public static final RegistryObject<EntityType<TripelTBatProjectile>> TRIPLE_T_BAT_PROJECTILE =
            ENTITY_TYPES.register("triple_t_bat_projectile", () ->
                    EntityType.Builder.<TripelTBatProjectile>of(TripelTBatProjectile::new, EntityClassification.MISC)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("triple_t_bat_projectile"));
public static final RegistryObject<EntityType<LightArrowProjectile>> LIGHT_ARROW_PROJECTILE =
            ENTITY_TYPES.register("light_arrow_projectile", () ->
                    EntityType.Builder.<LightArrowProjectile>of(LightArrowProjectile::new, EntityClassification.MISC)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("light_arrow_projectile"));
    public static final RegistryObject<EntityType<LightPortalProjectile>> LIGHT_PORTAL_PROJECTILE =
            ENTITY_TYPES.register("light_portal_projectile", () ->
                    EntityType.Builder.<LightPortalProjectile>of(LightPortalProjectile::new, EntityClassification.MISC)
                            .sized(1.5f, 3.5f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .build("light_portal_projectile"));
}
