package net.kazi.kazimod.mixin;

import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.gura.TenchiMeidoAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = TenchiMeidoAbility.class, remap = false)
public abstract class TenchiMeidoVisualMixin {
    @Shadow private RangeComponent rangeComponent;
    @Unique private long kazimod$lastPulse = Long.MIN_VALUE;
    @Redirect(method = "duringChargeEvent", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$Details;)V"))
    private void kazimod$replaceDebris(ParticleEffect old, Entity caster, double x, double y, double z, ParticleEffect.Details details) {
        long now = caster.level.getGameTime();
        if (kazimod$lastPulse != Long.MIN_VALUE && now - kazimod$lastPulse < 10) return;
        kazimod$lastPulse = now;
        GuraVfxEntity.spawn(caster.level, caster.getX(), caster.getY() + .08, caster.getZ(), 26, GuraVfxEntity.GROUND);
    }
    @Inject(method = "endChargeEvent", at = @At("TAIL"))
    private void kazimod$rupture(LivingEntity caster, IAbility ability, CallbackInfo ci) {
        GuraVfxEntity.spawn(caster.level, caster.getX(), caster.getY() + 2, caster.getZ(),
                Math.max(26, rangeComponent.getRange()), GuraVfxEntity.AIR);
    }
}
