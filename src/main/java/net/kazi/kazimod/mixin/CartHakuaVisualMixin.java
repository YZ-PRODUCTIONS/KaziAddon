package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.guraextra.HakuaAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = HakuaAbility.class, remap = false)
public abstract class CartHakuaVisualMixin {
    @Redirect(method = "endContinuityEvent", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;setSmokeParticles(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;)V"))
    private void kazimod$noSmoke(ExplosionAbility explosion, ParticleEffect old) {
        explosion.setSmokeParticles(null);
    }
}
