package net.kazi.kazimod.mixin.client;

import net.MrMagicalCart.cartaddon.entities.projectiles.inucerberus.CerberusProjectiles;
import net.kazi.kazimod.renderers.entities.projectiles.CerberusHeadsRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=CerberusProjectiles.class,remap=false)
public abstract class CerberusProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazimod$heads(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(CerberusProjectiles.HELL_HEAD_MOTOR.get(),CerberusHeadsRenderer::new);
    }
}
