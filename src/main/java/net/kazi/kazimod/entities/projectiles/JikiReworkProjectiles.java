//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.pixelatedw.mineminenomi.renderers.abilities.EmptyRenderer;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(
        bus = Bus.MOD
)
public class JikiReworkProjectiles {
    public static final RegistryObject<EntityType<GenocideRaidReworkProjectile>> GENOCIDE_RAID =
            WyRegistry.registerEntityType("Genocide Raid Rework", () ->
                    WyRegistry.createEntityType(GenocideRaidReworkProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:genocide_raid_rework"));

    public static final RegistryObject<EntityType<PunkCrossReworkProjectile>> PUNK_CROSS =
            WyRegistry.registerEntityType("Punk Cross Rework", () ->
                    WyRegistry.createEntityType(PunkCrossReworkProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:punk_cross_rework"));

    public JikiReworkProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler((EntityType)GENOCIDE_RAID.get(), new EmptyRenderer.Factory());
        RenderingRegistry.registerEntityRenderingHandler((EntityType)PUNK_CROSS.get(), new EmptyRenderer.Factory());
    }
}
