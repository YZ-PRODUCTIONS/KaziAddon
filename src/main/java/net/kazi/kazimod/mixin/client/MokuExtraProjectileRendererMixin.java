package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.moku.MokuProjectileRenderer;
import net.MrMagicalCart.cartaddon.entities.projectiles.mokuextra.MokuExtraProjectiles;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=MokuExtraProjectiles.class,remap=false)
public abstract class MokuExtraProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$smoke(FMLClientSetupEvent event,CallbackInfo ci){
        RenderingRegistry.registerEntityRenderingHandler(MokuExtraProjectiles.SMOKE_BLAST.get(),m->new MokuProjectileRenderer<>(m,3.2F));
    }
}
