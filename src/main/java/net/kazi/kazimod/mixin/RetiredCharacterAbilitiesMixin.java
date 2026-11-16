package net.kazi.kazimod.mixin;

import net.kazi.kazimod.events.RetiredCharacterOptions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataBase;

@Mixin(value = AbilityDataBase.class, remap = false)
public abstract class RetiredCharacterAbilitiesMixin {
    @Shadow private LivingEntity dataOwner;

    @Unique private boolean kazi$blocked(AbilityCore<?> core) {
        return (dataOwner == null || dataOwner instanceof PlayerEntity) && RetiredCharacterOptions.retired(core);
    }

    @Inject(method = "addUnlockedAbility(Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityCore;Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUnlock;)Z",
            at = @At("HEAD"), cancellable = true)
    private void kazi$blockGrant(AbilityCore<?> core, AbilityUnlock unlock, CallbackInfoReturnable<Boolean> cir) {
        if (kazi$blocked(core)) cir.setReturnValue(false);
    }

    @Inject(method = "addUnlockedAbility(Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityCoreUnlockWrapper;)Z",
            at = @At("HEAD"), cancellable = true)
    private void kazi$blockSavedGrant(AbilityCoreUnlockWrapper<?> wrapper, CallbackInfoReturnable<Boolean> cir) {
        if (wrapper != null && kazi$blocked(wrapper.getAbilityCore())) cir.setReturnValue(false);
    }

    @Inject(method = "addPassiveAbility", at = @At("HEAD"), cancellable = true)
    private void kazi$blockPassive(IAbility ability, CallbackInfoReturnable<Boolean> cir) {
        if (ability != null && kazi$blocked(ability.getCore())) cir.setReturnValue(false);
    }

    @Inject(method = "setEquippedAbility(ILxyz/pixelatedw/mineminenomi/api/abilities/IAbility;)Z",
            at = @At("HEAD"), cancellable = true)
    private void kazi$blockEquip(int slot, IAbility ability, CallbackInfoReturnable<Boolean> cir) {
        if (ability != null && kazi$blocked(ability.getCore())) cir.setReturnValue(false);
    }
}
