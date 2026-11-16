package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.alchemy.AlchemyClient;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class AlchemyPlayerPoseMixin {
    @Unique private final float[] kazi$alchemyBase = new float[36];
    @Unique private boolean kazi$alchemyApplied;

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void kazi$resetAlchemy(LivingEntity player, float walk, float amount, float time,
                                   float yaw, float pitch, CallbackInfo ci) {
        if (!kazi$alchemyApplied) return;
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        ModelRenderer[] parts = kazi$alchemyParts(model);
        for (int i = 0; i < parts.length; i++) {
            ModelRenderer part = parts[i]; int o = i * 6;
            part.x = kazi$alchemyBase[o]; part.y = kazi$alchemyBase[o + 1]; part.z = kazi$alchemyBase[o + 2];
            part.xRot = kazi$alchemyBase[o + 3]; part.yRot = kazi$alchemyBase[o + 4]; part.zRot = kazi$alchemyBase[o + 5];
        }
        model.hat.copyFrom(model.head); model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm); model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg); model.leftPants.copyFrom(model.leftLeg);
        kazi$alchemyApplied = false;
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void kazi$poseAlchemy(LivingEntity player, float walk, float amount, float time,
                                  float yaw, float pitch, CallbackInfo ci) {
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        ModelRenderer[] parts = kazi$alchemyParts(model);
        for (int i = 0; i < parts.length; i++) {
            ModelRenderer part = parts[i]; int o = i * 6;
            kazi$alchemyBase[o] = part.x; kazi$alchemyBase[o + 1] = part.y; kazi$alchemyBase[o + 2] = part.z;
            kazi$alchemyBase[o + 3] = part.xRot; kazi$alchemyBase[o + 4] = part.yRot; kazi$alchemyBase[o + 5] = part.zRot;
        }
        kazi$alchemyApplied = AlchemyClient.pose(model, player, time);
    }

    @Unique private static ModelRenderer[] kazi$alchemyParts(PlayerModel<?> m) {
        return new ModelRenderer[]{m.body, m.head, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
    }
}
