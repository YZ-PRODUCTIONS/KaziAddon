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
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.PawModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(bus = Bus.MOD)
public class NikyuReworkProjectiles {

    public static final RegistryObject<EntityType<PadHoReworkProjectile>> PAD_HO =
            WyRegistry.registerEntityType("Pad Ho Rework", () ->
                    WyRegistry.createEntityType(PadHoReworkProjectile::new)
                            .sized(2.5F, 2.5F)
                            .build("kazimod:pad_ho_rework"));

    public static final RegistryObject<EntityType<UrsusShockReworkProjectile>> URSUS_SHOCK =
            WyRegistry.registerEntityType("Ursus Shock Rework", () ->
                    WyRegistry.createEntityType(UrsusShockReworkProjectile::new)
                            .sized(1.0F, 1.0F)
                            .build("kazimod:ursus_shock_rework"));

    public NikyuReworkProjectiles() {}

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) PAD_HO.get(),
                (new AbilityProjectileRenderer.Factory(new PawModel()))
                        .setColor("#F8F8FF33").setScale(2.0));
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) URSUS_SHOCK.get(),
                (new AbilityProjectileRenderer.Factory(new PawModel()))
                        .setColor("#F8F8FF33").setScale(0.6));
    }
}