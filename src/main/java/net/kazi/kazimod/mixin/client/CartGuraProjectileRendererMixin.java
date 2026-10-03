package net.kazi.kazimod.mixin.client;

import net.MrMagicalCart.cartaddon.entities.projectiles.guraextra.NewGuraProjectiles;
import net.kazi.kazimod.renderers.entities.projectiles.GekishinVfxRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NewGuraProjectiles.class, remap = false)
public abstract class CartGuraProjectileRendererMixin {
    @Inject(method = "registerEntityRenderers", at = @At("TAIL"))
    private static void kazimod$shockwave(FMLClientSetupEvent event, CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(NewGuraProjectiles.NEW_GEKISHIN.get(), manager -> new GekishinVfxRenderer<>(manager, 8));
    }
}
