package net.kazi.kazimod.mixin.client;

import net.MrMagicalCart.cartaddon.entities.projectiles.opeextra.OpeExtraProjectiles;
import net.kazi.kazimod.ope.TaktRubbleRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OpeExtraProjectiles.class, remap = false)
public abstract class TaktRubbleRendererMixin {
    @Inject(method = "registerEntityRenderers", at = @At("TAIL"))
    private static void kazi$rubble(FMLClientSetupEvent event, CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(OpeExtraProjectiles.TAKT_TOSS.get(), TaktRubbleRenderer::new);
    }
}
