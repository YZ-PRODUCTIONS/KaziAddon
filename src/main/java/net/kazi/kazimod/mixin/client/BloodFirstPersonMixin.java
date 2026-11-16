package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.blood.BloodClient;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class BloodFirstPersonMixin {
    @Inject(method = "renderHand", at = @At("HEAD"))
    private void kazi$beginBloodHand(MatrixStack stack, IRenderTypeBuffer buffers, int light,
                                       AbstractClientPlayerEntity player, ModelRenderer arm, ModelRenderer sleeve, CallbackInfo ci) {
        BloodClient.beginHand();
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void kazi$finishBloodHand(MatrixStack stack, IRenderTypeBuffer buffers, int light,
                                        AbstractClientPlayerEntity player, ModelRenderer arm, ModelRenderer sleeve, CallbackInfo ci) {
        BloodClient.endHand();
    }
}
