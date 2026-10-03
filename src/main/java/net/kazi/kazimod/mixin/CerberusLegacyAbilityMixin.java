package net.kazi.kazimod.mixin;

import net.kazi.kazimod.preserved.cerberus.CerberusFeatures;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.*;

@Mixin(value=Ability.class,remap=false)
public abstract class CerberusLegacyAbilityMixin {
    @Inject(method="canUse(Lnet/minecraft/entity/LivingEntity;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",at=@At("HEAD"),cancellable=true)
    private void kazimod$removedMove(LivingEntity entity,CallbackInfoReturnable<AbilityUseResult> cir){
        if(CerberusFeatures.legacy(((IAbility)this).getCore()))
            cir.setReturnValue(AbilityUseResult.fail(new StringTextComponent("This Cerberus move has been replaced. Use the reworked moveset.")));
    }
}
