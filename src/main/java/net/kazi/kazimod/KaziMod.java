package net.kazi.kazimod;

import net.kazi.kazimod.abilities.Nusu.NusuEvents;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.events.AwakeningEssenceDeathHandler;
import net.kazi.kazimod.events.AwakeningAbilityLoginFix;
import net.kazi.kazimod.events.BossWaterCancelHandler;
import net.kazi.kazimod.events.handlers.*;
import net.kazi.kazimod.init.*;
import net.kazi.kazimod.setup.HakiAbilityInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
        KaziBlocks.ITEMS.register(modEventBus);
        NusuEvents.register();
        net.kazi.kazimod.mammoth.MammothFeatures.init(modEventBus);
        net.kazi.kazimod.mahoraga.MahoragaFeatures.init(modEventBus);

        modEventBus.addListener((Consumer<FMLCommonSetupEvent>) this::commonSetup);

        modEventBus.addListener(KaziEntityAttributes::onAttributeCreate);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KaziClientInit.init(modEventBus));

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EntitySizeHandler());
        MinecraftForge.EVENT_BUS.register(new SizeEffectHandler());
        MinecraftForge.EVENT_BUS.register(new KaziSpawnRulesHandler());
        MinecraftForge.EVENT_BUS.register(new AwakeningEssenceDeathHandler());
        MinecraftForge.EVENT_BUS.register(new AwakeningAbilityLoginFix());
        MinecraftForge.EVENT_BUS.register(new RecipeRemovalHandler());
        MinecraftForge.EVENT_BUS.register(new BossWaterCancelHandler());
        MinecraftForge.EVENT_BUS.register(new BootBoostFallHandler());
        net.kazi.kazimod.preserved.PreservedFeatures.init();
        LOGGER.info("kazimod constructed");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            KaziPacketHandler.register();
            HakiAbilityInjector.inject();
        });
    }
}
