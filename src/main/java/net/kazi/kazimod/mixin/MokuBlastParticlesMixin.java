package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.MrMagicalCart.cartaddon.entities.projectiles.mokuextra.SmokeBlastProjectile;

@Mixin(value=SmokeBlastProjectile.class,remap=false)
public abstract class MokuBlastParticlesMixin {
    // The terrain/damage explosion earlier in this callback still executes normally.
    @ModifyConstant(method="onTickEvent",constant=@Constant(intValue=35),allow=1)
    private int kazi$smokeMesh(int count){return 0;}
}
