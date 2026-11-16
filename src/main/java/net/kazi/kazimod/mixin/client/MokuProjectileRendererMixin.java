package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.moku.MokuProjectileRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.moku.MokuProjectiles;

@Mixin(value=MokuProjectiles.class,remap=false)
public abstract class MokuProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$smoke(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(MokuProjectiles.WHITE_BLOW.get(),m->new MokuProjectileRenderer<>(m,.7F));
        RenderingRegistry.registerEntityRenderingHandler(MokuProjectiles.WHITE_BLOW_RUSH.get(),m->new MokuProjectileRenderer<>(m,.65F));
        RenderingRegistry.registerEntityRenderingHandler(MokuProjectiles.WHITE_OUT.get(),m->new MokuProjectileRenderer<>(m,.85F));
        RenderingRegistry.registerEntityRenderingHandler(MokuProjectiles.WHITE_SNAKE.get(),m->new MokuProjectileRenderer<>(m,1.1F));
    }
}
