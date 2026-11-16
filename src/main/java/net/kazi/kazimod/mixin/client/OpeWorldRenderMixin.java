package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.ope.OpeEffects;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class OpeWorldRenderMixin {
    // Both graphics paths have finished drawing particles before the cloud/weather matrix is applied.
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;pushMatrix()V", ordinal = 1, remap = false))
    private void kazi$renderOpe(MatrixStack stack, float partial, long time, boolean outline,
                                ActiveRenderInfo camera, GameRenderer renderer, LightTexture light,
                                Matrix4f projection, CallbackInfo ci) {
        OpeEffects.render(stack, camera, partial);
        net.kazi.kazimod.ope.WilleClient.render(stack, camera, partial);
    }
}
