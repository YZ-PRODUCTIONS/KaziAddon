package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.ope.OpeEffects;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingRenderer.class)
public abstract class OpeSplitRenderMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V", at = @At("HEAD"), argsOnly = true)
    private IRenderTypeBuffer kazi$separate(IRenderTypeBuffer original, LivingEntity entity, float yaw, float partial,
                                           MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        return OpeEffects.split(entity, stack, original, partial);
    }
}
