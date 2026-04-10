package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.rokushikiextra.ReworkedTekkaiAbility;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;

@Mixin(value = ReworkedTekkaiAbility.class, remap = false)
public class ReworkedTekkaiAbilityMixin {

    @Shadow @Final
    private ContinuousComponent continuousComponent;

    @Redirect(
            method = "onUseEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lxyz/pixelatedw/mineminenomi/api/abilities/components/ContinuousComponent;triggerContinuity(Lnet/minecraft/entity/LivingEntity;F)V"
            ),
            remap = false
    )
    private void kazi$toggleContinuity(ContinuousComponent component, LivingEntity entity, float duration) {
        if (component.isContinuous() || ((Ability) (Object) this).isContinuous()) {
            component.stopContinuity(entity);
            return;
        }
        component.startContinuity(entity, duration);
    }
}
