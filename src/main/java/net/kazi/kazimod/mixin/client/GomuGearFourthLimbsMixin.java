package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.gomu.GomuLimbClient;
import net.minecraft.client.renderer.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.models.morphs.GearFourthModel;

@Mixin(value = GearFourthModel.class, remap = false)
public abstract class GomuGearFourthLimbsMixin {
    @Shadow @Final private ModelRenderer rightArm;
    @Shadow @Final private ModelRenderer leftArm;

    @Redirect(method = "renderToBuffer", remap = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/model/ModelRenderer;render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V"))
    private void kazi$replaceArm(ModelRenderer part, MatrixStack stack, IVertexBuilder buffer, int light, int overlay,
                                  float r, float g, float b, float a) {
        int side = part == rightArm ? 1 : part == leftArm ? -1 : 0;
        if (side == 0 || !GomuLimbClient.skipArm(part, side, stack)) part.render(stack, buffer, light, overlay, r, g, b, a);
    }
}
