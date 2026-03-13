package net.kazi.kazimod.init;

import net.kazi.kazimod.entities.TimeBubbleEntity;
import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.kazi.kazimod.entities.WhiteTornadoEntity;
import net.kazi.kazimod.entities.projectiles.FugaProjectile;
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
                            .sized(3.0F, 4.0F)
                            .clientTrackingRange(10)
                            .build("malevolent_shrine"));

    public static final RegistryObject<EntityType<FugaProjectile>> FUGA =
            ENTITY_TYPES.register("fuga", () ->
                    EntityType.Builder.<FugaProjectile>of(FugaProjectile::new, EntityClassification.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .build("fuga"));


    public static final RegistryObject<EntityType<TimeBubbleEntity>> TIME_BUBBLE =
            ENTITY_TYPES.register("time_bubble", () ->
                    EntityType.Builder.<TimeBubbleEntity>of(TimeBubbleEntity::new, EntityClassification.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .noSave()
                            .noSummon()
                            .build("time_bubble"));

    public static final RegistryObject<EntityType<WhiteTornadoEntity>> WHITE_TORNADO =
            ENTITY_TYPES.register("white_tornado", () ->
                    EntityType.Builder.<WhiteTornadoEntity>of(WhiteTornadoEntity::new, EntityClassification.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .noSave()
                            .noSummon()
                            .build("white_tornado"));
}