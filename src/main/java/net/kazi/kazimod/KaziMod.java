package net.kazi.kazimod;

import net.kazi.kazimod.abilities.Nusu.NusuEvents;
import net.kazi.kazimod.events.handlers.EntitySizeHandler;
import net.kazi.kazimod.events.handlers.SizeEffectHandler;
import net.kazi.kazimod.events.handlers.SizeRenderHandler;
import net.kazi.kazimod.effects.FlashbangEffect;
import net.kazi.kazimod.init.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("kazimod")
public class KaziMod {

    public static final String MODID = "kazimod";
    public static final Logger LOGGER = LogManager.getLogger();

    public KaziMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        KaziAbilities.register(modEventBus);
        KaziItems.register();
        KaziRegistry.ITEMS.register(modEventBus);
        KaziParticleEffects.register(modEventBus);
        KaziParticleTypes.register(modEventBus);
        KaziEffects.register(modEventBus);
        KaziSounds.register(modEventBus);
        KaziEntities.ENTITY_TYPES.register(modEventBus);
        KaziModPools.init();
        KaziAttributes.ATTRIBUTES.register(modEventBus);
        KaziBlocks.BLOCKS.register(modEventBus);
        NusuEvents.register();

        // commonSetup listener — safe on both sides
        modEventBus.addListener(this::commonSetup);

        // Client-only event registrations — wrapped in DistExecutor so the
        // lambda body is NEVER loaded on the dedicated server.
        // All renderer registration lives in KaziRenderers (annotated CLIENT-only)
        // so we only need to register the non-renderer client handlers here.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(FlashbangEffect.class);
            MinecraftForge.EVENT_BUS.register(new SizeRenderHandler());
        });

        // Both-side Forge event bus registrations
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EntitySizeHandler());
        MinecraftForge.EVENT_BUS.register(new SizeEffectHandler());

        LOGGER.info("kazimod constructed");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        KaziPacketHandler.register();
    }

    // ── NOTE ──────────────────────────────────────────────────────────────────
    // clientSetup() has been REMOVED from this class entirely.
    //
    // The following registrations that were previously in clientSetup() are now
    // all handled by KaziRenderers.onClientSetup() which is annotated with
    // @Mod.EventBusSubscriber(value = Dist.CLIENT) and therefore only ever
    // loaded on the client:
    //
    //   - KaziEntities.FUGA          → FugaProjectileRenderer::new
    //   - KaziEntities.TIME_BUBBLE   → TimeBubbleRenderer::new
    //   - KaziEntities.WHITE_TORNADO → WhiteTornadoRenderer.Factory
    //   - KaziEntities.INFINITE_VOID_BARRIER → InfiniteVoidBarrierRenderer::new
    //   - KaziItemModelProps.register()
    //   - KaziAnimations.clientInit()
    //   - All casino projectile renderers (Dice, Coin, PlayingCard, GiantDice)
    //   - All particle engine registrations
    //
    // The old inline anonymous EntityRenderer for WHITE_TORNADO caused the
    // dedicated server crash because the anonymous class KaziMod$1 extended
    // EntityRenderer (a client-only class) and was part of KaziMod.class,
    // which the server classloader tried to verify — even inside DistExecutor.
    // ─────────────────────────────────────────────────────────────────────────
}