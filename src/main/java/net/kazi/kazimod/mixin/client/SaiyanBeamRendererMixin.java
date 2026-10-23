package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.saiyan.*;
import net.kazi.kazimod.effects.LaserBeamVisualData;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.renderers.entities.LightningEntityRenderer;

@Mixin(value=LightningEntityRenderer.class,remap=false,priority=1100)
public abstract class SaiyanBeamRendererMixin {
    @Inject(method="renderLightning",at=@At("HEAD"),cancellable=true)
    private static void saiyan$beam(LightningEntity entity,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light,CallbackInfo ci){
        if(((LaserBeamVisualData)entity).kazimod$isKamehameha()){
            SaiyanClient.beam(entity,partial,stack,buffers);ci.cancel();
        }
    }
}
