package net.kazi.kazimod.mixin.balance;

import java.util.function.Predicate;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

@Mixin(targets="net.kazi.kazimod.abilities.Tenki.GaleStormAbility",remap=false)
public abstract class TenkiGaleStormMixin {
    @Shadow private Vector3d spawnPosition;
    @Shadow private AxisAlignedBB cachedBarrierBox;
    @Shadow private Predicate<LivingEntity> cachedEnemyPredicate;
    @Unique private int kazimod$slowTicks;
    @Inject(method="doPush",at=@At("HEAD"))
    private void kazimod$stormSlow(LivingEntity caster,CallbackInfo ci){
        if(caster.level.isClientSide||spawnPosition==null||cachedBarrierBox==null)return;
        if(++kazimod$slowTicks<5)return;
        kazimod$slowTicks=0;
        for(LivingEntity target:caster.level.getEntitiesOfClass(LivingEntity.class,cachedBarrierBox,cachedEnemyPredicate)){
            if(target==caster||!target.isAlive()||caster.isAlliedTo(target)||target.isSpectator()||(target instanceof PlayerEntity&&((PlayerEntity)target).isCreative()))continue;
            double dx=target.getX()-spawnPosition.x,dz=target.getZ()-spawnPosition.z;
            if(dx*dx+dz*dz<=83*83)target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,20,1,false,false,true));
        }
    }
    @Redirect(method="onTick",at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticleEffect(Lxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect;Lnet/minecraft/entity/Entity;DDD)V"))
    private void kazimod$replaceBarrierParticles(ParticleEffect effect,Entity source,double x,double y,double z){}
}
