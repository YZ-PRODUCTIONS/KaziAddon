package net.kazi.kazimod.mixin.balance;

import net.minecraft.entity.*;
import net.kazi.kazimod.entities.projectiles.GomuGomuNoKaminariProjectile;
import net.kazi.kazimod.preserved.tenki.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(targets="net.kazi.kazimod.abilities.Tenki.LightningJabAbility",remap=false)
public abstract class TenkiLightningVfxMixin {
    @Shadow private LightningEntity boltInner;
    @Shadow private LightningEntity boltOuter;
    @Inject(method="spawnFuryBeam",at=@At("RETURN"))
    private void kazimod$markBeams(LivingEntity caster,Vector3d origin,CallbackInfo ci){
        if(boltInner!=null)boltInner.getPersistentData().putBoolean("KaziTenkiVfx",true);
        if(boltOuter!=null)boltOuter.getPersistentData().putBoolean("KaziTenkiVfx",true);
    }
    @Inject(method="createProjectile",at=@At("RETURN"))
    private void kazimod$lightning(LivingEntity caster,CallbackInfoReturnable<GomuGomuNoKaminariProjectile> cir){if(cir.getReturnValue()!=null)TenkiEffects.attach(cir.getReturnValue(),4,5);}
    @Redirect(method="onContinuityTick",at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDD)V"))
    private void kazimod$arc(ParticleEffect effect,Entity owner,double x,double y,double z){if(!owner.level.isClientSide){TenkiVisualEntity e=TenkiVisualEntity.spawn(owner,4,5,8);e.setPos(x,y,z);}}
}
