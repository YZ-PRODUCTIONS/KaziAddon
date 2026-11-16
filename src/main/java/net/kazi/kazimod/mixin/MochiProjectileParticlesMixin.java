package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.entities.projectiles.mochi.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// These callbacks contain cosmetic particles only; collisions and damage run elsewhere.
@Mixin(value={GatlingMochiProjectile.class,MochiGrabProjectile.class,MochiBetterElephantGunProjectile.class},remap=false)
public abstract class MochiProjectileParticlesMixin {
    @Inject(method="onTickEvent",at=@At("HEAD"),cancellable=true)
    private void kazi$surfaceTrail(CallbackInfo ci){ci.cancel();}
}
