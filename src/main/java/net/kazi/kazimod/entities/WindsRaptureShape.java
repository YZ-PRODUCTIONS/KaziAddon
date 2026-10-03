package net.kazi.kazimod.entities;

/** Timing and gameplay envelope shared by the wind cast and its renderer. */
public final class WindsRaptureShape {
    public static final int CHARGE_TICKS = 60;
    public static final int DEPLOY_TICKS = 4;
    public static final int RELEASE_TICKS = 24;
    public static final int FADE_TICKS = 10;
    public static final float MAX_RANGE = 40;
    public static final float MAX_RADIUS = 2;

    private WindsRaptureShape() { }

    public static float length(float age, float range) {
        return Math.max(0, Math.min(MAX_RANGE, range)) * Math.max(0, Math.min(1, age / DEPLOY_TICKS));
    }

    public static boolean damaging(float age) { return age > 0 && age < RELEASE_TICKS; }

    public static float opacity(float age) {
        return Math.max(0, Math.min(1, (RELEASE_TICKS + FADE_TICKS - age) / FADE_TICKS));
    }
}
