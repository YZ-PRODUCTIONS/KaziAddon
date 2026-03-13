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
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.MiniHollowModel;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.NegativeHollowModel;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.TokuHollowModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(
        bus = Bus.MOD
)
public class HoroReworkProjectiles {
    // FIX: Changed namespace from "mineminenomi:negative_hollow" etc. to "kazimod:..."
    // to avoid "Duplicate registration" conflicts with the base mod's registered entities.
    public static final RegistryObject<EntityType<NegativeHollowReworkProjectile>> NEGATIVE_HOLLOW_REWORK =
            WyRegistry.registerEntityType("Negative Hollow Rework", () ->
                    WyRegistry.createEntityType(NegativeHollowReworkProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:negative_hollow_rework"));

    public static final RegistryObject<EntityType<MiniHollowReworkProjectile>> MINI_HOLLOW_REWORK =
            WyRegistry.registerEntityType("Mini Hollow Rework", () ->
                    WyRegistry.createEntityType(MiniHollowReworkProjectile::new)
                            .sized(0.5F, 0.5F)
                            .build("kazimod:mini_hollow_rework"));

    public static final RegistryObject<EntityType<TokuHollowReworkProjectile>> TOKU_HOLLOW_REWORK =
            WyRegistry.registerEntityType("Toku Hollow Rework", () ->
                    WyRegistry.createEntityType(TokuHollowReworkProjectile::new)
                            .sized(7.0F, 7.0F)
                            .build("kazimod:toku_hollow_rework"));

    public HoroReworkProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) NEGATIVE_HOLLOW_REWORK.get(),
                (new AbilityProjectileRenderer.Factory(new NegativeHollowModel()))
                        .setTexture("negativehollow").setAlpha(120).setScale((double) 2.0F));

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) MINI_HOLLOW_REWORK.get(),
                (new AbilityProjectileRenderer.Factory(new MiniHollowModel()))
                        .setColor("#F8F8FF").setAlpha(120));

        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) TOKU_HOLLOW_REWORK.get(),
                (new AbilityProjectileRenderer.Factory(new TokuHollowModel()))
                        .setTexture("tokuhollow").setAlpha(120).setScale((double) 4.0F));
    }
}