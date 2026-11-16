package net.kazi.kazimod.renderers.entities.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberPattern;

/** Continuous visual poses only. Combat timing and dimensions remain in ChaosSaberPattern. */
public final class ChaosSaberMotion {
    public static final double RAISED_PITCH = -1.18D;
    public static final double PULLED_BACK_PITCH = 0.0D;
    public static final double FOLLOW_THROUGH_PITCH = 0.0D;
    private static final double REST_YAW = 0.24D;
    private static final double OUTSIDE_YAW = 1.15D;
    private static final double FOLLOW_THROUGH_YAW = -0.7D;
    private static final double IMPACT_YAW_VELOCITY = -1.1D;
    private static final double SWEEP_HALF_DURATION = 1.5D;
    private static final int RECOVERY_END = 7;

    private ChaosSaberMotion() {}

    /** Radians around the blade's local X axis; negative angles hold its tip above the caster. */
    public static double pitch(float age, int side) {
        return motion(age, side, false);
    }

    /** Mirrored horizontal cuts pass through the unchanged combat direction on the hit tick. */
    public static double yaw(float age, int side) {
        return motion(age, side, true);
    }

    private static double motion(float age, int side, boolean horizontal) {
        int hand = side < 0 ? -1 : 1;
        double rest = horizontal ? hand * REST_YAW : RAISED_PITCH;
        if (!Float.isFinite(age)) return rest;
        int finalPullback = finalPullbackTick(hand);
        if (age >= finalPullback) {
            return horizontal ? hand * strokeYaw(age, finalPullback, ChaosSaberPattern.FINAL_SLASH_TICK,
                    ChaosSaberPattern.FINAL_HIT_TICK)
                    : strokePitch(age, finalPullback, ChaosSaberPattern.FINAL_SLASH_TICK,
                            ChaosSaberPattern.FINAL_HIT_TICK);
        }
        for (int i = 0; i < ChaosSaberPattern.SINGLE_SLASHES; i++) {
            if (ChaosSaberPattern.side(i) == hand && age >= ChaosSaberPattern.windupTick(i)
                    && age < ChaosSaberPattern.slashTick(i) + RECOVERY_END) {
                return horizontal ? hand * strokeYaw(age, ChaosSaberPattern.windupTick(i), ChaosSaberPattern.slashTick(i),
                        ChaosSaberPattern.hitTick(i))
                        : strokePitch(age, ChaosSaberPattern.windupTick(i), ChaosSaberPattern.slashTick(i),
                                ChaosSaberPattern.hitTick(i));
            }
        }
        return rest;
    }

    private static int finalPullbackTick(int side) {
        int ready = ChaosSaberPattern.FINAL_CHARGE_TICK;
        for (int i = 0; i < ChaosSaberPattern.SINGLE_SLASHES; i++) {
            if (ChaosSaberPattern.side(i) == side) {
                ready = Math.max(ready, ChaosSaberPattern.slashTick(i) + RECOVERY_END);
            }
        }
        return ready;
    }

    private static double strokePitch(double age, int windup, int slash, int hit) {
        double release = hit - SWEEP_HALF_DURATION;
        double pulledBack = Math.min(windup + ChaosSaberPattern.PULLBACK_DURATION, release);
        if (age < pulledBack) return blend(RAISED_PITCH, PULLED_BACK_PITCH, ease(age, windup, pulledBack));
        double follow = hit + SWEEP_HALF_DURATION;
        // Lower first, then keep the entire cutting arc in one horizontal plane.
        if (age <= follow) return 0.0D;
        return blend(FOLLOW_THROUGH_PITCH, RAISED_PITCH, ease(age, follow, slash + RECOVERY_END));
    }

    private static double strokeYaw(double age, int windup, int slash, int hit) {
        double release = hit - SWEEP_HALF_DURATION;
        double pulledBack = Math.min(windup + ChaosSaberPattern.PULLBACK_DURATION, release);
        if (age < pulledBack) return blend(REST_YAW, OUTSIDE_YAW, ease(age, windup, pulledBack));
        if (age < release) return OUTSIDE_YAW;
        // One three-tick sweep, with matching velocity and acceleration through the hit direction.
        if (age < hit) return quinticArc(age, release, hit, OUTSIDE_YAW, REST_YAW, 0.0D, IMPACT_YAW_VELOCITY);
        double follow = hit + SWEEP_HALF_DURATION;
        if (age < follow) return quinticArc(age, hit, follow, REST_YAW, FOLLOW_THROUGH_YAW, IMPACT_YAW_VELOCITY, 0.0D);
        return blend(FOLLOW_THROUGH_YAW, REST_YAW, ease(age, follow, slash + RECOVERY_END));
    }

    /** Hand offsets use the renderer's existing visualLength/22 scale. */
    public static double handX(float age, int side) {
        return (side < 0 ? -1.0D : 1.0D) * (1.1D + 4.0D * (1.0D - arrival(age)));
    }

    public static double handY(float age) {
        double arrival = arrival(age);
        return 1.0D + 1.5D * (1.0D - arrival) + 0.4D * Math.sin(Math.PI * arrival);
    }

    public static double handZ(float age) {
        return -0.65D * Math.sin(Math.PI * arrival(age));
    }

    private static double arrival(float age) {
        return ease(age, ChaosSaberPattern.APPEAR_TICK, ChaosSaberPattern.GRAB_TICK);
    }

    public static float bladeAlpha(float age) {
        if (!Float.isFinite(age) || age < 0.0F) return 0.0F;
        return (float) (ease(age, 0.0D, 4.0D) * (1.0D - shatterFraction(age)));
    }

    public static float shatterFraction(float age) {
        return (float) ease(age, ChaosSaberPattern.SPARKLE_TICK, ChaosSaberPattern.SWORD_FADE_END_TICK);
    }

    /** Weight actual earlier poses with this envelope; never introduce an unrelated crescent mesh. */
    public static float trailAlpha(float age, int side) {
        if (!Float.isFinite(age)) return 0.0F;
        double alpha = trail(age, ChaosSaberPattern.FINAL_SLASH_TICK);
        int hand = side < 0 ? -1 : 1;
        for (int i = 0; i < ChaosSaberPattern.SINGLE_SLASHES; i++) {
            if (ChaosSaberPattern.side(i) == hand) alpha = Math.max(alpha, trail(age, ChaosSaberPattern.slashTick(i)));
        }
        return (float) alpha;
    }

    public static float sparkleAlpha(float age) {
        if (!Float.isFinite(age)) return 0.0F;
        return (float) (ease(age, ChaosSaberPattern.SPARKLE_TICK, ChaosSaberPattern.SPARKLE_TICK + 1.0D)
                * (1.0D - ease(age, ChaosSaberPattern.SPARKLE_FADE_TICK, ChaosSaberPattern.DURATION)));
    }

    private static double trail(double age, int slash) {
        double hit = slash + 1.0D;
        double peak = hit - 0.25D;
        if (age < peak) return ease(age, hit - SWEEP_HALF_DURATION, peak);
        return 1.0D - ease(age, peak, hit + SWEEP_HALF_DURATION + 1.0D);
    }

    private static double blend(double from, double to, double fraction) {
        return from + (to - from) * fraction;
    }

    /** Quintic easing has zero velocity and acceleration at each end of a held pose. */
    private static double ease(double age, double start, double end) {
        if (!Double.isFinite(age)) return 0.0D;
        double t = Math.max(0.0D, Math.min(1.0D, (age - start) / (end - start)));
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    /** Zero endpoint acceleration avoids a visible kick when the fast cut begins or changes phase. */
    private static double quinticArc(double age, double start, double end,
                                     double from, double to, double startVelocity, double endVelocity) {
        double duration = end - start;
        double t = Math.max(0.0D, Math.min(1.0D, (age - start) / duration));
        double delta = to - from, m0 = duration * startVelocity, m1 = duration * endVelocity;
        double a3 = 10.0D * delta - 6.0D * m0 - 4.0D * m1;
        double a4 = -15.0D * delta + 8.0D * m0 + 7.0D * m1;
        double a5 = 6.0D * delta - 3.0D * m0 - 3.0D * m1;
        return from + m0 * t + t * t * t * (a3 + t * (a4 + t * a5));
    }
}
