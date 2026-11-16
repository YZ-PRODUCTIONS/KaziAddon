package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneVisuals;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The held item remains in the inventory; only its detached cinematic hand render is suppressed. */
@Mixin(HeldItemLayer.class)
public abstract class InnerDragonsHeldItemMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void kazi$hideDetachedItem(LivingEntity entity, ItemStack item, ItemCameraTransforms.TransformType transform,
                                       HandSide arm, MatrixStack stack, IRenderTypeBuffer buffers, int light, CallbackInfo ci) {
        if (arm == HandSide.RIGHT && InnerDragonsCutsceneVisuals.isArmMissing(entity)) ci.cancel();
    }
}
