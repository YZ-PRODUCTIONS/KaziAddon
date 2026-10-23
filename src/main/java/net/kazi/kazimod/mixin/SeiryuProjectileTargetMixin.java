package net.kazi.kazimod.mixin;

import net.kazi.kazimod.seiryu.SeiryuPartEntity;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

@Mixin(value=AbilityProjectileEntity.class,remap=false)
public abstract class SeiryuProjectileTargetMixin {
    @Shadow public abstract LivingEntity getThrower();
    @Inject(method="lambda$new$0",at=@At("HEAD"),cancellable=true)
    private void kazimod$bodyTarget(Entity target,CallbackInfoReturnable<Boolean> cir) {
        if(!(target instanceof SeiryuPartEntity))return;
        LivingEntity owner=((SeiryuPartEntity)target).owner(),thrower=getThrower();
        if(!target.canBeCollidedWith()||owner==null||owner==thrower||thrower!=null&&thrower.isAlliedTo(owner))cir.setReturnValue(false);
    }
    @ModifyVariable(method="onModHit",at=@At("HEAD"),argsOnly=true)
    private RayTraceResult kazimod$ownerImpact(RayTraceResult ray) {
        if(ray instanceof EntityRayTraceResult) {
            Entity entity=((EntityRayTraceResult)ray).getEntity();
            if(entity instanceof SeiryuPartEntity) {
                LivingEntity owner=((SeiryuPartEntity)entity).owner();
                if(owner!=null)return new EntityRayTraceResult(owner,ray.getLocation());
            }
        }
        return ray;
    }
}
