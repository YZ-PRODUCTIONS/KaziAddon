package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.guraextra.NewShimaYurashiAbility;
import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = NewShimaYurashiAbility.class, remap = false)
public abstract class CartShimaVisualMixin {
    @Unique private long kazimod$lastPulse = Long.MIN_VALUE;
    @Redirect(method = "duringChargeEvent", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDD)V"))
    private void kazimod$groundCracks(ParticleEffect old, Entity caster, double x, double y, double z) {
        if (caster.level.isClientSide) return;
        long now = caster.level.getGameTime();
        if (kazimod$lastPulse != Long.MIN_VALUE && now - kazimod$lastPulse < 10) return;
        kazimod$lastPulse = now;
        GuraVfxEntity.spawn(caster.level, caster.getX(), caster.getY() + .08, caster.getZ(), 27, GuraVfxEntity.GROUND);
    }
}
