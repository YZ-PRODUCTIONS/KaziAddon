package net.kazi.kazimod.preserved;

import net.kazi.kazimod.init.*;
import net.kazi.kazimod.particles.AngelGlowParticle;
import net.kazi.kazimod.particles.DivineResonanceParticle;
import net.kazi.kazimod.renderers.entities.GuraVfxRenderer;
import net.kazi.kazimod.renderers.entities.projectiles.TripelTBatProjectileRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import xyz.pixelatedw.mineminenomi.models.abilities.CubeModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.particles.SimpleParticle;

public final class PreservedClient {
    public static void init(IEventBus bus) {
        net.kazi.kazimod.worldturtle.WorldTurtleClient.init(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e)->net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(net.kazi.kazimod.kake.KakeVisuals.EFFECT.get(),net.kazi.kazimod.kake.KakeVfxRenderer::new));
        bus.addListener(PreservedClient::setup);
        bus.addListener(PreservedClient::particles);
    }
    private static void setup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(net.kazi.kazimod.preserved.tenki.TenkiEffects.EFFECT.get(),net.kazi.kazimod.preserved.tenki.TenkiRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(net.kazi.kazimod.preserved.sahur.SahurEffects.LIGHT.get(),
                net.kazi.kazimod.preserved.sahur.SahurLightRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(net.kazi.kazimod.preserved.cerberus.CerberusFeatures.EFFECT.get(),
                net.kazi.kazimod.preserved.cerberus.CerberusEffectRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(KaziEntities.GURA_VFX.get(), GuraVfxRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(KaziEntities.TRIPLE_T_BAT_PROJECTILE.get(), TripelTBatProjectileRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(KaziEntities.LIGHT_ARROW_PROJECTILE.get(), net.kazi.kazimod.preserved.sahur.HeavenlyArrowRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(KaziEntities.LIGHT_PORTAL_PROJECTILE.get(), new AbilityProjectileRenderer.Factory(new CubeModel()).setScale(0));
        RenderingRegistry.registerEntityRenderingHandler(KaziEntities.TRUE_FORM_REBELUS_BOSS.get(),
                net.kazi.kazimod.client.renderers.TrueFormRebelusBossRenderer::new);
    }
    private static void particles(ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particleEngine.register(KaziParticleTypes.ANGEL_PARTICLE_1.get(), AngelGlowParticle.Factory::new);
        Minecraft.getInstance().particleEngine.register(KaziParticleTypes.ANGEL_PARTICLE_2.get(), AngelGlowParticle.Factory::new);
        Minecraft.getInstance().particleEngine.register(KaziParticleTypes.DIVINE_RESONANCE.get(), DivineResonanceParticle.Factory::new);
        Minecraft.getInstance().particleEngine.register(KaziParticleTypes.LIGHT_PORTAL.get(), new SimpleParticle.Factory(KaziResources.LightPortal));
    }
}
