package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.mochi.*;
import net.kazi.kazimod.mochi.MochiProjectileRenderer.Shape;
import net.MrMagicalCart.cartaddon.entities.projectiles.mochi.MochiProjectiles;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=MochiProjectiles.class,remap=false)
public abstract class MochiProjectileRendererMixin {
    @Inject(method="registerEntityRenderers",at=@At("TAIL"))
    private static void kazi$liquid(FMLClientSetupEvent event,CallbackInfo ci) {
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.MOCHI_HADAN.get(),m->new MochiProjectileRenderer<>(m,Shape.BULLET,.23F,false));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.GATLING_MOCHI.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,.45F,false));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.MOCHI_BETTER_ELEPHANT.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,2.1F,false));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.YAKI_MOCHI.get(),m->new MochiProjectileRenderer<>(m,Shape.FIRE,1.2F,false));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.KURI_MOCHI.get(),MochiKuriRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.KURI_MOCHI_PROJECTILE.get(),m->new MochiProjectileRenderer<>(m,Shape.SPIKE,.18F,true));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.MOCHI_GRAB.get(),m->new MochiProjectileRenderer<>(m,Shape.GRAB,.6F,false));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.PASTRY_RIVER.get(),m->new MochiProjectileRenderer<>(m,Shape.RIVER,7,false));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.MOCHI_BARRAGE.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,1.25F,true));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.DOWN_SLAM.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,3.5F,true));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.CHIKARA_MOCHI.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,2,true));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.KAKU_MOCHI_RIGHT.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,.8F,true));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.KAKU_MOCHI_LEFT.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,.8F,true));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.DONUT.get(),m->new MochiProjectileRenderer<>(m,Shape.DONUT,1.5F,false));
        RenderingRegistry.registerEntityRenderingHandler(MochiProjectiles.ANEMONE.get(),m->new MochiProjectileRenderer<>(m,Shape.FIST,1.25F,true));
    }
}
