package net.kazi.kazimod.init;

import net.kazi.kazimod.entities.*;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
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

    public static final RegistryObject<EntityType<TimeBubbleEntity>> TIME_BUBBLE =
            ENTITY_TYPES.register("time_bubble", () ->
                    EntityType.Builder.<TimeBubbleEntity>of(TimeBubbleEntity::new, EntityClassification.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .setUpdateInterval(1)
                            .noSummon()
                            .build("time_bubble"));

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
                            .build("gojo_boss"));

    public static final RegistryObject<EntityType<SukunaBossEntity>> SUKUNA_BOSS =
            ENTITY_TYPES.register("sukuna_boss", () ->
                    EntityType.Builder.<SukunaBossEntity>of(SukunaBossEntity::new, EntityClassification.MONSTER)
                            .sized(1.0f, 2.5f)
                            .clientTrackingRange(80)
                            .build("sukuna_boss"));

    public static final RegistryObject<EntityType<LuffyBossEntity>> LUFFY_BOSS =
            ENTITY_TYPES.register("luffy_boss", () ->
                    EntityType.Builder.<LuffyBossEntity>of(LuffyBossEntity::new, EntityClassification.MONSTER)
                            .sized(1.0f, 2.5f)
                            .clientTrackingRange(80)
                            .build("luffy_boss"));

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
