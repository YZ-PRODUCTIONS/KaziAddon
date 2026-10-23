package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.goro.*;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.renderers.entities.LightningEntityRenderer;

@Mixin(value=LightningEntityRenderer.class,remap=false,priority=1200)
public abstract class GoroRendererMixin {
    @Inject(method="renderLightning",at=@At("HEAD"),cancellable=true)
    private static void kazi$electricity(LightningEntity bolt,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light,CallbackInfo ci) {
        if(((GoroBoltVisuals)bolt).kazi$goroKind()!=0) {
            GoroVfxRenderer.lightning(bolt,partial,stack,buffers);ci.cancel();
        }
    }
}
