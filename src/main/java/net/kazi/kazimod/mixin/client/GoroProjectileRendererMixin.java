package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.goro.GoroRaigoRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.GoroProjectiles;

@Mixin(value=GoroProjectiles.class,remap=false)
public abstract class GoroProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$stormSphere(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(GoroProjectiles.RAIGO.get(),GoroRaigoRenderer::new);
    }
}
