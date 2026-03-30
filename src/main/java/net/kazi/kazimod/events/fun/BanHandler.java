package net.kazi.kazimod.events.fun;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber
public class BanHandler {

    private static final Set<UUID> BANNED_UUIDS = new HashSet<>();
    private static final Map<UUID, Long> LIMITED_PLAYERS = new HashMap<>();
    private static final Map<UUID, PlaytimeState> PLAYTIME_STATES = new HashMap<>();

    private static final long RESET_INTERVAL_MS = 24L * 60 * 60 * 1000;
    private static final long THIRTY_MINUTES_TICKS = 20L * 60 * 30;
    private static final long TEN_MINUTES_TICKS = 20L * 60 * 10;
    private static final long ONE_MINUTE_TICKS = 20L * 60;

    static {
        BANNED_UUIDS.add(UUID.fromString("1ed19e32-9701-429a-b018-9efaf8cf71a5"));
        BANNED_UUIDS.add(UUID.fromString("6bdf28e8-90c3-45c6-b778-7577d0dfee91"));
        BANNED_UUIDS.add(UUID.fromString("f64eceed-951b-4ce0-b8a8-8cee1fc93516"));
        BANNED_UUIDS.add(UUID.fromString("256d9736-3f3b-4b2b-af77-68204867c799"));

        LIMITED_PLAYERS.put(UUID.fromString("be8d6275-d418-4b5c-8017-92a7f19916a8"), hoursToTicks(4.0D));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        UUID playerId = player.getUUID();

        if (BANNED_UUIDS.contains(playerId)) {
            player.connection.disconnect(new StringTextComponent("You have been disconnected."));
            return;
        }

        Long maxTicksPerDay = LIMITED_PLAYERS.get(playerId);
        if (maxTicksPerDay == null) {
            return;
        }

        PlaytimeState state = getOrCreateState(playerId);
        maybeResetWindow(state);
        if (state.ticksPlayedToday >= maxTicksPerDay) {
            player.connection.disconnect(buildLimitMessage(maxTicksPerDay, state.windowStartMs));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (Map.Entry<UUID, Long> entry : LIMITED_PLAYERS.entrySet()) {
            UUID playerId = entry.getKey();
            long maxTicksPerDay = entry.getValue();
            ServerPlayerEntity limitedPlayer = server.getPlayerList().getPlayer(playerId);
            if (limitedPlayer == null) {
                continue;
            }

            PlaytimeState state = getOrCreateState(playerId);
            maybeResetWindow(state);
            state.ticksPlayedToday++;

            long remaining = maxTicksPerDay - state.ticksPlayedToday;
            if (remaining == THIRTY_MINUTES_TICKS) {
                limitedPlayer.sendMessage(
                        new StringTextComponent(TextFormatting.YELLOW + "[Playtime] 30 minutes remaining today. Take a break soon!"),
                        playerId);
            }

            if (remaining == TEN_MINUTES_TICKS) {
                limitedPlayer.sendMessage(
                        new StringTextComponent(TextFormatting.RED + "[Playtime] 10 minutes remaining today!"),
                        playerId);
            }

            if (remaining == ONE_MINUTE_TICKS) {
                limitedPlayer.sendMessage(
                        new StringTextComponent(TextFormatting.DARK_RED + "[Playtime] 1 minute remaining - you will be disconnected!"),
                        playerId);
            }

            if (state.ticksPlayedToday >= maxTicksPerDay) {
                limitedPlayer.connection.disconnect(buildLimitMessage(maxTicksPerDay, state.windowStartMs));
            }
        }
    }

    private static PlaytimeState getOrCreateState(UUID playerId) {
        return PLAYTIME_STATES.computeIfAbsent(playerId, id -> new PlaytimeState());
    }

    private static void maybeResetWindow(PlaytimeState state) {
        long now = System.currentTimeMillis();
        if (now - state.windowStartMs >= RESET_INTERVAL_MS) {
            state.ticksPlayedToday = 0;
            state.windowStartMs = now;
        }
    }

    private static StringTextComponent buildLimitMessage(long maxTicksPerDay, long windowStartMs) {
        long msRemaining = Math.max(0L, RESET_INTERVAL_MS - (System.currentTimeMillis() - windowStartMs));
        long hoursLeft = msRemaining / 3600000L;
        long minsLeft = (msRemaining % 3600000L) / 60000L;
        long hoursAllowed = maxTicksPerDay / (20L * 60L * 60L);
        long minutesAllowed = (maxTicksPerDay / (20L * 60L)) % 60L;
        String allowedTime = minutesAllowed > 0 ? hoursAllowed + "h " + minutesAllowed + "m" : hoursAllowed + "h";
        return new StringTextComponent(
                TextFormatting.RED + "You've reached your " + allowedTime + " daily playtime limit.\n" +
                        TextFormatting.YELLOW + "Resets in " + hoursLeft + "h " + minsLeft + "m.\n" +
                        TextFormatting.GRAY + "Take a break and come back tomorrow!"
        );
    }

    private static long hoursToTicks(double hours) {
        return Math.round(20.0D * 60.0D * 60.0D * hours);
    }

    private static class PlaytimeState {
        private long ticksPlayedToday = 0L;
        private long windowStartMs = System.currentTimeMillis();
    }
}
