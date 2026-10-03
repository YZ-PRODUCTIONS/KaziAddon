package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.guraextra.NewShingenNoIchigekiAbility;
import net.MrMagicalCart.cartaddon.abilities.whitebeard.StrongestShingenNoIchigekiAbility;
import net.kazi.kazimod.effects.GuraFractureEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = {NewShingenNoIchigekiAbility.class, StrongestShingenNoIchigekiAbility.class}, remap = false)
public abstract class CartShingenVisualMixin {
    @Redirect(method = "onHitTrigger", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;setSmokeParticles(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;)V"))
    private void kazimod$impact(ExplosionAbility explosion, ParticleEffect old) {
        explosion.setSmokeParticles(new GuraFractureEffect());
    }
}
