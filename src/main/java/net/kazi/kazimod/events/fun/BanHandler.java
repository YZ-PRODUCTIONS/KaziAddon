package net.kazi.kazimod.events.fun;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class BanHandler {

    private static final Set<UUID> BANNED_UUIDS = new HashSet<>();

    static {
        BANNED_UUIDS.add(UUID.fromString("1ed19e32-9701-429a-b018-9efaf8cf71a5"));
        BANNED_UUIDS.add(UUID.fromString("6bdf28e8-90c3-45c6-b778-7577d0dfee91"));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;

        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        if (BANNED_UUIDS.contains(player.getUUID())) {
            player.connection.disconnect(
                    new StringTextComponent("You have been disconnected.")
            );
        }
    }
}