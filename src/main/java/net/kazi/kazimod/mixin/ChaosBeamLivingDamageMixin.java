package net.kazi.kazimod.mixin;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamDamage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Enforce the beam's health floor after all final-damage event handlers have run. */
@Mixin(LivingEntity.class)
public abstract class ChaosBeamLivingDamageMixin {
    @Redirect(method = "actuallyHurt(Lnet/minecraft/util/DamageSource;F)V", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setHealth(F)V"))
    private void kazi$keepChaosBeamTargetAlive(LivingEntity target, float proposedHealth,
                                              DamageSource source, float incomingDamage) {
        target.setHealth(ChaosBeamDamage.clampHealth(source, proposedHealth));
    }
}
