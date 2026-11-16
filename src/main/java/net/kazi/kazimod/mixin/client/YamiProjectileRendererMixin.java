package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.yami.YamiProjectileRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.yami.YamiProjectiles;

@Mixin(value=YamiProjectiles.class,remap=false)
public abstract class YamiProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$darkness(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(YamiProjectiles.DARK_MATTER.get(),m->new YamiProjectileRenderer<>(m,4.5F,false));
        RenderingRegistry.registerEntityRenderingHandler(YamiProjectiles.BLACK_ROAD.get(),m->new YamiProjectileRenderer<>(m,1.2F,true));
    }
}
