package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.rokushikiextra.ReworkedTekkaiAbility;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.Optional;

@Mixin(value = Ability.class, remap = false)
public abstract class TekkaiToggleUseMixin {

    @Inject(method = "use(Lnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void kazi$toggleActiveTekkai(LivingEntity entity, CallbackInfo ci) {
        if (!((Object) this instanceof ReworkedTekkaiAbility)) {
            return;
        }

        Ability self = (Ability) (Object) this;
        if (!self.isContinuous()) {
            return;
        }

        Optional<ContinuousComponent> component = self.getComponent(ModAbilityKeys.CONTINUOUS);
        if (!component.isPresent()) {
            return;
        }

        component.get().stopContinuity(entity);
        ci.cancel();
    }
}
