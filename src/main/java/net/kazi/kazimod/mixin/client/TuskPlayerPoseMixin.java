package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.tusk.TuskClient;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class TuskPlayerPoseMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void tusk$pose(LivingEntity user, float limb, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        TuskClient.pose((PlayerModel<?>)(Object)this, user, age);
    }
}
