package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.saber.DawnbreakerDashAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = DawnbreakerDashAbility.class, remap = false)
public class DawnbreakerDashAbilityMixin {

    @ModifyConstant(method = "onTickContinuityEvent", constant = @Constant(floatValue = 45.0F), remap = false)
    private float kazi$reduceDashDamage(float original) {
        return 30.0F;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(floatValue = 45.0F), remap = false)
    private static float kazi$reduceDashTooltipDamage(float original) {
        return 30.0F;
    }
}
