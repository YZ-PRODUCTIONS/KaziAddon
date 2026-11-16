package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.pika.PikaProjectileRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.pika.PikaProjectiles;

@Mixin(value=PikaProjectiles.class,remap=false)
public abstract class PikaProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$light(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(PikaProjectiles.AMATERASU.get(),m->new PikaProjectileRenderer<>(m,0));
        RenderingRegistry.registerEntityRenderingHandler(PikaProjectiles.YASAKANI_NO_MAGATAMA.get(),m->new PikaProjectileRenderer<>(m,1));
    }
}
