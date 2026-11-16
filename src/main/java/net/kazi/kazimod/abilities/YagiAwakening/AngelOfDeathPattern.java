package net.kazi.kazimod.abilities.YagiAwakening;

/** Final-form Asriel's alternating hand volleys, converted from 30 fps to Minecraft ticks. */
public final class AngelOfDeathPattern {
    public static final int DURATION = 107;
    public static final int SHOT_COUNT = 41;
    public static final int MAX_LIFE = 80;
    public static final int ASSIST_DELAY_TICKS = 6;
    public static final double SPEED = 1.45D;
    public static final double MAX_TURN_RADIANS = Math.toRadians(8.0D);
    public static final double MAX_TOTAL_TURN_RADIANS = Math.PI;

    private static final int PRELUDE_FRAMES = 20;
    private static final int[] FLASH_FRAMES = {8, 28, 48, 68, 103};
    private static final int[] FIRST_SHOT_FRAMES = {11, 31, 51, 71, 106};
    private static final int[] SHOTS_PER_VOLLEY = {8, 8, 7, 7, 11};
    private static final int[] FIRST_SHOT_INDEX = {0, 8, 16, 23, 30};
    // Stable irregular lanes span the source's randomized outward fan without client-side randomness.
    private static final double[] FAN_LANES = {.88, .18, .61, .02, .75, .34, 1, .47, .09, .93, .56};

    private AngelOfDeathPattern() { }

    public static int volleyIndex(int shot) {
        checkShot(shot);
        for (int volley = FIRST_SHOT_INDEX.length - 1; volley >= 0; volley--)
            if (shot >= FIRST_SHOT_INDEX[volley]) return volley;
        throw new AssertionError("Missing Angel of death volley");
    }

    /** Quantize each absolute source frame, allowing two source shots to share a server tick. */
    public static int shotTick(int shot) {
        int volley = volleyIndex(shot);
        return sourceTick(PRELUDE_FRAMES + FIRST_SHOT_FRAMES[volley] + shot - FIRST_SHOT_INDEX[volley]);
    }

    /** Negative is the caster's left hand; the final eleven-shot fan alternates both hands. */
    public static int side(int shot) {
        int volley = volleyIndex(shot);
        return volley == 4 ? ((shot - FIRST_SHOT_INDEX[4]) % 2 == 0 ? -1 : 1)
                : (volley % 2 == 0 ? -1 : 1);
    }

    public static int flashCount() { return FLASH_FRAMES.length; }

    public static int flashTick(int volley) {
        checkVolley(volley);
        return sourceTick(PRELUDE_FRAMES + FLASH_FRAMES[volley]);
    }

    /** The final hand flash is shared by both hands. */
    public static int flashSide(int volley) {
        checkVolley(volley);
        return volley == 4 ? 0 : (volley % 2 == 0 ? -1 : 1);
    }

    /** Local right/up/forward. Screen-down in the reference becomes forward travel in the world. */
    public static double[] initialDirection(int shot) {
        int volley = volleyIndex(shot);
        int lane = shot - FIRST_SHOT_INDEX[volley];
        double spread = FAN_LANES[lane];
        double outward = 9.0D + 8.0D * spread;
        double forward = 4.0D + 10.0D * (1.0D - spread);
        double length = Math.sqrt(outward * outward + forward * forward);
        return new double[]{side(shot) * outward / length, 0.0D, forward / length};
    }

    /**
     * Preserve the launch fan first, then turn at most eight degrees per tick toward the supplied
     * target offset. Callers may supply a fixed aim point or a visible enemy; this never teleports.
     */
    public static double[] steer(double vx, double vy, double vz, double tx, double ty, double tz, float age) {
        return steerLimited(vx, vy, vz, tx, ty, tz, age, MAX_TURN_RADIANS);
    }

    private static double[] steerLimited(double vx, double vy, double vz, double tx, double ty, double tz,
                                         float age, double maxTurn) {
        if (!finite(vx, vy, vz) || !finite(tx, ty, tz) || !Float.isFinite(age)) return new double[]{0, 0, 0};
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed < 1.0E-9D) return new double[]{0, 0, 0};
        double ax = vx / speed, ay = vy / speed, az = vz / speed;
        double targetLength = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (age < ASSIST_DELAY_TICKS || targetLength < 1.0E-9D)
            return new double[]{ax * SPEED, ay * SPEED, az * SPEED};
        double bx = tx / targetLength, by = ty / targetLength, bz = tz / targetLength;
        double dot = Math.max(-1.0D, Math.min(1.0D, ax * bx + ay * by + az * bz));
        double angle = Math.acos(dot);
        if (angle <= maxTurn) return new double[]{bx * SPEED, by * SPEED, bz * SPEED};

        // Resolve the desired direction into the tangent plane so a nearly opposite target stays finite.
        double px = bx - ax * dot, py = by - ay * dot, pz = bz - az * dot;
        double perpendicularLength = Math.sqrt(px * px + py * py + pz * pz);
        if (perpendicularLength < 1.0E-9D) {
            if (Math.abs(ay) < .9D) { px = -az; py = 0; pz = ax; }
            else { px = ay; py = -ax; pz = 0; }
            perpendicularLength = Math.sqrt(px * px + py * py + pz * pz);
        }
        double c = Math.cos(maxTurn), s = Math.sin(maxTurn) / perpendicularLength;
        return new double[]{(ax * c + px * s) * SPEED, (ay * c + py * s) * SPEED,
                (az * c + pz * s) * SPEED};
    }

    /** Per-projectile guidance state. Once a beam reaches or passes its goal it keeps flying onward. */
    public static final class Guidance {
        private boolean approaching;
        private boolean complete;
        private double totalTurn;

        public boolean isComplete() { return complete; }

        public double[] steer(double vx, double vy, double vz, double tx, double ty, double tz, float age) {
            if (!finite(vx, vy, vz) || !finite(tx, ty, tz) || !Float.isFinite(age)) return new double[]{0, 0, 0};
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (speed < 1.0E-9D) return new double[]{0, 0, 0};
            double[] straight = {vx / speed * SPEED, vy / speed * SPEED, vz / speed * SPEED};
            if (age < ASSIST_DELAY_TICKS || complete) return straight;
            double distanceSquared = tx * tx + ty * ty + tz * tz;
            double alignment = distanceSquared < 1.0E-18D ? 0
                    : (tx * vx + ty * vy + tz * vz) / Math.sqrt(distanceSquared) / speed;
            if (alignment > .75D) approaching = true;
            if (distanceSquared < .25D || approaching && alignment < 0) {
                complete = true;
                return straight;
            }
            // Close targets can remain inside the minimum turn radius. One inward half-turn is
            // enough assistance; exhausting this budget prevents those beams from circling forever.
            double remaining = Math.max(0, MAX_TOTAL_TURN_RADIANS - totalTurn);
            double[] next = steerLimited(vx, vy, vz, tx, ty, tz, age, Math.min(MAX_TURN_RADIANS, remaining));
            double dot = (vx * next[0] + vy * next[1] + vz * next[2]) / speed / SPEED;
            totalTurn += Math.acos(Math.max(-1, Math.min(1, dot)));
            if (totalTurn >= MAX_TOTAL_TURN_RADIANS - 1.0E-7D) complete = true;
            return next;
        }
    }

    private static int sourceTick(int frame) { return Math.round(frame * 20.0F / 30.0F); }
    private static boolean finite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
    private static void checkShot(int shot) {
        if (shot < 0 || shot >= SHOT_COUNT) throw new IndexOutOfBoundsException("Angel of death shot " + shot);
    }
    private static void checkVolley(int volley) {
        if (volley < 0 || volley >= SHOTS_PER_VOLLEY.length)
            throw new IndexOutOfBoundsException("Angel of death volley " + volley);
    }
}
