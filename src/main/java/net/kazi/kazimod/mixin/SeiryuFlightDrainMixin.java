package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.uoseriyu.SeiryuFlightPassiveAbility;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;

@Mixin(value = PropelledFlightAbility.class, remap = false)
public abstract class SeiryuFlightDrainMixin {
    @ModifyConstant(method = "onDuringPassive", constant = @Constant(doubleValue = -5.0), require = 1)
    private double kazi$flyPointDrain(double original, LivingEntity entity) {
        // Kaen Daiko shares the flight passive and legacy morph name, but is a separate form.
        if ((Object) this instanceof SeiryuFlightPassiveAbility
                && DevilFruitCapability.get(entity).getCurrentMorph().orElseGet(() -> MorphHelper.getZoanInfo(entity))
                == CartMorphs.SEIRYU_FLY.get()) {
            return original / 3.0;
        }
        return original;
    }
}
