package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.moku.*;

@Mixin(value={WhiteBlowProjectile.class,WhiteBlowRushProjectile.class,WhiteOutProjectile.class},remap=false)
public abstract class MokuProjectileParticlesMixin {
    // These three callbacks contain only smoke emission, not collision or movement logic.
    @Inject(method="onTickEvent",at=@At("HEAD"),cancellable=true)
    private void kazi$replaceSmoke(CallbackInfo ci){ci.cancel();}
}
