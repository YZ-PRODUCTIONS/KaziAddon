package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.alchemy.AlchemyClient;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class AlchemyFirstPersonMixin {
    @Inject(method = "renderHand", at = @At("HEAD"))
    private void kazi$beginAlchemyHand(MatrixStack stack, IRenderTypeBuffer buffers, int light,
                                       AbstractClientPlayerEntity player, ModelRenderer arm, ModelRenderer sleeve, CallbackInfo ci) {
        AlchemyClient.beginHand();
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void kazi$finishAlchemyHand(MatrixStack stack, IRenderTypeBuffer buffers, int light,
                                        AbstractClientPlayerEntity player, ModelRenderer arm, ModelRenderer sleeve, CallbackInfo ci) {
        AlchemyClient.endHand();
        AlchemyClient.hand((PlayerRenderer)(Object)this, stack, buffers, light, player, arm);
    }
}
