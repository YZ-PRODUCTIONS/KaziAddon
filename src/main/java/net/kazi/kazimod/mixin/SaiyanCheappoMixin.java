package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.generic.CheappoAbility;
import net.kazi.kazimod.saiyan.SaiyanState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=CheappoAbility.class,remap=false)
public abstract class SaiyanCheappoMixin {
    @Inject(method="canUnlock",at=@At("HEAD"),cancellable=true)
    private static void saiyan$allowCheappo(LivingEntity user,CallbackInfoReturnable<Boolean> ci){
        if(SaiyanState.hasFruit(user))ci.setReturnValue(true);
    }
}
