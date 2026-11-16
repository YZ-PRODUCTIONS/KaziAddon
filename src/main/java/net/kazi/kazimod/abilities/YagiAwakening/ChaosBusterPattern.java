package net.kazi.kazimod.abilities.YagiAwakening;

/** The original 30 fps cannon sequence, quantized once per event to Minecraft ticks. */
public final class ChaosBusterPattern {
    public static final int BEAM_START_TICK = 185;
    public static final int CHARGE_START_TICK = 151;
    public static final int AIM_LOCK_TICK = 173;
    public static final int BEAM_LIFETIME = 66;
    public static final int BEAM_SHRINK_TICKS = 16;
    public static final double BEAM_RANGE = 96.0D;
    /** Preserve the reference's travel time across a 48-block battlefield. */
    public static final double BOLT_SPEED = 48.0D / 485.0D * 20.0D * 30.0D / 20.0D;
    public static final double BEAM_RADIUS = 48.0D / 485.0D * 70.0D / 2.0D;
    public static final double BLASTER_BEAM_RADIUS = 3.2D * 2.0D;

    private static final int SOURCE_PRELUDE_FRAMES = 10;
    private static final float NORMAL_FILL_TICKS = 7.0F * 20.0F / 30.0F;
    private static final Group[] GROUPS = {
            new Group(2, 20, 3, 6),
            new Group(55, 10, 4, 6),
            new Group(81, 8, 3, 5),
            new Group(105, 8, 4, 5),
            new Group(127, 8, 3, 4),
            new Group(149, 8, 4, 4),
            new Group(167, 6, 3, 4),
            new Group(183, 6, 4, 4),
            new Group(197, 6, 3, 8)
    };

    private ChaosBusterPattern() {}

    private static int sourceTick(int frame) {
        return Math.round((frame + SOURCE_PRELUDE_FRAMES) * 20.0F / 30.0F);
    }

    public static int groupCount() { return GROUPS.length; }
    public static Group group(int index) { return GROUPS[index]; }

    /** Hold the full beam for the added two seconds, then retain its original narrowing tail. */
    public static double beamRadiusScale(float age, int duration) {
        if (!Float.isFinite(age) || age < 0 || duration <= 0 || age >= duration) return 0;
        double shrink = Math.max(0.0D, Math.min(1.0D,
                (age - (duration - BEAM_SHRINK_TICKS)) / (double) BEAM_SHRINK_TICKS));
        return 1.0D - shrink;
    }

    /** The warning ends on the first firing tick; earlier pellets can still be in flight. */
    public static int telegraphGroup(int tick) {
        for (int i = 0; i < GROUPS.length; i++) {
            Group group = GROUPS[i];
            if (tick >= group.warningTick && tick < group.firstShotTick) return i;
        }
        return -1;
    }

    /** Enhanced casts start at age zero; normal casts begin filling during their last volley. */
    public static float chargeFraction(float age, boolean enhanced) {
        if (!Float.isFinite(age)) return 0.0F;
        float start = enhanced ? 0.0F : CHARGE_START_TICK;
        float duration = enhanced ? 20.0F : NORMAL_FILL_TICKS;
        return Math.max(0.0F, Math.min(1.0F, (age - start) / duration));
    }

    /** A complete turn returns to the original pose before the cannon locks its aim. */
    public static float cannonSpinDegrees(float age) {
        if (!Float.isFinite(age) || age <= 161.0F || age >= 171.0F) return 0.0F;
        float progress = (age - 161.0F) / 10.0F;
        return 360.0F * progress * progress * (3.0F - 2.0F * progress);
    }

    public static final class Group {
        public final int warningTick;
        public final int firstShotTick;
        public final int volleyCount;
        public final int lanes;
        private final int firstShotSourceFrame;

        private Group(int warningSourceFrame, int warningFrames, int lanes, int volleys) {
            this.warningTick = sourceTick(warningSourceFrame);
            this.firstShotSourceFrame = warningSourceFrame + warningFrames;
            this.firstShotTick = sourceTick(firstShotSourceFrame);
            this.volleyCount = volleys;
            this.lanes = lanes;
        }

        /** Round the absolute source frame, avoiding accumulated drift between volleys. */
        public int volleyTick(int index) {
            if (index < 0 || index >= volleyCount) throw new IndexOutOfBoundsException("volley " + index);
            return sourceTick(firstShotSourceFrame + 2 * index);
        }

        public double[] shotAnglesRadians() {
            return lanes == 3 ? radians(-20, 0, 20) : radians(-30, -10, 10, 30);
        }

        public double[] warningAnglesRadians() {
            return lanes == 3 ? radians(-14, 0, 13) : radians(-20, -6, 6, 20);
        }

        private static double[] radians(double... degrees) {
            for (int i = 0; i < degrees.length; i++) degrees[i] = Math.toRadians(degrees[i]);
            return degrees;
        }
    }
}
