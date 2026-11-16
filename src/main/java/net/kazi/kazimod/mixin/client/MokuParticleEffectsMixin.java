package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.moku.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.*;
import xyz.pixelatedw.mineminenomi.particles.effects.moku.*;

@Mixin(value={WhiteLauncherParticleEffect.class,WhiteStrikeParticleEffect.class,SmokeDebuffParticleEffect.class,LogiaParticleEffect.class,SpecialFlyingParticleEffect.class},remap=false)
public abstract class MokuParticleEffectsMixin {
    @Inject(method="spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$NoDetails;)V",at=@At("HEAD"),cancellable=true)
    private void kazi$smoke(Entity source,World world,double x,double y,double z,ParticleEffect.NoDetails details,CallbackInfo ci) {
        int kind=MokuVisuals.kind((ParticleEffect<?>)(Object)this);if(kind<0)return;
        MokuEffects.emit(world,source==null?-1:source.getId(),kind,new Vector3d(x,y,z),MokuVisuals.radius(kind,details));ci.cancel();
    }
}
