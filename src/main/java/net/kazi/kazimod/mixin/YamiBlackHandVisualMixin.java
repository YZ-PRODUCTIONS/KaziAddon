package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.yamiextra.BlackHandAbility;
import net.kazi.kazimod.yami.*;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

@Mixin(value=BlackHandAbility.class,remap=false)
public abstract class YamiBlackHandVisualMixin {
    @Inject(method="onContinuityStart",at=@At("TAIL"))
    private void kazi$coat(LivingEntity owner,IAbility ability,CallbackInfo ci) {
        YamiVisualPacket.send(owner,YamiEffects.COAT,owner.position());
    }
    @Inject(method="onContinuityEnd",at=@At("TAIL"))
    private void kazi$stop(LivingEntity owner,IAbility ability,CallbackInfo ci) {
        YamiVisualPacket.send(owner,-1,owner.position());
    }
    @Inject(method="onHitEffect",at=@At("RETURN"))
    private void kazi$impact(LivingEntity owner,LivingEntity target,ModDamageSource source,CallbackInfoReturnable<Boolean> ci) {
        if(ci.getReturnValue())YamiVisualPacket.send(owner,YamiEffects.PULL,target.position().add(0,target.getBbHeight()*.5,0));
    }
}
