package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.particles.effects.guraextra.NewAirCrackParticleEffect;
import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NewAirCrackParticleEffect.class, remap = false)
public abstract class CartGuraAirCrackMixin {
    @Inject(method = "spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void kazimod$normal(Entity caster, World world, double x, double y, double z,
            double sx, double sy, double sz, CallbackInfo ci) {
        GuraVfxEntity.fracture(world, x, y, z, 7, caster.xRot, caster.yRot);
        ci.cancel();
    }
    @Inject(method = "spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDD)V", at = @At("HEAD"), cancellable = true)
    private void kazimod$sized(Entity caster, World world, double x, double y, double z, double size, CallbackInfo ci) {
        GuraVfxEntity.fracture(world, x, y, z, (float) size * .4375F, caster.xRot, caster.yRot);
        ci.cancel();
    }
    @Inject(method = "spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDDFF)V", at = @At("HEAD"), cancellable = true)
    private void kazimod$oriented(Entity caster, World world, double x, double y, double z, double size,
            float pitch, float yaw, CallbackInfo ci) {
        GuraVfxEntity.fracture(world, x, y, z, (float) size * .4375F, pitch, yaw);
        ci.cancel();
    }
}
