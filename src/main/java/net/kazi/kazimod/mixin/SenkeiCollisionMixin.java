package net.kazi.kazimod.mixin;

import java.util.function.Predicate;
import java.util.stream.Stream;
import net.kazi.kazimod.petal.SenkeiBoundary;
import net.kazi.kazimod.petal.PetalFeatures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SenkeiCollisionMixin {
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void holdBloomingPrisonTarget(MoverType mover,Vector3d motion,CallbackInfo callback){
        Entity entity=(Entity)(Object)this;
        if(entity instanceof LivingEntity&&PetalFeatures.prisonStunned((LivingEntity)entity))callback.cancel();
    }
    @Redirect(method="collide",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private Stream<VoxelShape> addSwordWall(World world,Entity entity,AxisAlignedBB query,Predicate<Entity> predicate){
        return Stream.concat(world.getEntityCollisions(entity,query,predicate),SenkeiBoundary.collisions(entity,query));
    }
}
