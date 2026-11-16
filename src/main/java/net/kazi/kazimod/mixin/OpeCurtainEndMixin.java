package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.opeextra.CurtainAbility;
import net.kazi.kazimod.ope.OpeVisualPacket;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value = CurtainAbility.class, remap = false)
public abstract class OpeCurtainEndMixin {
    @Inject(method = "onContinuityEnd", at = @At("TAIL"))
    private void kazi$stop(LivingEntity user, IAbility ability, CallbackInfo ci) {
        OpeVisualPacket.send(user, OpeVisualPacket.CURTAIN, 0);
    }
}
