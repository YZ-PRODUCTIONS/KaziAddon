package net.kazi.kazimod.mixin;

import java.util.function.Predicate;
import java.util.stream.Stream;
import net.kazi.kazimod.worldturtle.WorldTurtlePlatforms;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Entity.class)
public abstract class WorldTurtlePlatformMixin {
    @Redirect(method="collide",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private Stream<VoxelShape> addTurtleDeck(World world,Entity entity,AxisAlignedBB query,Predicate<Entity> predicate){
        return Stream.concat(world.getEntityCollisions(entity,query,predicate),WorldTurtlePlatforms.collisions(entity,query));
    }
}
