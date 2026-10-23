package net.kazi.kazimod.mixin;

import java.util.List;
import java.util.function.Predicate;
import net.kazi.kazimod.seiryu.SeiryuTargets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

@Mixin(value=WyHelper.class,remap=false)
public abstract class SeiryuAbilityTargetsMixin {
    @ModifyVariable(method="getNearbyEntities(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;[Ljava/lang/Class;)Ljava/util/List;",
            at=@At("HEAD"),argsOnly=true)
    private static Predicate<Entity> kazimod$ownerFilter(Predicate<Entity> predicate) {
        return SeiryuTargets.ownerPredicate(predicate);
    }
    @Inject(method="getNearbyEntities(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;[Ljava/lang/Class;)Ljava/util/List;",
            at=@At("RETURN"),cancellable=true)
    private static void kazimod$areaTargets(IWorld world,AxisAlignedBB box,Predicate<Entity> predicate,Class<?>[] types,
                                           CallbackInfoReturnable<List<Entity>> cir) {
        // Entity queries must retain physical sections for projectile ray intersection.
        if(SeiryuTargets.livingQuery(types))cir.setReturnValue(SeiryuTargets.livingTargets(cir.getReturnValue(),world,box,predicate,types));
    }
    @Inject(method="getEntitiesNearSphere",at=@At("RETURN"),cancellable=true)
    private static void kazimod$sphereTargets(Vector3d center,IWorld world,double radius,Predicate<Entity> predicate,Class<?>[] types,
                                             CallbackInfoReturnable<List<Entity>> cir) {
        if(SeiryuTargets.livingQuery(types))cir.setReturnValue(SeiryuTargets.normalize(cir.getReturnValue(),predicate));
    }
    @Inject(method="getEntitiesAroundCircle",at=@At("RETURN"),cancellable=true)
    private static void kazimod$circleTargets(Vector3d center,IWorld world,double radius,double thickness,Predicate<Entity> predicate,Class<?>[] types,
                                             CallbackInfoReturnable<List<Entity>> cir) {
        if(SeiryuTargets.livingQuery(types))cir.setReturnValue(SeiryuTargets.normalize(cir.getReturnValue(),predicate));
    }
    @Inject(method="getNearbyPlayers",at=@At("RETURN"),cancellable=true)
    private static void kazimod$playerTargets(Vector3d center,IWorld world,double radius,Predicate<Entity> predicate,
                                             CallbackInfoReturnable<List<Entity>> cir) {
        cir.setReturnValue(SeiryuTargets.livingTargets(cir.getReturnValue(),world,new AxisAlignedBB(center,center).inflate(radius),
                predicate,new Class<?>[]{PlayerEntity.class}));
    }
}
