package net.kazi.kazimod.entities.boss.gojo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last-fired timestamp for the Gojo boss's Max Output Lapse Blue
 * and Red abilities so they cannot be used within 5 seconds of each other.
 * This gate is only active before hollowNukeQueued becomes true.
 */
public class GojoCooldownTracker {

    /** 5 seconds in milliseconds. */
    public static final long COOLDOWN_MS = 5000L;

    /** Maps boss UUID → System.currentTimeMillis() of last Max Output Lapse Blue fire. */
    public static final Map<UUID, Long> LAST_MAX_BLUE_TIME = new ConcurrentHashMap<>();

    /** Maps boss UUID → System.currentTimeMillis() of last Red fire. */
    public static final Map<UUID, Long> LAST_RED_TIME = new ConcurrentHashMap<>();

    /**
     * Returns true if the given tracker shows a fire within the last COOLDOWN_MS.
     */
    public static boolean isOnCooldown(Map<UUID, Long> tracker, UUID entityId) {
        Long last = tracker.get(entityId);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < COOLDOWN_MS;
    }

    /** Call this when Max Output Lapse Blue fires. */
    public static void recordMaxBlue(UUID entityId) {
        LAST_MAX_BLUE_TIME.put(entityId, System.currentTimeMillis());
    }

    /** Call this when Red fires. */
    public static void recordRed(UUID entityId) {
        LAST_RED_TIME.put(entityId, System.currentTimeMillis());
    }
}