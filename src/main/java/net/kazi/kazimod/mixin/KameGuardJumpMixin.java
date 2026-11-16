package net.kazi.kazimod.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;

/** Block the actual jump impulse on both sides, before jump boosts or jump events run. */
@Mixin(LivingEntity.class)
public abstract class KameGuardJumpMixin {
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void kazimod$preventGuardJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (ModMorphs.KAME_GUARD.get().isActive(entity)) {
            entity.setJumping(false);
            ci.cancel();
        }
    }
}
