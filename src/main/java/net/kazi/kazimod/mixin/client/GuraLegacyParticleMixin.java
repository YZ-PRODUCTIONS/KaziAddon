package net.kazi.kazimod.mixin.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particles.IParticleData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;

@Mixin(ParticleManager.class)
public abstract class GuraLegacyParticleMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void kazimod$blockLegacyQuakeSprites(IParticleData data, double x, double y, double z,
            double dx, double dy, double dz, CallbackInfoReturnable<Particle> ci) {
        // Fracture entities replace these sprites; also reject legacy packets from other emitters.
        if (data.getType() == ModParticleTypes.GURA.get() || data.getType() == ModParticleTypes.GURA2.get()
                || data.getType() == net.MrMagicalCart.cartaddon.init.CartParticleTypes.GURA.get()) {
            ci.setReturnValue(null);
        }
    }
}
