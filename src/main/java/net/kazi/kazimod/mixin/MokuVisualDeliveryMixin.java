package net.kazi.kazimod.mixin;

import net.kazi.kazimod.moku.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

@Mixin(value=WyHelper.class,remap=false)
public abstract class MokuVisualDeliveryMixin {
    @Inject(method="spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$Details;)V",at=@At("HEAD"),cancellable=true)
    private static void kazi$smoke(ParticleEffect<?> effect,Entity source,double x,double y,double z,ParticleEffect.Details details,CallbackInfo ci) {
        int kind=MokuVisuals.kind(effect);if(kind<0)return;
        if(source!=null)MokuVisualPacket.send(source,kind,new Vector3d(x,y,z),MokuVisuals.radius(kind,details));ci.cancel();
    }
}
