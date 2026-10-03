package net.kazi.kazimod.mixin;

import net.kazi.kazimod.worldturtle.WorldTurtleImmunity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class WorldTurtleForcedEffectMixin {
    @Inject(method="forceAddEffect",at=@At("HEAD"),cancellable=true)
    private void rejectForcedTurtleStun(EffectInstance effect,CallbackInfo ci){
        if(WorldTurtleImmunity.blocks((LivingEntity)(Object)this,effect))ci.cancel();
    }
}
