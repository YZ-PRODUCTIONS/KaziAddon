package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.moku.WhiteSnakeProjectile;

@Mixin(value=WhiteSnakeProjectile.class,remap=false)
public abstract class MokuSnakeParticlesMixin {
    // Keep target acquisition and homing; only skip the 40-particle emission loop.
    @ModifyConstant(method="onTickEvent",constant=@Constant(intValue=40),allow=1)
    private int kazi$smokeMesh(int count){return 0;}
}
