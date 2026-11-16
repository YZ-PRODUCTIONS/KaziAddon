package net.kazi.kazimod.mixin;

import net.kazi.kazimod.abilities.SupaRework.RealityMarbleBoundary;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class RealityMarbleCollisionMixin {
    @Inject(method = {"setPos", "setPosRaw", "setPosAndOldPos", "absMoveTo(DDD)V"},
            at = @At("HEAD"), cancellable = true)
    private void kazimod$blockEscape(double x, double y, double z, CallbackInfo ci) {
        if (RealityMarbleBoundary.blocksTeleport((Entity)(Object)this, x, y, z)) ci.cancel();
    }
    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void kazimod$marbleWall(Vector3d movement, CallbackInfoReturnable<Vector3d> result) {
        result.setReturnValue(RealityMarbleBoundary.collide((Entity)(Object)this, result.getReturnValue()));
    }
}
