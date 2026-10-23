package net.kazi.kazimod.preserved.sahur;

import net.minecraft.entity.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;

public final class SahurEffects {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
    public static final RegistryObject<EntityType<SahurLightEntity>> LIGHT = TYPES.register("triple_t_heavenly_light", () ->
            EntityType.Builder.<SahurLightEntity>of(SahurLightEntity::new, EntityClassification.MISC)
                    .sized(1, 1).clientTrackingRange(12).updateInterval(2).noSave().build("triple_t_heavenly_light"));
    public static void init(IEventBus bus) {
        TYPES.register(bus);
        MinecraftForge.EVENT_BUS.addListener(HeavenlyShield::impact);
    }
}
