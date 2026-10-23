package net.kazi.kazimod.mixin;

import net.kazi.kazimod.network.MammothAnimationPacket;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.abilities.zoumammoth.AncientTrunkShotAbility;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

@Mixin(value=AncientTrunkShotAbility.class,remap=false)
public abstract class MammothTrunkAnimationMixin {
    @Inject(method="onHitEffect",at=@At("HEAD"))
    private void kazimod$shot(LivingEntity entity,LivingEntity target,ModDamageSource source,CallbackInfoReturnable<Boolean> ci) { MammothAnimationPacket.send(entity,2); }
}
