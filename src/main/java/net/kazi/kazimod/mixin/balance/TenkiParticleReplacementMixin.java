package net.kazi.kazimod.mixin.balance;

import net.minecraft.entity.Entity;
import net.kazi.kazimod.entities.WhiteTornadoEntity;
import net.kazi.kazimod.entities.projectiles.WindGustProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(value=WyHelper.class,remap=false)
public abstract class TenkiParticleReplacementMixin {
    private static boolean kazimod$replaced(Entity e){return e instanceof WhiteTornadoEntity||e instanceof WindGustProjectile||e.getPersistentData().getBoolean("KaziTenkiVfx");}
    @Inject(method="spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDD)V",at=@At("HEAD"),cancellable=true)
    private static void kazimod$plain(ParticleEffect effect,Entity e,double x,double y,double z,CallbackInfo ci){if(kazimod$replaced(e))ci.cancel();}
    @Inject(method="spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$Details;)V",at=@At("HEAD"),cancellable=true)
    private static void kazimod$detailed(ParticleEffect effect,Entity e,double x,double y,double z,ParticleEffect.Details details,CallbackInfo ci){if(kazimod$replaced(e))ci.cancel();}
}
