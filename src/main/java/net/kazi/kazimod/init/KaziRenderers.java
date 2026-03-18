package net.kazi.kazimod.init;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.kazi.kazimod.entities.projectiles.CasinoProjectiles;
import net.kazi.kazimod.entities.projectiles.CoinProjectile;
import net.kazi.kazimod.entities.projectiles.PlayingCardProjectile;
import net.kazi.kazimod.models.projectiles.DiceProjectileRenderer;
import net.kazi.kazimod.renderers.abilities.WeatherCloudReworkRenderer;
import net.kazi.kazimod.renderers.abilities.WhiteTornadoRenderer;
import net.kazi.kazimod.renderers.entities.*;
import net.kazi.kazimod.models.projectiles.FugaProjectileRenderer;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziItemModelProps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.pixelatedw.mineminenomi.particles.SimpleParticle;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KaziRenderers {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

        // ── Entity renderers previously in KaziMod.clientSetup ───────────────
        // These were the source of the dedicated server crash when registered
        // as inline anonymous classes directly inside KaziMod. They are now
        // safe here because this class is CLIENT-only via the annotation above.

        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.FUGA.get(),
                FugaProjectileRenderer::new);

        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.TIME_BUBBLE.get(),
                TimeBubbleRenderer::new);

        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.WHITE_TORNADO.get(),
                new WhiteTornadoRenderer.Factory());

        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.INFINITE_VOID_BARRIER.get(),
                InfiniteVoidBarrierRenderer::new);

        KaziItemModelProps.register();
        KaziAnimations.clientInit();

        // ── Other entity renderers ────────────────────────────────────────────
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.MALEVOLENT_SHRINE.get(),
                new MalevolentShrineRenderer.Factory());

        RenderingRegistry.registerEntityRenderingHandler(
                (net.minecraft.entity.EntityType<WeatherCloudReworkEntity>)
                        ForgeRegistries.ENTITIES.getValue(new ResourceLocation("cartaddon", "weather_cloud_rework")),
                new WeatherCloudReworkRenderer.Factory());

        // ── Casino projectile renderers ───────────────────────────────────────
        RenderingRegistry.registerEntityRenderingHandler(
                CasinoProjectiles.DICE.get(),
                DiceProjectileRenderer::new);

        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.GIANT_DICE.get(),
                GiantDiceRenderer::new);

        RenderingRegistry.registerEntityRenderingHandler(
                CasinoProjectiles.COIN.get(),
                manager -> new net.minecraft.client.renderer.entity.EntityRenderer<CoinProjectile>(manager) {
                    @Override
                    public ResourceLocation getTextureLocation(CoinProjectile entity) {
                        return new ResourceLocation("kazimod", "textures/particle/coin.png");
                    }
                });

        RenderingRegistry.registerEntityRenderingHandler(
                CasinoProjectiles.PLAYING_CARD.get(),
                new PlayingCardRenderer.Factory());

        // ── Particle engine registrations ─────────────────────────────────────
        ParticleManager pm = Minecraft.getInstance().particleEngine;

        pm.register(KaziParticleTypes.LUCKY_SLOT_0.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot0));
        pm.register(KaziParticleTypes.LUCKY_SLOT_1.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot1));
        pm.register(KaziParticleTypes.LUCKY_SLOT_2.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot2));
        pm.register(KaziParticleTypes.LUCKY_SLOT_3.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot3));
        pm.register(KaziParticleTypes.LUCKY_SLOT_4.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot4));
        pm.register(KaziParticleTypes.LUCKY_SLOT_5.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot5));
        pm.register(KaziParticleTypes.LUCKY_SLOT_6.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot6));
        pm.register(KaziParticleTypes.LUCKY_SLOT_7.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot7));
        pm.register(KaziParticleTypes.LUCKY_SLOT_8.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot8));
        pm.register(KaziParticleTypes.LUCKY_SLOT_9.get(),  new SimpleParticle.Factory(KaziResources.LuckySlot9));
        pm.register(KaziParticleTypes.COIN.get(),          new SimpleParticle.Factory(KaziResources.Coin));
        pm.register(KaziParticleTypes.CASINO_CHIP.get(),   new SimpleParticle.Factory(KaziResources.CasinoChip));
        pm.register(KaziParticleTypes.PLAYING_CARD.get(),  new SimpleParticle.Factory(KaziResources.PlayingCard));
    }
}