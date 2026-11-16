package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.kazi.kazimod.ope.OpeVisualPacket;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

@Mixin(value = WyHelper.class, remap = false)
public abstract class OpeVisualDeliveryMixin {
    @Inject(method = "spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$Details;)V", at = @At("HEAD"), cancellable = true)
    private static void kazi$ope(ParticleEffect<?> effect, Entity entity, double x, double y, double z, ParticleEffect.Details details, CallbackInfo ci) {
        int kind = effect == CartParticleEffects.CURTAIN.get() ? OpeVisualPacket.CURTAIN
                : effect == CartParticleEffects.RADIO_KNIFE.get() ? OpeVisualPacket.RADIO : -1;
        if (kind < 0) return;
        OpeVisualPacket.refresh(entity, kind);
        ci.cancel();
    }
}
