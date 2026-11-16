package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.MrMagicalCart.cartaddon.entities.projectiles.opeextra.TaktTossProjectile;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = TaktTossProjectile.class, remap = false)
public abstract class TaktRoomLifetimeMixin {
    // Cart removes an orphaned rock but then still dereferences its missing ROOM.
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/MrMagicalCart/cartaddon/abilities/opeextra/ReworkedRoomAbility;isPositionInRoom(Lnet/minecraft/util/math/BlockPos;)Z", remap = false), remap = true)
    private boolean kazi$validRoom(ReworkedRoomAbility room, BlockPos position) {
        return room != null && room.isPositionInRoom(position);
    }
}
