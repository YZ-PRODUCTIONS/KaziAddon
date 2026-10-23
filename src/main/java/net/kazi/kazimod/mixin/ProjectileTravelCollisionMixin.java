package net.kazi.kazimod.mixin;

import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

@Mixin(value = AbilityProjectileEntity.class, remap = false)
public abstract class ProjectileTravelCollisionMixin {
    @Unique private Vector3d kazi$travelStart;
    @Unique private double kazi$expectedTravel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void kazi$captureTravel(CallbackInfo ci) {
        AbilityProjectileEntity projectile = (AbilityProjectileEntity) (Object) this;
        kazi$travelStart = projectile.position();
        kazi$expectedTravel = projectile.getDeltaMovement().length();
    }

    // ThrowableEntity.tick has already moved the projectile when this scan runs.
    @ModifyVariable(method = "checkEntityCollision", at = @At("STORE"), ordinal = 0)
    private Vector3d kazi$traveledVelocity(Vector3d velocity) {
        AbilityProjectileEntity projectile = (AbilityProjectileEntity) (Object) this;
        Vector3d traveled = kazi$travelStart == null ? null : projectile.position().subtract(kazi$travelStart);
        // A gateway teleport is not a flight segment and must not sweep across the map.
        if (traveled == null || !Double.isFinite(traveled.lengthSqr())
                || traveled.length() > Math.max(1, kazi$expectedTravel * 2)) {
            kazi$travelStart = projectile.position();
            return velocity;
        }
        return traveled;
    }

    @ModifyVariable(method = "checkEntityCollision", at = @At("STORE"), ordinal = 1)
    private Vector3d kazi$startBeforeMovement(Vector3d position) {
        return kazi$travelStart == null ? position : kazi$travelStart;
    }
}
