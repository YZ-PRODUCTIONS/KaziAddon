package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.alchemy.AlchemyClient;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class AlchemyWorldRenderMixin {
    // Same ordering as OpeWorldRenderMixin: both particle paths have finished, composite has not.
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;pushMatrix()V", ordinal = 1, remap = false))
    private void kazi$renderAlchemy(MatrixStack stack, float partial, long time, boolean outline,
                                    ActiveRenderInfo camera, GameRenderer renderer, LightTexture light,
                                    Matrix4f projection, CallbackInfo ci) {
        AlchemyClient.render(stack, camera, partial, projection);
    }
}
