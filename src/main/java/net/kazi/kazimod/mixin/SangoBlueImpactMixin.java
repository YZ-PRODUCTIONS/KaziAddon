package net.kazi.kazimod.mixin;

import net.kazi.kazimod.goro.GoroVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.particles.effects.goro.LightningExplosionParticleEffect;

@Mixin(value = LightningExplosionParticleEffect.class, remap = false)
public abstract class SangoBlueImpactMixin {
    @Redirect(method = "spawn(Lnet/minecraft/world/World;DDDDDD)V",
            at = @At(value = "INVOKE", target = "Lxyz/pixelatedw/mineminenomi/config/ClientConfig;isGoroBlue()Z"))
    private boolean kazi$blueImpact(ClientConfig config) {
        return GoroVisuals.isBlueImpact() || config.isGoroBlue();
    }
}
