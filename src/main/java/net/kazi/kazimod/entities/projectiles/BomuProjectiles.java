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

@EventBusSubscriber(
        bus = Bus.MOD
)
public class BomuProjectiles {

    public static final RegistryObject<EntityType<ClusterProjectile>> CLUSTER = WyRegistry.registerEntityType(
            "Cluster",
            () -> WyRegistry.createEntityType(ClusterProjectile::new)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .build("kazimod:cluster")
    );

    public BomuProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) CLUSTER.get(),
                (new AbilityProjectileRenderer.Factory(new CubeModel())).setScale((double) 0.0F, (double) 0.0F, (double) 0.0F)
        );
    }
}