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
        // Custom fruit lists can bypass native Hana ability initialization. Its
        // renderer subscriber still runs, so initialize the entity holders before registries fire.
        xyz.pixelatedw.mineminenomi.entities.projectiles.hana.HanaProjectiles.HANDS.getId();
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
        net.kazi.kazimod.agito.AgitoFeatures.init(modEventBus);
        net.kazi.kazimod.shikigami.ShikigamiFeatures.init(modEventBus);
        net.kazi.kazimod.saiyan.SaiyanFeatures.init(modEventBus);
        net.kazi.kazimod.petal.PetalFeatures.init(modEventBus);
        net.kazi.kazimod.decay.DecayFeatures.init(modEventBus);
        net.kazi.kazimod.alchemy.AlchemyFeatures.init(modEventBus);
        net.kazi.kazimod.blood.BloodFeatures.init(modEventBus);
        net.kazi.kazimod.tusk.TuskFeatures.init(modEventBus);
        net.kazi.kazimod.gengar.GengarFeatures.init(modEventBus);
        net.kazi.kazimod.leopard.LeopardFeatures.init(modEventBus);
        net.kazi.kazimod.hermano.HermanoFeatures.init(modEventBus);
        net.kazi.kazimod.longbob.LongbobFeatures.init(modEventBus);
        net.kazi.kazimod.jerome.JeromeFeatures.init(modEventBus);
        net.kazi.kazimod.ghoul.GhoulFeatures.init(modEventBus);

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
