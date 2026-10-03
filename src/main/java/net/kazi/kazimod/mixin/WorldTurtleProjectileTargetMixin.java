package net.kazi.kazimod.mixin;

import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.kazi.kazimod.worldturtle.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=AbilityProjectileEntity.class,remap=false)
public abstract class WorldTurtleProjectileTargetMixin {
    @Shadow public abstract LivingEntity getThrower();
    @Inject(method="lambda$new$0",at=@At("HEAD"),cancellable=true)
    private void turtlePartTargets(Entity target,CallbackInfoReturnable<Boolean> cir){
        if(target instanceof PlayerEntity&&WorldTurtleHitboxes.active((LivingEntity)target))cir.setReturnValue(false);
        if(target instanceof WorldTurtlePartEntity){
            LivingEntity owner=((WorldTurtlePartEntity)target).owner(),thrower=getThrower();
            if(!target.canBeCollidedWith()||owner==null||owner==thrower||(thrower!=null&&thrower.isAlliedTo(owner)))cir.setReturnValue(false);
        }
    }
    @ModifyVariable(method="onModHit",at=@At("HEAD"),argsOnly=true)
    private net.minecraft.util.math.RayTraceResult turtleOwnerImpact(net.minecraft.util.math.RayTraceResult ray){
        if(ray instanceof net.minecraft.util.math.EntityRayTraceResult){
            Entity target=((net.minecraft.util.math.EntityRayTraceResult)ray).getEntity();
            if(target instanceof WorldTurtlePartEntity){
                // Keep shell impacts on the immune part instead of bypassing it.
                if(((WorldTurtlePartEntity)target).isShell())return ray;
                LivingEntity owner=((WorldTurtlePartEntity)target).owner();
                if(owner!=null)return new net.minecraft.util.math.EntityRayTraceResult(owner,ray.getLocation());
            }
        }
        return ray;
    }
}
