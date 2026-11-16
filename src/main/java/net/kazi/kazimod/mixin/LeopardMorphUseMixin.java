package net.kazi.kazimod.mixin;

import net.kazi.kazimod.leopard.LeopardFeatures;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;

@Mixin(value = AbilityHelper.class, remap = false)
public abstract class LeopardMorphUseMixin {
    @Inject(method = "canUseMorphAbility", at = @At("HEAD"), cancellable = true)
    private static void kazi$allowAwakenedLeopard(LivingEntity user, IAbility ability, MorphInfo[] morphs, CallbackInfoReturnable<AbilityUseResult> cir) {
        if (!LeopardFeatures.empowered(user)) return;
        for (MorphInfo morph : morphs) if (morph == ModMorphs.LEOPARD_HEAVY.get() || morph == ModMorphs.LEOPARD_WALK.get()) {
            cir.setReturnValue(AbilityUseResult.success());
            return;
        }
    }
}
