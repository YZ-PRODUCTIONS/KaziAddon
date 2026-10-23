package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.uoseriyu.SeiryuFlyPointAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = SeiryuFlyPointAbility.class, remap = false)
public abstract class SeiryuFlyStatsMixin {
    @ModifyConstant(method = "<clinit>", constant = @Constant(doubleValue = 25.0), require = 1)
    private static double kazi$flyPointHealth(double original) {
        return 100.0;
    }
}
