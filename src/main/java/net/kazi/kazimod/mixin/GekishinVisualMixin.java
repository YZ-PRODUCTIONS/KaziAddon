package net.kazi.kazimod.mixin;

import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.gura.GekishinAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value = GekishinAbility.class, remap = false)
public abstract class GekishinVisualMixin {
    @Unique private GuraVfxEntity kazimod$charge;
    @Inject(method = "onChargeStart", at = @At("TAIL"))
    private void kazimod$start(LivingEntity caster, IAbility ability, CallbackInfo ci) {
        if (kazimod$charge != null) kazimod$charge.remove();
        kazimod$charge = GuraVfxEntity.charge(caster);
    }
    @Inject(method = "onChargeEnd", at = @At("HEAD"))
    private void kazimod$end(LivingEntity caster, IAbility ability, CallbackInfo ci) {
        if (kazimod$charge != null) kazimod$charge.remove();
        kazimod$charge = null;
    }
}
