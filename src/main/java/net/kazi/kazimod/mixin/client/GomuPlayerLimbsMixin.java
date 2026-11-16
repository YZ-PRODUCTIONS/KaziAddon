package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.gomu.GomuLimbClient;
import net.minecraft.client.renderer.entity.model.AgeableModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AgeableModel.class)
public abstract class GomuPlayerLimbsMixin {
    @Inject(method = "renderToBuffer", at = @At("HEAD"))
    private void kazi$hide(MatrixStack stack, IVertexBuilder buffer, int light, int overlay, float r, float g, float b, float a, CallbackInfo ci) {
        GomuLimbClient.beginModel(this);
    }
    @Inject(method = "renderToBuffer", at = @At("RETURN"))
    private void kazi$restore(MatrixStack stack, IVertexBuilder buffer, int light, int overlay, float r, float g, float b, float a, CallbackInfo ci) {
        GomuLimbClient.endModel();
    }
}
