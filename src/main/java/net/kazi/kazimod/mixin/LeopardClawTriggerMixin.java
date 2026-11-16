package net.kazi.kazimod.mixin;

import java.util.function.Predicate;
import net.kazi.kazimod.leopard.LeopardAltMode;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.ClawStrikeAbility;

@Mixin(value = ClawStrikeAbility.class, remap = false)
public abstract class LeopardClawTriggerMixin {
    @Inject(method = "canActivate", at = @At("HEAD"), cancellable = true)
    private void kazi$noExtraClawHit(CallbackInfoReturnable<Predicate<LivingEntity>> cir) {
        if (LeopardAltMode.alternate(this)) cir.setReturnValue(user -> false);
    }
}
