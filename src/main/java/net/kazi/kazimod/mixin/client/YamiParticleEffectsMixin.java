package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.yami.YamiEffects;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.yami.*;

@Mixin(value={BlackHoleParticleEffect.class,BlackWorldParticleEffect.class,DarkMatterChargingParticleEffect.class,
        DarkMatterParticleEffect.class},remap=false)
public abstract class YamiParticleEffectsMixin {
    @Inject(method="spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$NoDetails;)V",
            at=@At("HEAD"),cancellable=true)
    private void kazi$darknessVolume(Entity entity,World world,double x,double y,double z,ParticleEffect.NoDetails details,CallbackInfo ci) {
        int kind=net.kazi.kazimod.yami.YamiVisuals.kind((ParticleEffect<?>)(Object)this,entity);
        if(kind<0)return;
        YamiEffects.emit(world,entity,kind,x,y,z);
        ci.cancel();
    }
}
