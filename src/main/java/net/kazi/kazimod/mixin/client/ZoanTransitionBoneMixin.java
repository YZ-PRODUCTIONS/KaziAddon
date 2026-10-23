package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.zoan.ZoanTransitions;
import net.minecraft.client.renderer.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelRenderer.class)
public abstract class ZoanTransitionBoneMixin {
    @Inject(method = "translateAndRotate", at = @At("TAIL"))
    private void kazimod$joint(MatrixStack stack, CallbackInfo ci) {
        ZoanTransitions.joint((ModelRenderer)(Object)this, stack);
    }
}
