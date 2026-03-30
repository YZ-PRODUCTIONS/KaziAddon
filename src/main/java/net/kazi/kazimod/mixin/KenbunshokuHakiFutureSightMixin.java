package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.pixelatedw.mineminenomi.abilities.haki.KenbunshokuHakiFutureSightAbility;

@Mixin(value = KenbunshokuHakiFutureSightAbility.class, remap = false)
public class KenbunshokuHakiFutureSightMixin {

    @ModifyArg(
            method = "damageTakenEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lxyz/pixelatedw/mineminenomi/data/entity/haki/IHakiData;alterHakiOveruse(I)V"
            ),
            index = 0
    )
    private int kazi$increaseFutureSightOveruse(int originalOveruse) {
        return Math.max(1, Math.round(originalOveruse * 1.25F));
    }
}
