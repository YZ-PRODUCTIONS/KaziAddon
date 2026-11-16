package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.pika.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.*;
import xyz.pixelatedw.mineminenomi.particles.effects.pika.*;
import net.MrMagicalCart.cartaddon.particles.effects.pikaextra.*;

@Mixin(value={ChargingPikaParticleEvent.class,FlashParticleEffect.class,YataNoKagamiParticleEffect.class,
        LogiaParticleEffect.class,JewelFlashParticleEffect.class,DivineCutParticleEffect.class,DivineCutProjectileParticleEffect.class},remap=false)
public abstract class PikaParticleEffectsMixin {
    @Inject(method="spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$NoDetails;)V",at=@At("HEAD"),cancellable=true)
    private void kazi$light(Entity source,World world,double x,double y,double z,ParticleEffect.NoDetails details,CallbackInfo ci) {
        int kind=PikaVisuals.kind((ParticleEffect<?>)(Object)this);if(kind<0)return;
        PikaEffects.emit(world,source==null?-1:source.getId(),kind,new Vector3d(x,y,z),1);ci.cancel();
    }
}
