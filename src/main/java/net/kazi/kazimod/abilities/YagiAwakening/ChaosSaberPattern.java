package net.kazi.kazimod.abilities.YagiAwakening;

/** Normal Chaos Saber's complete 30 fps source sequence, expressed in Minecraft ticks. */
public final class ChaosSaberPattern {
    public static final double LENGTH = 88.0D;
    public static final double SLASH_RADIUS = 7.2D;
    public static final double SPARK_RADIUS = 20.0D;
    public static final int APPEAR_TICK = 0;
    public static final int GRAB_TICK = 29;
    public static final int SINGLE_SLASHES = 5;
    public static final int FINAL_CHARGE_TICK = 128;
    public static final int FINAL_SLASH_TICK = 151;
    public static final int FINAL_HIT_TICK = 152;
    public static final int SPARKLE_TICK = 152;
    public static final int SWORD_FADE_END_TICK = 159;
    public static final int SPARKLE_FADE_TICK = 179;
    public static final int DURATION = 192;
    public static final int PULLBACK_DURATION = 6;
    public static final int SWING_DURATION = 2;
    public static final int SMEAR_DURATION = 4;
    public static final int RECOVERY_DURATION = 5;

    private static final int[] WINDUPS = {38, 56, 74, 92, 110};
    private static final int[] SLASHES = {49, 67, 85, 103, 121};
    private static final int[] SIDES = {-1, 1, -1, 1, -1};
    private static final int[] SPARK_DAMAGE = {160, 174};

    private ChaosSaberPattern() {}

    public static int windupTick(int index) { return WINDUPS[index]; }
    public static int slashTick(int index) { return SLASHES[index]; }
    /** The source's blade collision starts one frame after its bright slash smear. */
    public static int hitTick(int index) { return SLASHES[index] + 1; }
    public static int side(int index) { return SIDES[index]; }
    public static int sparkDamageTick(int index) { return SPARK_DAMAGE[index]; }

    public static float arrivalFraction(float age) {
        return fraction(age, APPEAR_TICK, GRAB_TICK);
    }

    /** Raise the chosen sword over six ticks, then hold until the slash starts. */
    public static float pullbackFraction(float age, int index) {
        if (!Float.isFinite(age) || age >= slashTick(index)) return 0.0F;
        return fraction(age, windupTick(index), windupTick(index) + PULLBACK_DURATION);
    }

    /** Fast deployment; the renderer can recover the resting pose over RECOVERY_DURATION. */
    public static float swingFraction(float age, int index) {
        return fraction(age, slashTick(index), slashTick(index) + SWING_DURATION);
    }

    public static float smearAlpha(float age, int index) {
        return smear(age, slashTick(index));
    }

    public static float finalChargeFraction(float age) {
        return fraction(age, FINAL_CHARGE_TICK, FINAL_SLASH_TICK);
    }

    public static float finalSmearAlpha(float age) {
        return smear(age, FINAL_SLASH_TICK);
    }

    /** Arrival is handled by position; the blades stay solid until they shatter after the finale. */
    public static float swordAlpha(float age) {
        if (!Float.isFinite(age) || age < APPEAR_TICK) return 0.0F;
        return 1.0F - fraction(age, SPARKLE_TICK, SWORD_FADE_END_TICK);
    }

    public static float sparkleAlpha(float age) {
        if (!Float.isFinite(age) || age < SPARKLE_TICK) return 0.0F;
        return 1.0F - fraction(age, SPARKLE_FADE_TICK, DURATION);
    }

    private static float smear(float age, int start) {
        if (!Float.isFinite(age) || age < start) return 0.0F;
        return 1.0F - fraction(age, start, start + SMEAR_DURATION);
    }

    private static float fraction(float age, int start, int end) {
        if (!Float.isFinite(age)) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, (age - start) / (float) (end - start)));
    }
}
