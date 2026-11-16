package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.pika.*;

@Mixin(value={AmaterasuProjectile.class,YasakaniNoMagatamaProjectile.class},remap=false)
public abstract class PikaProjectileParticlesMixin {
    // These callbacks are purely cosmetic. Flight, collisions and impact callbacks remain native.
    @Inject(method="onTickEvent",at=@At("HEAD"),cancellable=true)
    private void kazi$meshTrail(CallbackInfo ci){ci.cancel();}
}
