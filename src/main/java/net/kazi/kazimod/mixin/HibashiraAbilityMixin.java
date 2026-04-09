package net.kazi.kazimod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.kazi.kazimod.events.AwakeningAbilityLoginFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.abilities.mera.HibashiraAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;

@Mixin(value = HibashiraAbility.class, remap = false)
public class HibashiraAbilityMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, remap = false)
    private void kazi$blockAwakenedHibashira(LivingEntity entity, CallbackInfoReturnable<AbilityUseResult> cir) {
        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        if (devilFruit == null || !devilFruit.hasDevilFruit(ModAbilities.MERA_MERA_NO_MI) || !devilFruit.hasAwakenedFruit()) {
            return;
        }

        if (entity instanceof PlayerEntity) {
            AwakeningAbilityLoginFix.syncPlayerAwakeningReplacements((PlayerEntity) entity);
        }
        cir.setReturnValue(AbilityUseResult.fail(null));
    }
}
