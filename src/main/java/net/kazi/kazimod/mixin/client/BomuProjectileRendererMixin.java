package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.bomu.BomuProjectileRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.bomu.BomuProjectiles;

@Mixin(value=BomuProjectiles.class,remap=false)
public abstract class BomuProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$renderExplosives(FMLClientSetupEvent event, CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(BomuProjectiles.NOSE_FANCY_CANNON.get(),m->new BomuProjectileRenderer<>(m,.32F));
        RenderingRegistry.registerEntityRenderingHandler(BomuProjectiles.BREEZE_BREATH_BOMB.get(),m->new BomuProjectileRenderer<>(m,.9F));
    }
}
