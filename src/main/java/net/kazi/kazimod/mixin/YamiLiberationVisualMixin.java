package net.kazi.kazimod.mixin;

import net.kazi.kazimod.yami.*;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.yami.LiberationAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;

@Mixin(value=LiberationAbility.class,remap=false)
public abstract class YamiLiberationVisualMixin {
    @Shadow @Final private AltModeComponent<LiberationAbility.Mode> altModeComponent;
    @Inject(method="onUseEvent",at=@At(value="INVOKE",
            target="Lxyz/pixelatedw/mineminenomi/api/abilities/components/CooldownComponent;startCooldown(Lnet/minecraft/entity/LivingEntity;F)V"))
    private void kazi$release(LivingEntity owner,IAbility ability,CallbackInfo ci) {
        YamiVisualPacket.send(owner,altModeComponent.getCurrentMode()==LiberationAbility.Mode.RELEASE?YamiEffects.RELEASE:YamiEffects.ABSORB,owner.position());
    }
}
