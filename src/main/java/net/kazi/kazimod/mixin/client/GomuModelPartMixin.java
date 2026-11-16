package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.gomu.GomuLimbClient;
import net.minecraft.client.renderer.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelRenderer.class)
public abstract class GomuModelPartMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V", at = @At("HEAD"), cancellable = true)
    private void kazi$replaceExtendedLimb(MatrixStack stack, IVertexBuilder buffer, int light, int overlay,
                                          float r, float g, float b, float a, CallbackInfo ci) {
        if (GomuLimbClient.skipPart((ModelRenderer)(Object)this, stack)) ci.cancel();
    }
}
