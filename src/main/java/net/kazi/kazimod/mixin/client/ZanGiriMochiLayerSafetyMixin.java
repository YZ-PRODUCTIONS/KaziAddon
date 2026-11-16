package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.renderers.layers.morphs.ZanGiriMochiNewLayer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.kazi.kazimod.mochi.ZanGiriMochiVisual;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cart's layer pushes the pose stack before looking up its original ability.
 * A migrated player can retain the old morph while only having our cloned
 * ability, causing that lookup to fail and leaving the pose stack unbalanced.
 */
@Mixin(value = ZanGiriMochiNewLayer.class, remap = false)
public abstract class ZanGiriMochiLayerSafetyMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void kazimod$skipLayerWithoutCartAbility(MatrixStack matrixStack,
                                                      IRenderTypeBuffer buffer,
                                                      int packedLight,
                                                      LivingEntity entity,
                                                      float limbSwing,
                                                      float limbSwingAmount,
                                                      float partialTicks,
                                                      float ageInTicks,
                                                      float netHeadYaw,
                                                      float headPitch,
                                                      CallbackInfo ci) {
        if (!entity.isInvisible()) {
            ZanGiriMochiVisual.render(entity, matrixStack, buffer, ageInTicks, partialTicks, packedLight);
        }
        ci.cancel();
    }
}
