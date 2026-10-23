package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.renderers.entities.projectiles.GekishinVfxRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gura.GuraProjectiles;

@Mixin(value = GuraProjectiles.class, remap = false)
public abstract class GuraProjectileRendererMixin {
    @Inject(method = "registerEntityRenderers", at = @At("TAIL"))
    private static void kazimod$renderer(FMLClientSetupEvent event, CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(GuraProjectiles.GEKISHIN.get(), GekishinVfxRenderer::new);
    }
}
