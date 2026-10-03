package net.kazi.kazimod.mixin;
import net.kazi.kazimod.mammoth.MammothFeatures;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.*;

@Mixin(value=Ability.class,remap=false)
public abstract class MammothLegacyAbilityMixin {
    @Inject(method="canUse(Lnet/minecraft/entity/LivingEntity;)Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUseResult;",at=@At("HEAD"),cancellable=true)
    private void kazimod$legacy(LivingEntity user,CallbackInfoReturnable<AbilityUseResult> ci){
        if(MammothFeatures.legacy(((IAbility)this).getCore()))ci.setReturnValue(AbilityUseResult.fail(new StringTextComponent("This Mammoth move has been replaced. Reconnect to update your ability slots.")));
    }
}
