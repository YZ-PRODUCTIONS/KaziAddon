package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(net.minecraft.client.renderer.ItemRenderer.class)
public abstract class KRoomSwordItemRendererMixin {

    @Inject(
            method = "renderQuadList(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;Ljava/util/List;Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kazi$renderKRoomTint(MatrixStack matrixStack, IVertexBuilder builder, List<BakedQuad> quads,
                                      ItemStack stack, int packedLight, int packedOverlay,
                                      CallbackInfo ci) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem) || !stack.hasTag()) {
            return;
        }

        if (!stack.getTag().getBoolean("kroomSwordActive")) {
            return;
        }

        MatrixStack.Entry entry = matrixStack.last();
        for (BakedQuad quad : quads) {
            builder.addVertexData(entry, quad, 0.55F, 0.9F, 1.0F, packedLight, packedOverlay, true);
        }
        ci.cancel();
    }
}
