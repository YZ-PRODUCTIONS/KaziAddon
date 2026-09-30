package net.kazi.kazimod.mixin;

import com.mojang.authlib.GameProfile;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.security.RemoteUuidBanlist;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/** Rejects remotely banlisted UUIDs before Minecraft admits them to the server. */
@Mixin(PlayerList.class)
public abstract class PlayerListBanlistMixin {

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void kazi$rejectBanlistedProfile(SocketAddress address, GameProfile profile,
                                              CallbackInfoReturnable<ITextComponent> cir) {
        if (profile == null || profile.getId() == null) {
            return;
        }

        RemoteUuidBanlist.BanEntry ban = RemoteUuidBanlist.find(profile.getId());
        if (ban == null) {
            return;
        }

        KaziMod.LOGGER.warn("Rejected remotely banlisted UUID {} ({})", ban.getUuid(), ban.getReason());
        cir.setReturnValue(new StringTextComponent(
                "Access to KaziAdditions has been revoked. Reason: " + ban.getReason()));
    }
}
