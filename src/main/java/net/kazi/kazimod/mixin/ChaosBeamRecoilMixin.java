package net.kazi.kazimod.mixin;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamRecoil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.events.DamageCalcsEvents;

/** Keep Chaos Beam's recoil independent of MMNM toughness and damage reduction. */
@Mixin(value = DamageCalcsEvents.class, remap = false)
public abstract class ChaosBeamRecoilMixin {
    @Inject(method = "calcDamageReduction(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/DamageSource;F)F",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void kazi$keepChaosBeamRecoil(LivingEntity target, DamageSource source, float amount,
                                                CallbackInfoReturnable<Float> cir) {
        // MMNM ignores bypassArmor for toughness. Scope this exception to recoil instead of
        // using bypassInvul, which would also defeat creative immunity and totem protection.
        if (ChaosBeamRecoil.isRecoil(source)) cir.setReturnValue(amount);
    }
}
