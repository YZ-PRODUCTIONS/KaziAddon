package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.models.projectiles.RedRocProjectileRenderer;
import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.pixelatedw.mineminenomi.models.abilities.EntityArmModel;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(
        bus = Bus.MOD
)
public class GomuReworkProjectiles {

    public static final RegistryObject<EntityType<GomuGomuNoRedRocProjectile>> GOMU_GOMU_NO_RED_ROC = WyRegistry.registerEntityType("Gomu Gomu Red Roc", () -> WyRegistry.createEntityType(GomuGomuNoRedRocProjectile::new).sized(3.0F, 3.0F).build("kazimod:gomu_gomu_no_red_roc"));

    public static final RegistryObject<EntityType<GomuGomuNoBajrangGunReworkProjectile>> GOMU_GOMU_NO_BAJRANG_GUN_REWORK = WyRegistry.registerEntityType("Gomu Gomu Bajrang Gun Rework", () -> WyRegistry.createEntityType(GomuGomuNoBajrangGunReworkProjectile::new).sized(30.0F, 30.0F).build("kazimod:gomu_gomu_no_bajrang_gun_rework"));

    public static final RegistryObject<EntityType<GomuGomuNoStarGunProjectile>> GOMU_GOMU_NO_STAR_GUN = WyRegistry.registerEntityType("Gomu Gomu Star Gun", () -> WyRegistry.createEntityType(GomuGomuNoStarGunProjectile::new).sized(1.0F, 1.0F).build("kazimod:gomu_gomu_no_star_gun"));

    public static final RegistryObject<EntityType<GomuGomuNoDawnGatlingProjectile>> GOMU_GOMU_DAWN_GATLING = WyRegistry.registerEntityType("Gomu Gomu Dawn Gatling", () -> WyRegistry.createEntityType(GomuGomuNoDawnGatlingProjectile::new).sized(1.0F, 1.0F).build("kazimod:gomu_gomu_dawn_gatling"));

    public GomuReworkProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_NO_RED_ROC.get(),
                (new RedRocProjectileRenderer.Factory(new EntityArmModel(), new EntityArmModel()))
                        .setStretchScale(3.1, 3.1)
                        .setScale(15.0F, 15.0F, 10.0F)
                        .setPlayerTexture()
        );

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_NO_BAJRANG_GUN_REWORK.get(),
                (new RedRocProjectileRenderer.Factory(new EntityArmModel(), new EntityArmModel()))
                        .setStretchScale(3.1, 3.1)
                        .setScale(150.0F, 150.0F, 10.0F)
                        .setPlayerTexture()
        );

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_NO_STAR_GUN.get(),
                (new RedRocProjectileRenderer.Factory(new EntityArmModel(), new EntityArmModel()))

        );

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) GOMU_GOMU_DAWN_GATLING.get(),
                (new RedRocProjectileRenderer.Factory(new EntityArmModel(), new EntityArmModel()))
        );

    }
}