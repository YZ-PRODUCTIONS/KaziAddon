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
import xyz.pixelatedw.mineminenomi.models.abilities.CubeModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(bus = Bus.MOD)
public class GojoProjectiles {

    public static final RegistryObject<EntityType<RedProjectile>> RED = WyRegistry.registerEntityType(
            "Red",
            () -> WyRegistry.createEntityType(RedProjectile::new)
                    .sized(2.0F, 2.0F)
                    .fireImmune()
                    .build("kazimod:red")
    );

    public static final RegistryObject<EntityType<MaxOutputRedProjectile>> MAX_OUTPUT_RED = WyRegistry.registerEntityType(
            "MaxOutputRed",
            () -> WyRegistry.createEntityType(MaxOutputRedProjectile::new)
                    .sized(2.0F, 2.0F)
                    .fireImmune()
                    .build("kazimod:max_output_red")
    );

    public static final RegistryObject<EntityType<LapseBlueProjectile>> LAPSE_BLUE = WyRegistry.registerEntityType(
            "LapseBlue",
            () -> WyRegistry.createEntityType(LapseBlueProjectile::new)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .build("kazimod:lapse_blue")
    );

    public static final RegistryObject<EntityType<HollowNukeProjectile>> HOLLOW_NUKE = WyRegistry.registerEntityType(
            "HollowNuke",
            () -> WyRegistry.createEntityType(HollowNukeProjectile::new)
                    .sized(1.0F, 1.0F)
                    .fireImmune()
                    .build("kazimod:hollow_nuke")
    );

    public static final RegistryObject<EntityType<HollowPurpleProjectile>> HOLLOW_PURPLE = WyRegistry.registerEntityType(
            "HollowPurple",
            () -> WyRegistry.createEntityType(HollowPurpleProjectile::new)
                    .sized(1.0F, 1.0F)
                    .fireImmune()
                    .build("kazimod:hollow_purple")
    );

    public GojoProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) RED.get(),
                (new AbilityProjectileRenderer.Factory(new CubeModel()))
                        .setScale((double) 0.0F, (double) 0.0F, (double) 0.0F)
        );
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) MAX_OUTPUT_RED.get(),
                (new AbilityProjectileRenderer.Factory(new CubeModel()))
                        .setScale((double) 0.0F, (double) 0.0F, (double) 0.0F)
        );

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) LAPSE_BLUE.get(),
                (new AbilityProjectileRenderer.Factory(new CubeModel()))
                        .setScale((double) 0.0F, (double) 0.0F, (double) 0.0F)
        );

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) HOLLOW_NUKE.get(),
                (new AbilityProjectileRenderer.Factory(new CubeModel()))
                        .setScale((double) 0.0F, (double) 0.0F, (double) 0.0F)
        );

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) HOLLOW_PURPLE.get(),
                (new AbilityProjectileRenderer.Factory(new CubeModel()))
                        .setScale((double) 0.0F, (double) 0.0F, (double) 0.0F)
        );
    }
}