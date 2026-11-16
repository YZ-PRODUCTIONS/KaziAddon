package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.opeextra.RadioKnifeAbility;
import net.kazi.kazimod.abilities.OpeRework.RadioKnifeRework;
import net.kazi.kazimod.ope.OpeVisualPacket;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value = {RadioKnifeAbility.class, RadioKnifeRework.class}, remap = false)
public abstract class OpeRadioEndMixin {
    @Inject(method = "endChargeEvent", at = @At("TAIL"))
    private void kazi$stop(LivingEntity user, IAbility ability, CallbackInfo ci) {
        OpeVisualPacket.send(user, OpeVisualPacket.RADIO, 0);
    }
}
