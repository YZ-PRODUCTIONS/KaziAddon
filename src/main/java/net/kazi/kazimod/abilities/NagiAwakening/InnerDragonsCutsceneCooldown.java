package net.kazi.kazimod.abilities.NagiAwakening;

/** Replay scheduling uses server game time, independently of health and ability instances. */
public final class InnerDragonsCutsceneCooldown {
    public static final int COOLDOWN_TICKS = 10 * 60 * 20;

    private InnerDragonsCutsceneCooldown() { }

    public static boolean ready(long now, long nextAllowedTick) {
        return now >= 0 && (nextAllowedTick <= 0 || now >= nextAllowedTick);
    }

    public static long nextAllowedTick(long now) {
        long start = Math.max(0, now);
        return start > Long.MAX_VALUE - COOLDOWN_TICKS ? Long.MAX_VALUE : start + COOLDOWN_TICKS;
    }
}
