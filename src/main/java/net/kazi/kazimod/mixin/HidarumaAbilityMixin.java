package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xyz.pixelatedw.mineminenomi.abilities.mera.HidarumaAbility;

@Mixin(value = HidarumaAbility.class, remap = false)
public abstract class HidarumaAbilityMixin {

    @ModifyConstant(method = "onContinuityTick", constant = @Constant(doubleValue = 0.25D))
    private double kazi$increaseSpawnSpeed(double original) {
        return 0.375D;
    }
}
