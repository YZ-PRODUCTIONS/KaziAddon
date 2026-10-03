package net.kazi.kazimod.mixin;

import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.abilities.gura.ShingenNoIchigekiAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = ShingenNoIchigekiAbility.class, remap = false)
public abstract class ShingenVisualMixin {
    @Redirect(method = {"onUseEvent", "onHitTrigger"}, at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;setSmokeParticles(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;)V"))
    private void kazimod$replaceExplosion(ExplosionAbility explosion, ParticleEffect old) {
        explosion.setSmokeParticles(new net.kazi.kazimod.effects.GuraFractureEffect());
    }
}
