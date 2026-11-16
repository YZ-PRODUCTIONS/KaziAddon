package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneVisuals;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide the right armor sleeve along with the cinematic arm, including Forge armor-model overrides. */
@Mixin(BipedArmorLayer.class)
public abstract class InnerDragonsArmorMixin {
    @Unique private boolean kazi$hideRightArm;
    @Unique private BipedModel<?> kazi$maskedArmor;
    @Unique private boolean kazi$previousArm;

    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void kazi$beginPiece(MatrixStack stack, IRenderTypeBuffer buffers, LivingEntity entity,
                                 EquipmentSlotType slot, int light, BipedModel<?> model, CallbackInfo ci) {
        kazi$restoreArmor();
        kazi$hideRightArm = InnerDragonsCutsceneVisuals.isArmMissing(entity);
    }

    @Inject(method = "setPartVisibility", at = @At("TAIL"))
    private void kazi$hideSleeve(BipedModel<?> model, EquipmentSlotType slot, CallbackInfo ci) {
        if (!kazi$hideRightArm) return;
        kazi$maskedArmor = model;
        kazi$previousArm = model.rightArm.visible;
        model.rightArm.visible = false;
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void kazi$endPiece(MatrixStack stack, IRenderTypeBuffer buffers, LivingEntity entity,
                               EquipmentSlotType slot, int light, BipedModel<?> model, CallbackInfo ci) {
        kazi$restoreArmor();
        kazi$hideRightArm = false;
    }

    @Unique private void kazi$restoreArmor() {
        if (kazi$maskedArmor != null) {
            kazi$maskedArmor.rightArm.visible = kazi$previousArm;
            kazi$maskedArmor = null;
        }
    }
}
