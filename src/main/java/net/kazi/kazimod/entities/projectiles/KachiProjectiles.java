package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.pixelatedw.mineminenomi.models.abilities.SphereModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.MOD)
public class KachiProjectiles {

    public static final RegistryObject<EntityType<CruelSunProjectile>> CRUEL_SUN =
            WyRegistry.registerEntityType("Cruel Sun", () ->
                    WyRegistry.createEntityType(CruelSunProjectile::new)
                            .fireImmune()
                            .sized(2.0F, 2.0F)
                            .build("kazimod:cruel_sun"));

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        // Sun-coloured: deep orange-gold (R=1.0, G=0.75, B=0.1)
        RenderingRegistry.registerEntityRenderingHandler(
                (EntityType) CRUEL_SUN.get(),
                (new AbilityProjectileRenderer.Factory(new SphereModel()))
                        .setColor(1.0F, 0.75F, 0.1F, 1.0F)
                        .setScale(10.0)   // slightly smaller than Entei's 15.0
                        .setGlowing()
        );
    }
}