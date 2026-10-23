package net.kazi.kazimod.mixin;

import net.kazi.kazimod.bomu.*;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.bomu.ZenshinKibakuAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value = ZenshinKibakuAbility.class, remap = false)
public abstract class BomuChargeVisualMixin {
    @Unique private BomuVfxEntity kazi$charge;

    @Inject(method = "onUseEvent", at = @At("TAIL"))
    private void kazi$chargeStart(LivingEntity owner, IAbility ability, CallbackInfo ci) {
        BomuVisuals.stop(kazi$charge);
        kazi$charge = BomuVisuals.hands(owner, 102);
    }

    @Inject(method = "endChargeEvent", at = @At("HEAD"))
    private void kazi$chargeEnd(LivingEntity owner, IAbility ability, CallbackInfo ci) {
        BomuVisuals.stop(kazi$charge);
        kazi$charge = null;
    }
}
