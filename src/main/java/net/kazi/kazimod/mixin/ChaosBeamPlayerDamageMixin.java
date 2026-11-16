package net.kazi.kazimod.mixin;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamDamage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Players override actuallyHurt, so their separate health write needs the same beam-only floor. */
@Mixin(PlayerEntity.class)
public abstract class ChaosBeamPlayerDamageMixin {
    @Redirect(method = "actuallyHurt(Lnet/minecraft/util/DamageSource;F)V", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setHealth(F)V"))
    private void kazi$keepChaosBeamPlayerAlive(PlayerEntity target, float proposedHealth,
                                              DamageSource source, float incomingDamage) {
        target.setHealth(ChaosBeamDamage.clampHealth(source, proposedHealth));
    }
}
