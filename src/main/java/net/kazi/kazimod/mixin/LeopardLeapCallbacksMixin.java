package net.kazi.kazimod.mixin;

import net.kazi.kazimod.leopard.LeopardAltMode;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.FerociousLeapAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value = FerociousLeapAbility.class, remap = false)
public abstract class LeopardLeapCallbacksMixin {
    @Inject(method = {"startContinuityEvent", "endContinuityEvent"}, at = @At("HEAD"), cancellable = true)
    private void kazi$keepNativeLeapSeparate(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (LeopardAltMode.alternate(this)) ci.cancel();
    }
}
