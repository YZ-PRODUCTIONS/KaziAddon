package net.kazi.kazimod.mixin;

import net.kazi.kazimod.leopard.LeopardAltMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.ClawStrikeAbility;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.FerociousLeapAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

@Mixin(value = {ClawStrikeAbility.class, FerociousLeapAbility.class}, remap = false)
public abstract class LeopardAbilityModesMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void kazi$addModes(AbilityCore<?> core, CallbackInfo ci) {
        Ability self = (Ability)(Object)this;
        new LeopardAltMode(self, self instanceof ClawStrikeAbility, false);
    }
}
