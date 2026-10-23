package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.saiyan.SaiyanClient;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class SaiyanSpaceSkyMixin {
    // Unlike renderSky, this point also runs when render distance is below four chunks.
    @Inject(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/WorldRenderer;setupRender(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ClippingHelper;ZIZ)V"))
    private void saiyan$spaceSky(MatrixStack stack,float partial,long time,boolean outline,ActiveRenderInfo camera,
                                 GameRenderer renderer,LightTexture light,Matrix4f projection,CallbackInfo ci){
        SaiyanClient.renderSpaceSky(stack,partial);
    }

    @Inject(method="renderClouds",at=@At("HEAD"),cancellable=true)
    private void saiyan$spaceClouds(MatrixStack stack,float partial,double x,double y,double z,CallbackInfo ci){
        if(SaiyanClient.spaceOpacity()>=.99F)ci.cancel();
    }

    @Inject(method="renderChunkLayer",at=@At("HEAD"),cancellable=true)
    private void saiyan$spaceTerrain(RenderType type,MatrixStack stack,double x,double y,double z,CallbackInfo ci){
        if(SaiyanClient.spaceOpacity()>=.99F)ci.cancel();
    }
}
