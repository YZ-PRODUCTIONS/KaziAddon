package net.kazi.kazimod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.kazi.kazimod.worldturtle.WorldTurtleHitboxes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WorldTurtleBodyTargetMixin {
    @Inject(method="isPickable",at=@At("HEAD"),cancellable=true)
    private void turtlePartsInsteadOfMovementBox(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof PlayerEntity&&WorldTurtleHitboxes.active((LivingEntity)(Object)this))cir.setReturnValue(false);
    }
}
