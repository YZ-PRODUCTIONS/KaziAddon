package net.kazi.kazimod.mixin;

import java.util.HashMap;
import java.util.Map;
import net.kazi.kazimod.projectiles.ClashState;
import net.kazi.kazimod.projectiles.ProjectileClashes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

@Mixin(value = AbilityProjectileEntity.class, remap = false)
public abstract class ProjectileClashMixin implements ClashState {
    @Unique private float kazi$clashHealth = Float.NaN;
    @Unique private final Map<Integer, Long> kazi$clashes = new HashMap<>();

    @Override public float kazi$getClashHealth() { return kazi$clashHealth; }
    @Override public void kazi$setClashHealth(float health) { kazi$clashHealth = health; }
    @Override public long kazi$getLastClash(int id) { return kazi$clashes.getOrDefault(id, Long.MIN_VALUE); }
    @Override public void kazi$markClash(int id, long tick) { kazi$clashes.put(id, tick); }

    @Inject(method = "onProjectileCollision", at = @At("HEAD"), cancellable = true)
    private void kazi$trade(AbilityProjectileEntity first, AbilityProjectileEntity second, CallbackInfo ci) {
        if (ProjectileClashes.trade(first, second)) ci.cancel();
    }
}
