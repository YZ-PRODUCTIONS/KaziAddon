package net.kazi.kazimod.mixin;

import net.kazi.kazimod.leopard.LeopardAltMode;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.ClawStrikeAbility;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.FerociousLeapAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;

@Mixin(value = {PunchAbility2.class, DropHitAbility.class}, remap = false)
public abstract class LeopardBaseCallbacksMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "xyz/pixelatedw/mineminenomi/api/abilities/components/ContinuousComponent"))
    private ContinuousComponent kazi$parallelMode(IAbility ability, boolean parallel) {
        if (ability instanceof ClawStrikeAbility || ability instanceof FerociousLeapAbility)
            return new ContinuousComponent(ability, c -> parallel && !LeopardAltMode.alternate(ability));
        return new ContinuousComponent(ability, parallel);
    }
    @Inject(method = "onUseEvent", at = @At("HEAD"), cancellable = true)
    private void kazi$use(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (LeopardAltMode.alternate(this)) { LeopardAltMode.get(this).use(user); ci.cancel(); }
    }
    @Inject(method = "startContinuityEvent", at = @At("HEAD"), cancellable = true)
    private void kazi$start(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (LeopardAltMode.alternate(this)) { LeopardAltMode.get(this).start(user); ci.cancel(); }
    }
    @Inject(method = "tickContinuityEvent", at = @At("HEAD"), cancellable = true)
    private void kazi$tick(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (LeopardAltMode.alternate(this)) { LeopardAltMode.get(this).tickAttack(user); ci.cancel(); }
    }
    @Inject(method = "endContinuityEvent", at = @At("HEAD"), cancellable = true)
    private void kazi$end(LivingEntity user, IAbility ability, CallbackInfo ci) {
        if (LeopardAltMode.alternate(this)) { LeopardAltMode.get(this).end(user); ci.cancel(); }
    }
}
