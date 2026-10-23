package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.guraextra.ReworkedKabutowariAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = ReworkedKabutowariAbility.class, remap = false)
public abstract class CartKabutowariVisualMixin {
    @Redirect(method = "onChargeEnd", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;setSmokeParticles(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;)V"))
    private void kazimod$noSmoke(ExplosionAbility explosion, ParticleEffect old) {
        // Cart already emits an air crack at the grabbed target; do not duplicate it.
        explosion.setSmokeParticles(null);
    }
}
