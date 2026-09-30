package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.abilities.mochi2.ZanGiriMochiAbility;
import net.MrMagicalCart.cartaddon.models.abilities.ZanGiriMochiNewModel;
import net.MrMagicalCart.cartaddon.renderers.layers.morphs.ZanGiriMochiNewLayer;
import net.kazi.kazimod.abilities.MochiRework.ZanGiriMochiClone;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModRenderTypes;

/**
 * Cart's layer pushes the pose stack before looking up its original ability.
 * A migrated player can retain the old morph while only having our cloned
 * ability, causing that lookup to fail and leaving the pose stack unbalanced.
 */
@Mixin(value = ZanGiriMochiNewLayer.class, remap = false)
public abstract class ZanGiriMochiLayerSafetyMixin {

    @Shadow private ZanGiriMochiNewModel model;

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
        if (AbilityDataCapability.get(entity)
                .getEquippedAbility(ZanGiriMochiAbility.INSTANCE) != null) return;

        ZanGiriMochiClone clone = (ZanGiriMochiClone) AbilityDataCapability.get(entity)
                .getEquippedAbility(ZanGiriMochiClone.INSTANCE);
        if (clone == null || entity.isInvisible()) {
            ci.cancel();
            return;
        }

        matrixStack.pushPose();
        try {
            matrixStack.translate(-1.2D, 0.2D, 0.0D);
            if (clone.isCharging()) {
                float speed = clone.getChargeComponent(entity).getChargeTime() >= 40.0F ? 2.0F : 0.5F;
                model.wholeArm.yRot = ageInTicks * speed % ((float) Math.PI * 2.0F);
            } else {
                model.wholeArm.yRot = 0.0F;
                model.wholeArm.zRot = clone.isContinuous() ? -300.0F : 300.0F;
            }

            RenderType renderType = ModRenderTypes.getZoanWithCullingRenderType(
                    ZanGiriMochiNewLayer.TEXTURE);
            model.renderToBuffer(matrixStack, buffer.getBuffer(renderType), packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            matrixStack.popPose();
        }
        ci.cancel();
    }
}
