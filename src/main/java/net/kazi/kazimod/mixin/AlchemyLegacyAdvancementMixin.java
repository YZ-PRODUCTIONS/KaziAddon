package net.kazi.kazimod.mixin;

import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.data.triggers.UnlockAbilityTrigger;

/** Old datapacks may still reference a removed ability when new techniques are unlocked. */
@Mixin(value = UnlockAbilityTrigger.Instance.class, remap = false)
public abstract class AlchemyLegacyAdvancementMixin {
    @Shadow private AbilityCore ability;

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void kazi$skipMissingCriterion(ServerPlayerEntity player, AbilityCore core,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (ability == null) cir.setReturnValue(false);
    }
}
