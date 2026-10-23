package net.kazi.kazimod.mixin;

import net.kazi.kazimod.bomu.BomuVisuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.bomu.BreezeBreathBombProjectile;

@Mixin(targets = {
        "xyz.pixelatedw.mineminenomi.abilities.bomu.ExplosivePunchAbility",
        "xyz.pixelatedw.mineminenomi.abilities.bomu.KickBombAbility",
        "xyz.pixelatedw.mineminenomi.abilities.bomu.ZenshinKibakuAbility",
        "xyz.pixelatedw.mineminenomi.entities.projectiles.bomu.BreezeBreathBombProjectile",
        "xyz.pixelatedw.mineminenomi.entities.projectiles.bomu.NoseFancyCannonProjectile"
}, remap = false)
public abstract class BomuNativeExplosionMixin {
    @Redirect(method = {"onHitEvent", "onUseEvent", "endChargeEvent", "onTickEvent", "onBlockImpactEvent"},
            at = @At(value = "INVOKE", target = "Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;doExplosion()V"))
    private void kazi$bomuExplosion(ExplosionAbility explosion) {
        Object source = this;
        // The breath damages every tick; its overlapping visual bursts need only 1/3 that rate.
        boolean visible = !(source instanceof BreezeBreathBombProjectile)
                || ((BreezeBreathBombProjectile) source).tickCount % 3 == 0;
        BomuVisuals.explode(explosion, visible);
    }
}
