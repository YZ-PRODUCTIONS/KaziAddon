package net.kazi.kazimod.client.asriel;

/** Asriel's intro cadence, converted from the original thirty-frame clock to game ticks. */
final class ChaosBeamTitleMotion {
    static final int DURATION_TICKS = 134;
    static final int LETTER_TICKS = 4;
    static final double FIRST_LETTER_TICK = 4D / 3D;
    static final int LAYERS = 7;
    private static final float[] OPACITY = {.9F, .7F, .5F, .4F, .3F, .2F, .1F};
    private static final int[] SPREAD = {2, 2, 8, 12, 16, 20, 24};

    private ChaosBeamTitleMotion() { }

    static boolean active(double age) {
        return Double.isFinite(age) && age >= 0 && age < DURATION_TICKS;
    }

    static int visibleCharacters(double age, int length) {
        if (!active(age) || frame(age) < 2) return 0;
        return Math.min(Math.max(0, length), 1 + (int)((frame(age) - 2) / 6));
    }

    static float opacity(int layer) { return OPACITY[layer]; }

    /** Ease all text layers to transparent over the final second. */
    static float opacity(int layer, double age) {
        if (!active(age)) return 0;
        double remaining = Math.max(0, Math.min(1, (DURATION_TICKS - age) / 20D));
        return OPACITY[layer] * (float)(remaining * remaining * (3 - 2 * remaining));
    }

    static double offset(double age, int layer, boolean vertical, long seed) {
        long frame = frame(age);
        long value = seed ^ frame * 0x9e3779b97f4a7c15L ^ (layer * 2L + (vertical ? 1 : 0)) * 0x632be59bd9b4e019L;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        value ^= value >>> 31;
        int spread = SPREAD[layer];
        double unit = (value >>> 11) * 0x1.0p-53;
        return unit * spread * 2 - spread - (layer == 1 && vertical ? 1 : 0);
    }

    private static long frame(double age) { return (long)Math.floor(age * 1.5D + 1.0E-9D); }
}
