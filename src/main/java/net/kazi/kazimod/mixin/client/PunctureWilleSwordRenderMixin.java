package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class PunctureWilleSwordRenderMixin {

    @Unique
    private static final ThreadLocal<Boolean> kazi$punctureScaled = ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "render(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/model/ItemCameraTransforms$TransformType;ZLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;IILnet/minecraft/client/renderer/model/IBakedModel;)V",
            at = @At("HEAD")
    )
    private void kazi$scalePunctureSword(ItemStack stack, ItemCameraTransforms.TransformType transformType, boolean leftHand,
                                         MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight,
                                         int packedOverlay, IBakedModel model, CallbackInfo ci) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem) || !stack.hasTag()) {
            kazi$punctureScaled.set(false);
            return;
        }

        boolean punctureActive = stack.getTag().getBoolean("punctureWilleSwordActive");
        boolean shockActive = stack.getTag().getBoolean("shockWilleSwordActive");
        if (!punctureActive && !shockActive) {
            kazi$punctureScaled.set(false);
            return;
        }

        if (transformType != ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND
                && transformType != ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                && transformType != ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND
                && transformType != ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND) {
            kazi$punctureScaled.set(false);
            return;
        }

        float scale = punctureActive
                ? stack.getTag().getFloat("punctureWilleSwordScale")
                : stack.getTag().getFloat("shockWilleSwordScale");
        if (scale <= 1.0F) {
            kazi$punctureScaled.set(false);
            return;
        }

        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.0D, -0.14D * (scale - 1.0F));
        matrixStack.scale(0.62F, 0.62F, scale * 1.12F);
        kazi$punctureScaled.set(true);
    }

    @Inject(
            method = "render(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/model/ItemCameraTransforms$TransformType;ZLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;IILnet/minecraft/client/renderer/model/IBakedModel;)V",
            at = @At("RETURN")
    )
    private void kazi$unscalePunctureSword(ItemStack stack, ItemCameraTransforms.TransformType transformType, boolean leftHand,
                                           MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight,
                                           int packedOverlay, IBakedModel model, CallbackInfo ci) {
        if (kazi$punctureScaled.get()) {
            matrixStack.popPose();
            kazi$punctureScaled.set(false);
        }
    }
}
