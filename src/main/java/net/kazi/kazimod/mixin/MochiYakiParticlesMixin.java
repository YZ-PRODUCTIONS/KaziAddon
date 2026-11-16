package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.entities.projectiles.mochi.YakiMochiProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=YakiMochiProjectile.class,remap=false)
public abstract class MochiYakiParticlesMixin {
    @ModifyConstant(method="onTickEvent",constant=@Constant(intValue=9))
    private int kazi$flameMesh(int count){return 0;}
}
