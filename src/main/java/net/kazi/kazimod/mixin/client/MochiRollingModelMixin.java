package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.mochi.MochiRollingModel;
import net.MrMagicalCart.cartaddon.entities.zoans.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

@Mixin(value={CarvedRollingMochiMorphInfo.class,RollingMochiMorphInfo.class},remap=false)
public abstract class MochiRollingModelMixin {
    @Inject(method="getModel",at=@At("HEAD"),cancellable=true)
    private void kazi$fluidWheel(CallbackInfoReturnable<MorphModel> cir){cir.setReturnValue(new MochiRollingModel((Object)this instanceof CarvedRollingMochiMorphInfo));}
}
