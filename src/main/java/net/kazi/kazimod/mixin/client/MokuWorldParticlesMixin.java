package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.moku.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.MrMagicalCart.cartaddon.particles.effects.mokuextra.WorldOfSmokeParticleEffect;

@Mixin(value=WorldOfSmokeParticleEffect.class,remap=false)
public abstract class MokuWorldParticlesMixin {
    @Inject(method="spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDLnet/MrMagicalCart/cartaddon/particles/effects/mokuextra/WorldOfSmokeParticleEffect$Details;)V",at=@At("HEAD"),cancellable=true)
    private void kazi$volume(Entity source,World world,double x,double y,double z,WorldOfSmokeParticleEffect.Details details,CallbackInfo ci) {
        MokuEffects.emit(world,source==null?-1:source.getId(),MokuVisuals.WORLD,new Vector3d(x,y,z),MokuVisuals.radius(MokuVisuals.WORLD,details));ci.cancel();
    }
}
