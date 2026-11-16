package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.yamiextra.ReworkedLiberationAbility;
import net.kazi.kazimod.yami.*;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

@Mixin(value=ReworkedLiberationAbility.class,remap=false)
public abstract class CartYamiLiberationVisualMixin {
    @Inject(method="absorbDarkness",at=@At("TAIL"))
    private void kazi$absorb(LivingEntity owner,CallbackInfo ci) {
        YamiVisualPacket.send(owner,YamiEffects.ABSORB,owner.position());
    }
    @Inject(method="onRepeaterTrigger",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/World;addFreshEntity(Lnet/minecraft/entity/Entity;)Z",remap=true))
    private void kazi$release(LivingEntity owner,IAbility ability,CallbackInfo ci) {
        YamiVisualPacket.send(owner,YamiEffects.RELEASE,owner.position());
    }
}
