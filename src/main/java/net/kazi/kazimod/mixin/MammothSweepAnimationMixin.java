package net.kazi.kazimod.mixin;

import net.kazi.kazimod.network.MammothAnimationPacket;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.zoumammoth.AncientSweepAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value=AncientSweepAbility.class,remap=false)
public abstract class MammothSweepAnimationMixin {
    @Inject(method="endChargeEvent",at=@At("HEAD"))
    private void kazimod$sweep(LivingEntity entity,IAbility ability,CallbackInfo ci) { MammothAnimationPacket.send(entity,0); }
}
