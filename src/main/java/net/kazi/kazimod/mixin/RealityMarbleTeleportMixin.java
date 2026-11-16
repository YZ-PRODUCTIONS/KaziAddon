package net.kazi.kazimod.mixin;

import java.util.Set;
import net.kazi.kazimod.abilities.SupaRework.RealityMarbleBoundary;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reject before the connection records and sends an unreachable teleport destination. */
@Mixin(ServerPlayNetHandler.class)
public abstract class RealityMarbleTeleportMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), cancellable = true)
    private void kazimod$containTeleport(double x, double y, double z, float yaw, float pitch,
            Set<SPlayerPositionLookPacket.Flags> flags, CallbackInfo ci) {
        // Coordinates here are absolute; flags only control encoding of the outgoing packet.
        if (RealityMarbleBoundary.blocksTeleport(player, x, y, z)) ci.cancel();
    }
}
