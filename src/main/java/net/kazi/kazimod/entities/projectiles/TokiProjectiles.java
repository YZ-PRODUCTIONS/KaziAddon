package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(
        bus = Bus.MOD
)
public class TokiProjectiles {

    public static final RegistryObject<EntityType<TimeTheftProjectile>> TIME_THEFT_BEAM =
            WyRegistry.registerEntityType("Time Theft Beam", () ->
                    WyRegistry.createEntityType(TimeTheftProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:time_theft_beam"));

    public TokiProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        // TimeTheftProjectile is a LightningEntity-based beam — no model renderer needed.
        // Add future Toki projectile renderers here.
    }
}