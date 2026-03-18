package net.kazi.kazimod.events.fun;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class BanHandler {

    private static final Set<UUID> BANNED_UUIDS = new HashSet<>();

    // Playtime limit config
    private static final UUID  LIMITED_PLAYER     = UUID.fromString("be8d6275-d418-4b5c-8017-92a7f19916a8");
    private static final long  MAX_TICKS_PER_DAY  = 20L * 60 * 60 * 4; // 4 hours in ticks
    private static final long  RESET_INTERVAL_MS  = 24L * 60 * 60 * 1000; // 24 hours in ms
    private static final long  WARN_INTERVAL_TICKS = 20L * 60 * 5; // warn every 5 minutes

    // Tracks ticks played today and when the 24h window started
    private static long  ticksPlayedToday  = 0;
    private static long  windowStartMs     = System.currentTimeMillis();
    private static long  lastWarnTick      = 0;
    private static int   serverTick        = 0;

    static {
        BANNED_UUIDS.add(UUID.fromString("1ed19e32-9701-429a-b018-9efaf8cf71a5"));
        BANNED_UUIDS.add(UUID.fromString("6bdf28e8-90c3-45c6-b778-7577d0dfee91"));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        // Hard ban check
        if (BANNED_UUIDS.contains(player.getUUID())) {
            player.connection.disconnect(new StringTextComponent("You have been disconnected."));
            return;
        }

        // Playtime limit check on login
        if (player.getUUID().equals(LIMITED_PLAYER)) {
            maybeResetWindow();
            if (ticksPlayedToday >= MAX_TICKS_PER_DAY) {
                long msRemaining = RESET_INTERVAL_MS - (System.currentTimeMillis() - windowStartMs);
                long hoursLeft   = msRemaining / 3600000;
                long minsLeft    = (msRemaining % 3600000) / 60000;
                player.connection.disconnect(new StringTextComponent(
                        TextFormatting.RED + "You've reached your 4-hour daily playtime limit.\n" +
                                TextFormatting.YELLOW + "Resets in " + hoursLeft + "h " + minsLeft + "m.\n" +
                                TextFormatting.GRAY + "Take a break and come back tomorrow!"
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTick++;

        // Check if the limited player is online
        net.minecraft.server.MinecraftServer server =
                net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayerEntity limitedPlayer = server.getPlayerList().getPlayer(LIMITED_PLAYER);
        if (limitedPlayer == null) return;

        maybeResetWindow();

        ticksPlayedToday++;

        long remaining = MAX_TICKS_PER_DAY - ticksPlayedToday;

        // Warn at 30 minutes remaining
        if (remaining == 20L * 60 * 30) {
            limitedPlayer.sendMessage(
                    new StringTextComponent(TextFormatting.YELLOW +
                            "[Playtime] 30 minutes remaining today. Take a break soon!"),
                    limitedPlayer.getUUID());
        }

        // Warn at 10 minutes remaining
        if (remaining == 20L * 60 * 10) {
            limitedPlayer.sendMessage(
                    new StringTextComponent(TextFormatting.RED +
                            "[Playtime] 10 minutes remaining today!"),
                    limitedPlayer.getUUID());
        }

        // Warn at 1 minute remaining
        if (remaining == 20L * 60) {
            limitedPlayer.sendMessage(
                    new StringTextComponent(TextFormatting.DARK_RED +
                            "[Playtime] 1 minute remaining — you will be disconnected!"),
                    limitedPlayer.getUUID());
        }

        // Time's up — disconnect
        if (ticksPlayedToday >= MAX_TICKS_PER_DAY) {
            long msRemaining = RESET_INTERVAL_MS - (System.currentTimeMillis() - windowStartMs);
            long hoursLeft   = msRemaining / 3600000;
            long minsLeft    = (msRemaining % 3600000) / 60000;
            limitedPlayer.connection.disconnect(new StringTextComponent(
                    TextFormatting.RED + "You've reached your 4-hour daily playtime limit.\n" +
                            TextFormatting.YELLOW + "Resets in " + hoursLeft + "h " + minsLeft + "m.\n" +
                            TextFormatting.GRAY + "Take a break and come back tomorrow!"
            ));
        }
    }

    // Resets the 24-hour window and tick counter if 24 hours have passed
    private static void maybeResetWindow() {
        long now = System.currentTimeMillis();
        if (now - windowStartMs >= RESET_INTERVAL_MS) {
            ticksPlayedToday = 0;
            windowStartMs    = now;
        }
    }
}