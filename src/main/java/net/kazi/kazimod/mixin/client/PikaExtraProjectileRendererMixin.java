package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.pika.PikaProjectileRenderer;
import net.MrMagicalCart.cartaddon.entities.projectiles.pikaclone.PikaExtraProjectiles;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PikaExtraProjectiles.class,remap=false)
public abstract class PikaExtraProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$cut(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(PikaExtraProjectiles.DIVINE_CUT.get(),m->new PikaProjectileRenderer<>(m,2));
    }
}
