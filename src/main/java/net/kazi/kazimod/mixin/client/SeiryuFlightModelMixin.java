package net.kazi.kazimod.mixin.client;

import net.MrMagicalCart.cartaddon.entities.zoans.UoSeiryuFlyPointMorphInfo;
import net.MrMagicalCart.cartaddon.entities.zoans.UoKaenDaikoMorphInfo;
import net.kazi.kazimod.seiryu.SeiryuFlightModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

@Mixin(value={UoSeiryuFlyPointMorphInfo.class,UoKaenDaikoMorphInfo.class},remap=false)
public abstract class SeiryuFlightModelMixin {
    @Inject(method="getModel",at=@At("HEAD"),cancellable=true)
    private void kazimod$spine(CallbackInfoReturnable<MorphModel> cir){cir.setReturnValue(new SeiryuFlightModel<>());}
}
