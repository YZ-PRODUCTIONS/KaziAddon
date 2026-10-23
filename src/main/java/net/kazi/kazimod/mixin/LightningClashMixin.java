package net.kazi.kazimod.mixin;

import net.kazi.kazimod.projectiles.ProjectileClashes;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

@Mixin(value = LightningEntity.class, remap = false)
public abstract class LightningClashMixin {
    @Shadow private float maxTravelDistance;
    @Shadow private float travelSpeed;
    @Unique private float kazi$clashLimit = Float.POSITIVE_INFINITY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void kazi$scanClashes(CallbackInfo ci) {
        LightningEntity beam = (LightningEntity) (Object) this;
        if (beam.level.isClientSide) return;
        float reach = Math.min(maxTravelDistance, beam.getLength() + travelSpeed);
        kazi$clashLimit = ProjectileClashes.scan(beam, reach);
        // The native tick only expands the ray. Shorten an already extended beam, too.
        if (beam.isAlive() && beam.getLength() > kazi$clashLimit) beam.setLength(kazi$clashLimit);
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lxyz/pixelatedw/mineminenomi/entities/projectiles/goro/LightningEntity;setLength(F)V"), index = 0)
    private float kazi$limitGrowth(float length) {
        return Math.min(length, kazi$clashLimit);
    }

    @Inject(method = "onEntityImpactEvent", at = @At("HEAD"), cancellable = true)
    private void kazi$preventUnconditionalDeletion(Entity target, CallbackInfo ci) {
        if (target instanceof AbilityProjectileEntity) ci.cancel();
    }
}
