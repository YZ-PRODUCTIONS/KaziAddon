package net.kazi.kazimod.abilities.NagiAwakening;

/** Shared cinematic clock and health-edge latch; independent of client or Minecraft classes. */
public final class InnerDragonsSequence {
    public static final int DURATION_TICKS = 160;
    public static final int ARM_BREAK_TICK = 24;
    public static final int EMERGENCE_TICK = 40;
    public static final int STAGGER_TICKS = 10;
    public static final int EMERGENCE_DURATION = 45;
    public static final double HEALTH_FRACTION = .30D;
    private final double threshold;
    public InnerDragonsSequence() { this(HEALTH_FRACTION); }
    private final boolean inclusive;
    private final int durationTicks;
    public InnerDragonsSequence(double threshold) { this(threshold, false, DURATION_TICKS); }
    public InnerDragonsSequence(double threshold, boolean inclusive, int durationTicks) {
        if (!Double.isFinite(threshold) || threshold <= 0 || threshold > 1 || durationTicks <= 0)
            throw new IllegalArgumentException("Invalid cinematic threshold or duration");
        this.threshold = threshold;
        this.inclusive = inclusive;
        this.durationTicks = durationTicks;
    }
    private boolean triggeredWhileLow;
    private long startTick = Long.MIN_VALUE;

    public static boolean belowThreshold(double health, double maxHealth) {
        return Double.isFinite(health) && Double.isFinite(maxHealth)
                && health > 0 && maxHealth > 0 && health < maxHealth * HEALTH_FRACTION;
    }

    /** Returns true only on the first eligible low-health tick of a health cycle. */
    public boolean update(double health, double maxHealth, long now, boolean eligible) {
        if (!(Double.isFinite(health) && Double.isFinite(maxHealth) && health > 0 && maxHealth > 0)) {
            triggeredWhileLow = false;
            stop();
            return false;
        }
        boolean low = inclusive ? health <= maxHealth * threshold : health < maxHealth * threshold;
        if (!low) triggeredWhileLow = false;
        if (!eligible) {
            // Suppression must not repeatedly replay the scene while health stays low.
            stop();
            return false;
        }
        // Health only triggers a scene; healing cannot truncate or restart its clock.
        if (isPlaying(now)) {
            if (low) triggeredWhileLow = true;
            return false;
        }
        if (!low) { stop(); return false; }
        if (triggeredWhileLow) return false;
        triggeredWhileLow = true;
        startTick = now;
        return true;
    }

    public boolean isPlaying(long now) {
        return startTick != Long.MIN_VALUE && now >= startTick && now - startTick < durationTicks;
    }

    /** Evaluate after update and any cooldown-driven stop: the replacement outlasts the scene. */
    public boolean shouldTransformArm(long now, boolean eligible) {
        return eligible && (!isPlaying(now) || now - startTick >= ARM_BREAK_TICK);
    }

    public long startTick() { return startTick; }
    public void stop() { startTick = Long.MIN_VALUE; }

    public static boolean active(float age) {
        return Float.isFinite(age) && age >= 0 && age < DURATION_TICKS;
    }

    /** Smooth, staggered emergence. Completed dragons remain fully emerged until the handoff. */
    public static float emergence(int kind, float age) {
        if (kind < 0 || kind >= 8 || !Float.isFinite(age)) return 0;
        float t = Math.max(0, Math.min(1, (age - EMERGENCE_TICK - kind * STAGGER_TICKS) / EMERGENCE_DURATION));
        return t * t * (3 - 2 * t);
    }
}
