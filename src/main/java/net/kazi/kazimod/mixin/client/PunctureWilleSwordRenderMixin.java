package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.ope.WilleClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonRenderer.class)
public abstract class PunctureWilleSwordRenderMixin {
    // Both first-person and HeldItemLayer use this entry point.
    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/model/ItemCameraTransforms$TransformType;ZLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("HEAD"), cancellable = true)
    private void kazi$connectedSword(LivingEntity user, ItemStack item, ItemCameraTransforms.TransformType type, boolean left,
                                     MatrixStack stack, IRenderTypeBuffer buffer, int light, CallbackInfo ci) {
        if (item == user.getMainHandItem() && WilleClient.active(user)) ci.cancel();
    }
}
