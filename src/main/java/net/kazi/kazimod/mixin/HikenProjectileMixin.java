package net.kazi.kazimod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.mera.HikenProjectile;

@Mixin(value = HikenProjectile.class, remap = false)
public abstract class HikenProjectileMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)V", at = @At("RETURN"))
    private void kazi$increaseMaxLife(World world, LivingEntity thrower, CallbackInfo ci) {
        ((HikenProjectile) (Object) this).setMaxLife(48);
    }
}
