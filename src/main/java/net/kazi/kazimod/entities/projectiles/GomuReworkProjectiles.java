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
import xyz.pixelatedw.mineminenomi.models.abilities.EntityArmModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.renderers.abilities.StretchingProjectileRenderer;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(
        bus = Bus.MOD
)
public class GomuReworkProjectiles {

    public static final RegistryObject<EntityType<GomuGomuNoStarGunProjectile>> GOMU_GOMU_NO_STAR_GUN =
            WyRegistry.registerEntityType("Gomu Gomu no Star Gun", () ->
                    WyRegistry.createEntityType(GomuGomuNoStarGunProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:gomu_gomu_no_star_gun"));

    public static final RegistryObject<EntityType<GomuGomuNoDawnGatlingProjectile>> GOMU_GOMU_DAWN_GATLING =
            WyRegistry.registerEntityType("Gomu Gomu no Dawn Gatling", () ->
                    WyRegistry.createEntityType(GomuGomuNoDawnGatlingProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:gomu_gomu_no_dawn_gatling"));

    public static final RegistryObject<EntityType<GomuGomuNoDawnRocketProjectile>> GOMU_GOMU_NO_DAWN_ROCKET =
            WyRegistry.registerEntityType("Gomu Gomu no Dawn Rocket", () ->
                    WyRegistry.createEntityType(GomuGomuNoDawnRocketProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:gomu_gomu_no_dawn_rocket"));

    // Kaminari: sized like BoloBreath (3x3), fire-immune since it's an energy beam guide
    public static final RegistryObject<EntityType<GomuGomuNoKaminariProjectile>> GOMU_GOMU_NO_KAMINARI =
            WyRegistry.registerEntityType("Gomu Gomu no Kaminari", () ->
                    WyRegistry.createEntityType(GomuGomuNoKaminariProjectile::new)
                            .sized(3.0F, 3.0F)
                            .fireImmune()
                            .build("kazimod:gomu_gomu_no_kaminari"));

    public GomuReworkProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_NO_STAR_GUN.get(),
                (new StretchingProjectileRenderer.Factory(new EntityArmModel()))
                        .setStretchScale(3.1, 3.1, (double) 10.0F)
                        .setPlayerTexture());

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_DAWN_GATLING.get(),
                (new StretchingProjectileRenderer.Factory(new EntityArmModel()))
                        .setStretchScale(3.1, 3.1, (double) 10.0F)
                        .setPlayerTexture());

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_NO_DAWN_ROCKET.get(),
                (new StretchingProjectileRenderer.Factory(new EntityArmModel()))
                        .setStretchScale(3.1, 3.1, (double) 10.0F)
                        .setPlayerTexture());

        // Kaminari guide projectile — invisible (scale 0), identical to BoloBreath's setup
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_NO_KAMINARI.get(),
                (new AbilityProjectileRenderer.Factory(new CubeModel())).setScale((double) 0.0F));
    }
}