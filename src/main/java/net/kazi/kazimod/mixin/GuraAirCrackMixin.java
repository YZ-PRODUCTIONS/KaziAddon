package net.kazi.kazimod.mixin;

import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.gura.AirCrackParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.gura.GekishinParticleEffect;

@Mixin(value = {AirCrackParticleEffect.class, GekishinParticleEffect.class}, remap = false)
public abstract class GuraAirCrackMixin {
    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
    private void kazimod$fracture(World world, double x, double y, double z, double sx, double sy, double sz, CallbackInfo ci) {
        GuraVfxEntity.spawn(world, x, y, z, (Object) this instanceof GekishinParticleEffect ? 12 : 7, GuraVfxEntity.AIR);
        ci.cancel();
    }
}
