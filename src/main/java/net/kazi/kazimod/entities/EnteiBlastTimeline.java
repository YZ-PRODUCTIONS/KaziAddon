package net.kazi.kazimod.entities;

public final class EnteiBlastTimeline {
    public static final int SWELL_TICKS = 12;
    public static final int EXPAND_TICKS = 16;
    public static final int FADE_TICKS = 35;

    private EnteiBlastTimeline() {
    }

    public static float advance(float previous, float candidate, float maximum) {
        float step = Math.max(0.25f, maximum * 1.5f / 16.0f);
        return Math.max(previous, Math.min(candidate, previous + step));
    }

    public static float radius(float age, float initial, float maximum) {
        maximum = Math.max(initial, maximum);
        if (age < 12.0f) {
            return initial;
        }
        float progress = Math.max(0.0f, Math.min(1.0f, (age - 12.0f + 1.0f) / 16.0f));
        return initial + (maximum - initial) * (float)Math.pow(progress, 0.7);
    }
}
