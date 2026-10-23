package net.kazi.kazimod.mixin;

import net.kazi.kazimod.bomu.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value = ExplosionAbility.class, remap = false)
public abstract class BomuExplosionVisualMixin implements BomuExplosionVisuals, net.kazi.kazimod.goro.GoroExplosionVisuals {
    @Shadow private float explosionSize;
    @Unique private int kazi$bomu = -1;
    @Unique private int kazi$goro = -1;

    @Override public void kazi$goroExplosion(boolean visible) { kazi$goro = visible ? 1 : 0; }

    @Override public void kazi$bomuVisual(boolean visible) { kazi$bomu = visible ? 1 : 0; }

    // Run at the native visual callback, after cancellation and height checks.
    @Redirect(method = "doExplosion", at = @At(value = "INVOKE",
            target = "Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;spawn(Lnet/minecraft/world/World;DDDDDD)V"))
    private void kazi$replaceBurst(ParticleEffect<?> particles, World world, double x, double y, double z,
                                   double vx, double vy, double vz) {
        if (kazi$goro >= 0) {
            if (kazi$goro == 1) net.kazi.kazimod.goro.GoroVfxEntity.spawn(world,
                    new net.minecraft.util.math.vector.Vector3d(x, y + .3, z),
                    net.kazi.kazimod.goro.GoroVfxEntity.IMPACT, Math.max(1, explosionSize), 20);
        }
        else if (kazi$bomu < 0) particles.spawn(world, x, y, z, vx, vy, vz);
        else if (kazi$bomu == 1) BomuVfxEntity.spawn(world, x, y + .3, z, explosionSize,
                BomuVfxEntity.BLAST, 32);
    }
}
