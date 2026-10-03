package net.kazi.kazimod.mixin;

import net.kazi.kazimod.mammoth.*;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.abilities.zoumammoth.MammothTrampleAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

@Mixin(value=MammothTrampleAbility.class,remap=false)
public abstract class MammothTrampleControlMixin {
    @Shadow public float speed;
    @Inject(method="duringPassiveEvent",at=@At("HEAD"),cancellable=true)
    private void kazimod$controlledMovement(LivingEntity user,CallbackInfo ci){
        for(MammothAbility.Move move:new MammothAbility.Move[]{MammothAbility.Move.STOMP,MammothAbility.Move.STAMPEDE}){
            IAbility ability=AbilityDataCapability.get(user).getEquippedAbility(MammothFeatures.CORES.get(move));
            if(ability instanceof MammothAbility&&(((MammothAbility)ability).continuous.isContinuous()||((MammothAbility)ability).charge.isCharging())){speed=0;ci.cancel();return;}
        }
    }
}
