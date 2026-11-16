package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.mochi.*;
import net.MrMagicalCart.cartaddon.particles.effects.mochi.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value={MochiParticleEffect.class,MochiGrabbedParticleEffect.class,NagareMochiParticleEffect.class,
        PastryRiverParticleEffect.class,MochiBarrageParticleEffect.class,ZanGiriMochiParticleEffect.class},remap=false)
public abstract class MochiParticleEffectsMixin {
    @Inject(method="spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$NoDetails;)V",at=@At("HEAD"),cancellable=true)
    private void kazi$liquid(Entity source,World world,double x,double y,double z,ParticleEffect.NoDetails details,CallbackInfo ci) {
        int kind=MochiVisuals.kind((ParticleEffect<?>)(Object)this);
        MochiEffects.emit(world,source==null?-1:source.getId(),kind,new Vector3d(x,y,z),MochiVisuals.radius(kind));ci.cancel();
    }
}
