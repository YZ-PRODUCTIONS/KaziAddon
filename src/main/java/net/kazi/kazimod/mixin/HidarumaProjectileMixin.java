package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xyz.pixelatedw.mineminenomi.entities.projectiles.mera.HidarumaProjectile;

@Mixin(value = HidarumaProjectile.class, remap = false)
public abstract class HidarumaProjectileMixin {

    @ModifyConstant(method = "onTickEvent", constant = @Constant(doubleValue = 0.5D))
    private double kazi$increaseHomingSpeed(double original) {
        return 0.75D;
    }
}
