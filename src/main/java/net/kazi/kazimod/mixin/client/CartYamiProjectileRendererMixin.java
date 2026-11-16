package net.kazi.kazimod.mixin.client;

import net.MrMagicalCart.cartaddon.entities.projectiles.yamiextra.ReworkedYamiProjectiles;
import net.kazi.kazimod.yami.YamiProjectileRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ReworkedYamiProjectiles.class,remap=false)
public abstract class CartYamiProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$darkness(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(ReworkedYamiProjectiles.REWORKED_DARK_MATTER.get(),m->new YamiProjectileRenderer<>(m,6,false));
        RenderingRegistry.registerEntityRenderingHandler(ReworkedYamiProjectiles.REWORKED_BLACK_ROAD.get(),m->new YamiProjectileRenderer<>(m,1.2F,true));
    }
}
