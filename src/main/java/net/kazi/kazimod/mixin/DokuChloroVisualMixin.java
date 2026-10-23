package net.kazi.kazimod.mixin;

import net.kazi.kazimod.doku.DokuGasEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.doku.ChloroBallProjectile;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value=ChloroBallProjectile.class,remap=false)
public abstract class DokuChloroVisualMixin {
    @Unique private DokuGasEntity kazi$trail;
    @Inject(method="onTickEvent",at=@At("HEAD"),cancellable=true)
    private void kazi$vapor(CallbackInfo ci){
        ChloroBallProjectile self=(ChloroBallProjectile)(Object)this;
        if(!self.isOnFire()){
            kazi$trail=DokuGasEntity.refresh(kazi$trail,self,DokuGasEntity.PROJECTILE,1.1F,self.isDemonMode(),true);
            ci.cancel();
        }
    }
    @Redirect(method="onBlockImpactEvent",at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDD)V"))
    private void kazi$burst(ParticleEffect<?> effect,Entity source,double x,double y,double z){
        ChloroBallProjectile self=(ChloroBallProjectile)(Object)this;
        DokuGasEntity.refresh(null,self,DokuGasEntity.BURST,self.getPoisonRange(),self.isDemonMode(),false);
        if(kazi$trail!=null)kazi$trail.finish();
    }
    @Redirect(method="onTickEvent",at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;doExplosion()V"))
    private void kazi$ignition(ExplosionAbility explosion){net.kazi.kazimod.bomu.BomuVisuals.explode(explosion);}
}
