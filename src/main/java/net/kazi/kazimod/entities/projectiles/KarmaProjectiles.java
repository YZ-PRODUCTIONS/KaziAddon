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
import xyz.pixelatedw.mineminenomi.models.abilities.SphereModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@EventBusSubscriber(
        bus = Bus.MOD
)
public class KarmaProjectiles {
    public static final RegistryObject<EntityType<KarmaExplosionProjectile>> KARMA_EXPLOSION = WyRegistry.registerEntityType("Karma Explosion", () -> WyRegistry.createEntityType(KarmaExplosionProjectile::new).fireImmune().sized(3.0F, 3.0F).build("mineminenomi:karma_explosion"));

    public KarmaProjectiles() {
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler((EntityType)KARMA_EXPLOSION.get(), (new AbilityProjectileRenderer.Factory(new SphereModel())).setColor(0.8F, 0.0F, 0.0F, 1.0F).setScale((double)15.0F).setGlowing());
    }
}