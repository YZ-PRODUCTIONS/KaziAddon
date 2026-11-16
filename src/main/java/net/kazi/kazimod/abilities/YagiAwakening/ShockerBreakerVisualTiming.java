package net.kazi.kazimod.abilities.YagiAwakening;

/** Each warning produces one impact, a sustained discharge and a short fade, for both variants. */
public final class ShockerBreakerVisualTiming {
    public static final int CHARGE_TICKS = 40;
    public static final int STRIKE_TICKS = 58;
    public static final int FADE_TICKS = 8;
    public static final int LIFETIME = CHARGE_TICKS + STRIKE_TICKS + FADE_TICKS;
    public static final int EX_STRIKE_TICKS = STRIKE_TICKS;
    public static final int EX_FADE_TICKS = FADE_TICKS;
    private static final int IMPACT_FLASH_TICKS = 8;
    private static final int[] COLOR_TIMES = {0, 4, 10, 15, 20, 25, 35, 45};
    private static final float[][] COLORS = {
            {1F, .60F, .16F}, {1F, .94F, .30F}, {.45F, 1F, .26F}, {.20F, 1F, .90F},
            {.20F, .82F, 1F}, {.34F, .56F, 1F}, {.72F, .30F, 1F}, {1F, .28F, .82F}};

    private ShockerBreakerVisualTiming() { }

    public static float pulse(float strikeAge) {
        if (!Float.isFinite(strikeAge) || strikeAge < 0 || strikeAge >= IMPACT_FLASH_TICKS) return 0;
        return (float) Math.exp(-strikeAge * .67F);
    }

    public static int lifetime(boolean enhanced) {
        return LIFETIME;
    }

    public static float pulse(float strikeAge, boolean enhanced) {
        return pulse(strikeAge);
    }

    public static float columnStrength(float strikeAge, boolean enhanced) {
        return columnStrength(strikeAge);
    }

    public static float fade(float strikeAge, boolean enhanced) {
        return fade(strikeAge);
    }

    /** Only the initial impact emits a strike sound. */
    public static int pulseIndexAt(int strikeAge) {
        return strikeAge == 0 ? 0 : -1;
    }

    /** Raw column energy; the renderer applies fade once to the whole discharge. */
    public static float columnStrength(float strikeAge) {
        if (!Float.isFinite(strikeAge) || strikeAge < 0 || strikeAge >= STRIKE_TICKS + FADE_TICKS) return 0;
        return .68F + .32F * pulse(strikeAge);
    }

    /** Small continuous electrical variations, independent of the one-off impact flash and sound. */
    public static float sustainedFlicker(float strikeAge) {
        if (!Float.isFinite(strikeAge) || strikeAge < 0 || strikeAge >= STRIKE_TICKS + FADE_TICKS) return 0;
        return (float) (.86D + .09D * Math.cos(strikeAge * 2.7D) + .05D * Math.cos(strikeAge * 5.9D));
    }

    public static float fade(float strikeAge) {
        if (!Float.isFinite(strikeAge) || strikeAge < 0) return 0;
        return Math.max(0, Math.min(1, (STRIKE_TICKS + FADE_TICKS - strikeAge) / FADE_TICKS));
    }

    public static float[] color(float strikeAge) {
        float time = Float.isFinite(strikeAge) ? Math.max(0, Math.min(45, strikeAge)) : 0;
        int index = 0;
        while (index < COLORS.length - 2 && time >= COLOR_TIMES[index + 1]) index++;
        float blend = (time - COLOR_TIMES[index]) / (COLOR_TIMES[index + 1] - COLOR_TIMES[index]);
        float[] a = COLORS[index], b = COLORS[index + 1];
        return new float[]{a[0] + (b[0] - a[0]) * blend, a[1] + (b[1] - a[1]) * blend,
                a[2] + (b[2] - a[2]) * blend};
    }
}
