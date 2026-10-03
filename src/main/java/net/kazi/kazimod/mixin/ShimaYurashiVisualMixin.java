package net.kazi.kazimod.mixin;

import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.gura.ShimaYurashiAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = ShimaYurashiAbility.class, remap = false)
public abstract class ShimaYurashiVisualMixin {
    @Shadow private RangeComponent rangeComponent;
    @Unique private long kazimod$lastPulse = Long.MIN_VALUE;
    @Redirect(method = "duringChargeEvent", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDD)V"))
    private void kazimod$groundFractures(ParticleEffect old, Entity caster, double x, double y, double z) {
        long now = caster.level.getGameTime();
        if (now == kazimod$lastPulse || (kazimod$lastPulse != Long.MIN_VALUE && now - kazimod$lastPulse < 10)) return;
        kazimod$lastPulse = now;
        GuraVfxEntity.spawn(caster.level, caster.getX(), caster.getY() + .08, caster.getZ(), 12, GuraVfxEntity.GROUND);
    }
    @Inject(method = "endChargeEvent", at = @At("TAIL"))
    private void kazimod$rupture(LivingEntity caster, IAbility ability, CallbackInfo ci) {
        GuraVfxEntity.spawn(caster.level, caster.getX(), caster.getY() + 2, caster.getZ(),
                Math.max(12, rangeComponent.getRange()), GuraVfxEntity.AIR);
    }
}
