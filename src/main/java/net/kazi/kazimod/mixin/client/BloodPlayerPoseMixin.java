package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.blood.BloodClient;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class BloodPlayerPoseMixin {
    @Unique private final float[] kazi$bloodBase = new float[36];
    @Unique private boolean kazi$bloodApplied;

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void kazi$resetBlood(LivingEntity player, float walk, float amount, float time,
                                   float yaw, float pitch, CallbackInfo ci) {
        if (!kazi$bloodApplied) return;
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        ModelRenderer[] parts = kazi$bloodParts(model);
        for (int i = 0; i < parts.length; i++) {
            ModelRenderer part = parts[i]; int o = i * 6;
            part.x = kazi$bloodBase[o]; part.y = kazi$bloodBase[o + 1]; part.z = kazi$bloodBase[o + 2];
            part.xRot = kazi$bloodBase[o + 3]; part.yRot = kazi$bloodBase[o + 4]; part.zRot = kazi$bloodBase[o + 5];
        }
        model.hat.copyFrom(model.head); model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm); model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg); model.leftPants.copyFrom(model.leftLeg);
        kazi$bloodApplied = false;
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void kazi$poseBlood(LivingEntity player, float walk, float amount, float time,
                                  float yaw, float pitch, CallbackInfo ci) {
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        ModelRenderer[] parts = kazi$bloodParts(model);
        for (int i = 0; i < parts.length; i++) {
            ModelRenderer part = parts[i]; int o = i * 6;
            kazi$bloodBase[o] = part.x; kazi$bloodBase[o + 1] = part.y; kazi$bloodBase[o + 2] = part.z;
            kazi$bloodBase[o + 3] = part.xRot; kazi$bloodBase[o + 4] = part.yRot; kazi$bloodBase[o + 5] = part.zRot;
        }
        kazi$bloodApplied = BloodClient.pose(model, player, time);
    }

    @Unique private static ModelRenderer[] kazi$bloodParts(PlayerModel<?> m) {
        return new ModelRenderer[]{m.body, m.head, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
    }
}
