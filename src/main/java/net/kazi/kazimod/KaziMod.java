package net.kazi.kazimod;

import net.kazi.kazimod.abilities.Nusu.NusuEvents;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.client.renderers.GojoBossRenderer;
import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.effects.FlashbangEffect;
import net.kazi.kazimod.events.AwakeningEssenceDeathHandler;
import net.kazi.kazimod.events.AwakeningAbilityLoginFix;
import net.kazi.kazimod.events.BossWaterCancelHandler;
import net.kazi.kazimod.events.handlers.*;
import net.kazi.kazimod.init.*;
import net.kazi.kazimod.renderers.entities.VegapunkTraderRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.kazi.kazimod.renderers.entities.InfiniteVoidBarrierRenderer;
import net.kazi.kazimod.client.renderers.SukunaBossRenderer;
import net.kazi.kazimod.client.renderers.LuffyBossRenderer;

import java.util.function.Consumer;

@Mod("kazimod")
public class KaziMod {

    public static final String MODID = "kazimod";
    public static final Logger LOGGER = LogManager.getLogger();

    public KaziMod() {
        // Register config before anything else so values are available during setup
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, KaziConfig.SPEC, "kazimod.toml");

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        KaziAbilities.register(modEventBus);
        KaziItems.register();
        KaziRegistry.ITEMS.register(modEventBus);
        KaziItems2.ITEMS.register(modEventBus);
        KaziParticleEffects.register(modEventBus);
        KaziParticleTypes.register(modEventBus);
        KaziEffects.register(modEventBus);
        KaziSounds.register(modEventBus);
        KaziEntities.ENTITY_TYPES.register(modEventBus);
        KaziModPools.init();
        KaziAttributes.ATTRIBUTES.register(modEventBus);
        KaziBlocks.BLOCKS.register(modEventBus);
        NusuEvents.register();

        modEventBus.addListener((Consumer<FMLCommonSetupEvent>) this::commonSetup);

        modEventBus.addListener(KaziEntityAttributes::onAttributeCreate);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                modEventBus.addListener((Consumer<FMLClientSetupEvent>) this::clientSetup)
        );

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EntitySizeHandler());
        MinecraftForge.EVENT_BUS.register(new SizeEffectHandler());
        MinecraftForge.EVENT_BUS.register(new KaziSpawnRulesHandler());
        MinecraftForge.EVENT_BUS.register(new AwakeningEssenceDeathHandler());
        MinecraftForge.EVENT_BUS.register(new AwakeningAbilityLoginFix());
        MinecraftForge.EVENT_BUS.register(new RecipeRemovalHandler());
        MinecraftForge.EVENT_BUS.register(new BossWaterCancelHandler());

        LOGGER.info("kazimod constructed");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        KaziPacketHandler.register();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.GOJO_BOSS.get(),
                GojoBossRenderer::new
        );

        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.SUKUNA_BOSS.get(),
                SukunaBossRenderer::new
        );

        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.VEGAPUNK_TRADER.get(),
                VegapunkTraderRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.AWAKENING_BARRIER.get(),
                InfiniteVoidBarrierRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.LUFFY_BOSS.get(),
                LuffyBossRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.SHADOW_DOPPELMAN.get(),
                manager -> new BipedRenderer<ShadowDoppelmanEntity, BipedModel<ShadowDoppelmanEntity>>(manager, new BipedModel<>(0.0F), 0.5F) {
                    @Override
                    protected void scale(ShadowDoppelmanEntity entity, MatrixStack matrixStack, float partialTicks) {
                        float shadowsUsed = entity.getShadows();
                        float scale = shadowsUsed > 0.0F ? 1.0F + shadowsUsed / 6.0F : 1.0F;
                        matrixStack.scale(scale, scale, scale);
                    }

                    @Override
                    public ResourceLocation getTextureLocation(ShadowDoppelmanEntity entity) {
                        return new ResourceLocation("mineminenomi", "textures/models/doppelman.png");
                    }
                }
        );

        MinecraftForge.EVENT_BUS.register(FlashbangEffect.class);
        MinecraftForge.EVENT_BUS.register(new SizeRenderHandler());
    }
}
