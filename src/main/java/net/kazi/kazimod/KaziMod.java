package net.kazi.kazimod;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.renderers.entities.TimeBubbleRenderer;
import net.kazi.kazimod.events.handlers.SizeRenderHandler;
import net.kazi.kazimod.init.*;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziItemModelProps;
import net.kazi.kazimod.models.projectiles.FugaProjectileRenderer;
import net.kazi.kazimod.init.KaziPacketHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.kazi.kazimod.init.KaziModPools;
import net.kazi.kazimod.events.handlers.EntitySizeHandler;
import net.kazi.kazimod.events.handlers.SizeEffectHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(KaziMod.MODID)
public class KaziMod {

    public static final String MODID = "kazimod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public KaziMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        KaziAbilities.register(modBus);
        KaziItems.register();
        KaziRegistry.ITEMS.register(modBus);
        KaziParticleEffects.register(modBus);
        KaziParticleTypes.register(modBus);
        KaziEffects.register(modBus);
        KaziSounds.register(modBus);
        KaziEntities.ENTITY_TYPES.register(modBus);
        KaziModPools.init();
        KaziAttributes.ATTRIBUTES.register(modBus);

        modBus.addListener(this::commonSetup);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(this::clientSetup);
        });

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EntitySizeHandler());
        MinecraftForge.EVENT_BUS.register(new SizeEffectHandler());
        MinecraftForge.EVENT_BUS.register(new SizeRenderHandler());

        LOGGER.info("kazimod constructed");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        KaziPacketHandler.register();
    }

    private void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.FUGA.get(), FugaProjectileRenderer::new
        );
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.TIME_BUBBLE.get(), TimeBubbleRenderer::new
        );
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.WHITE_TORNADO.get(),
                manager -> new net.minecraft.client.renderer.entity.EntityRenderer<net.kazi.kazimod.entities.WhiteTornadoEntity>(manager) {
                    @Override
                    public net.minecraft.util.ResourceLocation getTextureLocation(net.kazi.kazimod.entities.WhiteTornadoEntity entity) {
                        return new net.minecraft.util.ResourceLocation("kazimod", "textures/entity/empty.png");
                    }
                }
        );
        KaziItemModelProps.register();
        KaziAnimations.clientInit();
    }
}