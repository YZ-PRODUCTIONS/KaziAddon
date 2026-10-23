package net.kazi.kazimod.mixin;

import net.kazi.kazimod.network.MammothAnimationPacket;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.zoumammoth.AncientStompAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value=AncientStompAbility.class,remap=false)
public abstract class MammothStompAnimationMixin {
    @Inject(method="triggerRepeaterEvent",at=@At("HEAD"))
    private void kazimod$stomp(LivingEntity entity,IAbility ability,CallbackInfo ci) { MammothAnimationPacket.send(entity,1); }
}
