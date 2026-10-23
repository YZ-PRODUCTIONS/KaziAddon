package net.kazi.kazimod.mixin;

import net.kazi.kazimod.doku.DokuGasEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.doku.ChloroBallProjectile;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value=ChloroBallProjectile.ChloroBallCloudEntity.class,remap=false)
public abstract class DokuChloroCloudVisualMixin {
    @Shadow private ChloroBallProjectile proj;
    @Unique private DokuGasEntity kazi$cloud;
    @Redirect(method="tick",remap=true,at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$Details;)V",remap=false))
    private void kazi$cloud(ParticleEffect<?> effect,Entity source,double x,double y,double z,ParticleEffect.Details details){
        kazi$cloud=DokuGasEntity.refresh(kazi$cloud,source,DokuGasEntity.CLOUD,proj.getPoisonRange(),proj.isDemonMode(),false);
    }
}
