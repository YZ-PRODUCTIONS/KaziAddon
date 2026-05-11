package net.kazi.kazimod;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.renderers.BakugoBossRenderer;
import net.kazi.kazimod.client.renderers.AizenBossRenderer;
import net.kazi.kazimod.client.renderers.GojoBossRenderer;
import net.kazi.kazimod.client.renderers.LawBossRenderer;
import net.kazi.kazimod.client.renderers.LuffyBossRenderer;
import net.kazi.kazimod.client.renderers.SunJinWooBossRenderer;
import net.kazi.kazimod.client.renderers.SukunaBossRenderer;
import net.kazi.kazimod.effects.FlashbangEffect;
import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.kazi.kazimod.events.handlers.SizeRenderHandler;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.renderers.entities.InfiniteVoidBarrierRenderer;
import net.kazi.kazimod.renderers.entities.VegapunkTraderRenderer;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.FistModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;

public final class KaziClientInit {

    private KaziClientInit() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(KaziClientInit::clientSetup);
    }

    private static void clientSetup(final FMLClientSetupEvent event) {
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
                KaziEntities.BAKUGO_BOSS.get(),
                BakugoBossRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.LAW_BOSS.get(),
                LawBossRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.AIZEN_BOSS.get(),
                AizenBossRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                KaziEntities.SUN_JIN_WOO_BOSS.get(),
                SunJinWooBossRenderer::new
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
