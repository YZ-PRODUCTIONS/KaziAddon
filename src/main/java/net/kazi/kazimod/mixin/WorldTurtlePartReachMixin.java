package net.kazi.kazimod.mixin;

import net.kazi.kazimod.worldturtle.WorldTurtlePartEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerPlayNetHandler.class)
public abstract class WorldTurtlePartReachMixin {
    @Redirect(method="handleInteract",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/ServerPlayerEntity;distanceToSqr(Lnet/minecraft/entity/Entity;)D"))
    private double turtleSurfaceReach(ServerPlayerEntity player,Entity target){
        if(!(target instanceof WorldTurtlePartEntity))return player.distanceToSqr(target);
        WorldTurtlePartEntity part=(WorldTurtlePartEntity)target;
        if(!part.canBeCollidedWith()||part.owner()==player)return Double.POSITIVE_INFINITY;
        // Preserve the packet's six-block limit, measured to the aimed-at surface,
        // not the center of a giant shell or flipper several blocks behind it.
        Vector3d eye=player.getEyePosition(1);
        if(part.getBoundingBox().contains(eye))return 0;
        return part.getBoundingBox().clip(eye,eye.add(player.getLookAngle().scale(6)))
                .map(eye::distanceToSqr).orElse(Double.POSITIVE_INFINITY);
    }
}
