import java.util.Set;
import java.util.TreeSet;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberPattern;
import net.kazi.kazimod.renderers.entities.asriel.ChaosSaberMotion;

/** Fractional-frame continuity checks for visual motion, independent of Minecraft and rendering. */
public final class ChaosSaberMotionTest {
    private static final double VISUAL_LENGTH = 11.0D;
    private static final double HAND_SCALE = VISUAL_LENGTH / 22.0D;
    private static int checks;

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private static void near(double value, double expected, double tolerance, String reason) {
        check(Math.abs(value - expected) <= tolerance, reason + ": " + value + " vs " + expected);
    }

    public static void main(String[] args) {
        Set<Float> boundaries = new TreeSet<>();
        for (float tick : new float[]{0, 4, 29, 128, 134, 150.5F, 151, 151.75F, 152, 153,
                153.5F, 154.5F, 158, 159, 179, 192}) {
            boundaries.add(tick);
        }
        for (int i = 0; i < ChaosSaberPattern.SINGLE_SLASHES; i++) {
            int windup = ChaosSaberPattern.windupTick(i), slash = ChaosSaberPattern.slashTick(i);
            for (float tick : new float[]{windup, windup + 6, slash - 0.5F, slash, slash + 0.75F,
                    ChaosSaberPattern.hitTick(i), slash + 2.5F, slash + 3.5F, slash + 7}) boundaries.add(tick);
            int side = ChaosSaberPattern.side(i);
            near(ChaosSaberMotion.pitch(windup, side), -1.18, 1.0E-9, "Windup begins at the held pose");
            near(ChaosSaberMotion.pitch(windup + 6, side), 0, 1.0E-9, "Sword drops to level before the slash");
            near(ChaosSaberMotion.yaw(windup, side), side * 0.24, 1.0E-9, "Yaw preparation begins at the held pose");
            near(ChaosSaberMotion.yaw(windup + 6, side), side * 1.15, 1.0E-9, "Sword opens toward its outer side");
            near(ChaosSaberMotion.pitch(ChaosSaberPattern.hitTick(i), side), 0, 1.0E-9,
                    "Blade remains horizontal on its damage tick");
            near(ChaosSaberMotion.yaw(ChaosSaberPattern.hitTick(i), side), side * 0.24, 1.0E-9,
                    "Horizontal cut crosses the existing damage direction on the hit tick");
            near(ChaosSaberMotion.pitch(slash + 2.5F, side), 0, 1.0E-9, "Follow-through stays horizontal");
            near(ChaosSaberMotion.yaw(slash + 2.5F, side), -side * 0.7, 1.0E-9, "Sword follows through across the front");
            near(ChaosSaberMotion.pitch(slash + 7, side), -1.18, 1.0E-9, "Faster recovery returns without snapping");
            near(ChaosSaberMotion.yaw(slash + 7, side), side * 0.24, 1.0E-9, "Yaw returns smoothly after seven ticks");
            assertHorizontalSweep(slash, side);
        }

        for (int side : new int[]{-1, 1}) {
            near(ChaosSaberMotion.pitch(ChaosSaberPattern.FINAL_HIT_TICK, side), 0, 1.0E-9,
                    "Final blades cross horizontal together");
            near(ChaosSaberMotion.yaw(ChaosSaberPattern.FINAL_HIT_TICK, side), side * 0.24, 1.0E-9,
                    "Final horizontal cuts reach the existing damage directions together");
            assertHorizontalSweep(ChaosSaberPattern.FINAL_SLASH_TICK, side);
            for (float boundary : boundaries) {
                float before = boundary - 0.001F, now = boundary, after = boundary + 0.001F;
                double p0 = ChaosSaberMotion.pitch(before, side);
                double p1 = ChaosSaberMotion.pitch(now, side);
                double p2 = ChaosSaberMotion.pitch(after, side);
                check(Math.abs(p2 - p0) < 0.003,
                        "No angular discontinuity at side " + side + " tick " + boundary);
                double leftVelocity = (p1 - p0) / (now - before);
                double rightVelocity = (p2 - p1) / (after - now);
                near(leftVelocity, rightVelocity, 0.02,
                        "Angular velocity remains continuous at side " + side + " tick " + boundary);
                double y0 = ChaosSaberMotion.yaw(before, side), y1 = ChaosSaberMotion.yaw(now, side);
                double y2 = ChaosSaberMotion.yaw(after, side);
                check(Math.abs(y2 - y0) < 0.003, "No yaw discontinuity at a phase boundary");
                near((y1 - y0) / (now - before), (y2 - y1) / (after - now), 0.02,
                        "Yaw velocity remains continuous at side " + side + " tick " + boundary);
                near(acceleration(boundary, side, false, -1), acceleration(boundary, side, false, 1), 0.03,
                        "Pitch acceleration remains continuous at side " + side + " tick " + boundary);
                near(acceleration(boundary, side, true, -1), acceleration(boundary, side, true, 1), 0.03,
                        "Yaw acceleration remains continuous at side " + side + " tick " + boundary);
                near(ChaosSaberMotion.handX(before, side), ChaosSaberMotion.handX(after, side), 0.001,
                        "No sideways jump at a phase boundary");
                near(ChaosSaberMotion.handY(before), ChaosSaberMotion.handY(after), 0.001,
                        "No vertical jump at a phase boundary");
                near(ChaosSaberMotion.handZ(before), ChaosSaberMotion.handZ(after), 0.001,
                        "No forward jump at a phase boundary");
                near(ChaosSaberMotion.bladeAlpha(before), ChaosSaberMotion.bladeAlpha(after), 0.002,
                        "Blade opacity does not pop at a phase boundary");
                near(ChaosSaberMotion.trailAlpha(before, side), ChaosSaberMotion.trailAlpha(after, side), 0.0031,
                        "A trail grows out of the actual movement instead of appearing as a slab");
            }
        }

        double maximumTipStep = 0, maximumAngularStep = 0;
        // At 60 fps, each visible sample advances one third of a Minecraft tick.
        for (int side : new int[]{-1, 1}) {
            double[] previousTip = tip(0, side);
            double previousPitch = ChaosSaberMotion.pitch(0, side);
            double previousYaw = ChaosSaberMotion.yaw(0, side);
            for (int frame = 1; frame <= ChaosSaberPattern.DURATION * 3; frame++) {
                float age = frame / 3.0F;
                double angle = ChaosSaberMotion.pitch(age, side);
                double yaw = ChaosSaberMotion.yaw(age, side);
                double[] position = tip(age, side);
                double displacement = distance(position, previousTip);
                maximumTipStep = Math.max(maximumTipStep, displacement);
                double angularStep = Math.hypot(angle - previousPitch, yaw - previousYaw);
                maximumAngularStep = Math.max(maximumAngularStep, angularStep);
                check(displacement < 4.2D, "The faster 11-block blade moves less per frame than its previous larger version");
                check(angularStep < 0.38D, "Faster combined angular travel stays bounded at 60 fps");
                check(angle >= -1.180001D && angle <= 0.000001D, "Pitch never dips below the level cutting plane");
                check(Math.abs(yaw) <= 1.150001D, "Yaw never overshoots its controlled horizontal arc");
                for (float opacity : new float[]{ChaosSaberMotion.bladeAlpha(age), ChaosSaberMotion.trailAlpha(age, side),
                        ChaosSaberMotion.shatterFraction(age), ChaosSaberMotion.sparkleAlpha(age)}) {
                    check(Float.isFinite(opacity) && opacity >= 0 && opacity <= 1, "Every visual weight stays bounded");
                }
                previousTip = position;
                previousPitch = angle;
                previousYaw = yaw;
            }
        }
        check(maximumAngularStep > 0.30D, "The horizontal stroke is visibly faster than the old five-tick sweep");
        check(maximumTipStep < 4.6056D, "Halving the visual size reduces maximum tip travel despite the faster turn");

        // Compare alternating hands at the same point in their respective sequences.
        for (float phase = 0; phase <= 20; phase += 0.25F) {
            float leftAge = ChaosSaberPattern.windupTick(0) + phase;
            float rightAge = ChaosSaberPattern.windupTick(1) + phase;
            near(ChaosSaberMotion.yaw(leftAge, -1), -ChaosSaberMotion.yaw(rightAge, 1), 1.0E-9,
                    "Alternating sword paths mirror each other");
            near(ChaosSaberMotion.pitch(leftAge, -1), ChaosSaberMotion.pitch(rightAge, 1), 1.0E-9,
                    "Alternating swords lower and recover identically");
        }
        for (float age = 128; age <= 160; age += 0.25F) {
            near(ChaosSaberMotion.yaw(age, -1), -ChaosSaberMotion.yaw(age, 1), 1.0E-9,
                    "Both final swords perform mirrored horizontal cuts");
        }

        near(ChaosSaberMotion.bladeAlpha(0), 0, 0, "Appearance starts transparent");
        near(ChaosSaberMotion.bladeAlpha(4), 1, 0, "Appearance finishes smoothly");
        near(ChaosSaberMotion.bladeAlpha(152), 1, 0, "The final hit still has a visible blade");
        near(ChaosSaberMotion.bladeAlpha(159), 0, 0, "Blade fragments are gone on schedule");
        near(ChaosSaberMotion.sparkleAlpha(152), 0, 0, "Sparkles fade into view");
        near(ChaosSaberMotion.sparkleAlpha(153), 1, 0, "Sparkles finish their short appearance");
        near(ChaosSaberMotion.sparkleAlpha(192), 0, 0, "Sparkles finish on the unchanged sequence boundary");
        near(ChaosSaberMotion.handX(29, 1), 1.1, 1.0E-9, "Arrival reaches the correct right-hand position");
        near(ChaosSaberMotion.handX(29, -1), -1.1, 1.0E-9, "Arrival reaches the correct left-hand position");
        near(ChaosSaberMotion.handY(29), 1, 1.0E-9, "Hands settle at the intended height");
        near(ChaosSaberMotion.handZ(29), 0, 1.0E-9, "Arrival arc ends in the held plane");
        for (float badAge : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            check(Double.isFinite(ChaosSaberMotion.pitch(badAge, 1)), "Invalid ages cannot corrupt model transforms");
            check(Double.isFinite(ChaosSaberMotion.yaw(badAge, -1)), "Invalid ages cannot corrupt horizontal rotation");
            check(Double.isFinite(ChaosSaberMotion.handX(badAge, -1)), "Invalid ages cannot corrupt hand positions");
            near(ChaosSaberMotion.bladeAlpha(badAge), 0, 0, "Invalid ages keep blades invisible");
            near(ChaosSaberMotion.trailAlpha(badAge, -1), 0, 0, "Invalid ages keep trails invisible");
        }
        System.out.println("Chaos Saber motion: " + checks + " checks; maximum 60 fps tip step "
                + maximumTipStep + " blocks, angular step " + maximumAngularStep + " radians.");
    }

    private static double[] tip(float age, int side) {
        double pitch = ChaosSaberMotion.pitch(age, side), yaw = ChaosSaberMotion.yaw(age, side);
        double forward = VISUAL_LENGTH * Math.cos(pitch);
        return new double[]{ChaosSaberMotion.handX(age, side) * HAND_SCALE + forward * Math.sin(yaw),
                ChaosSaberMotion.handY(age) * HAND_SCALE - VISUAL_LENGTH * Math.sin(pitch),
                ChaosSaberMotion.handZ(age) * HAND_SCALE + forward * Math.cos(yaw)};
    }

    private static void assertHorizontalSweep(int slash, int side) {
        float release = slash - 0.5F, follow = slash + 2.5F, hit = slash + 1.0F;
        near(follow - release, 3.0D, 0, "The complete horizontal sweep lasts three ticks instead of five");
        near((follow - release) / 5.0D, 0.6D, 0, "The sweep duration is reduced by forty percent");
        near(ChaosSaberMotion.yaw(release, side), side * 1.15D, 1.0E-9, "The shortened sweep begins at the outer pose");
        near(ChaosSaberMotion.yaw(release - 0.25F, side), side * 1.15D, 1.0E-9,
                "The old early start does not consume part of the shortened sweep");
        near(ChaosSaberMotion.yaw(follow, side), -side * 0.7D, 1.0E-9,
                "The shorter sweep still covers the entire arc");
        near(ChaosSaberMotion.trailAlpha(hit - 0.25F, side), 1.0D, 0,
                "The motion trail peaks just before the hit");
        near(ChaosSaberMotion.trailAlpha(hit + 2.5F, side), 0.0D, 0,
                "The shorter stroke leaves a bounded fading trail");
        float step = 1.0F / 128.0F;
        double impactVelocity = (ChaosSaberMotion.yaw(hit + step, side)
                - ChaosSaberMotion.yaw(hit - step, side)) / (2.0D * step);
        near(impactVelocity, -side * 1.1D, 0.0002D, "The faster cut maintains its intended velocity through impact");
        for (float boundary : new float[]{release, hit, follow}) {
            near(acceleration(boundary, side, true, -1), 0, 0.03,
                    "Yaw approaches every sweep join with zero acceleration");
            near(acceleration(boundary, side, true, 1), 0, 0.03,
                    "Yaw leaves every sweep join with zero acceleration");
        }
        double previousYaw = ChaosSaberMotion.yaw(release, side);
        for (float age = release; age <= follow; age += 0.0625F) {
            near(ChaosSaberMotion.pitch(age, side), 0, 1.0E-9, "Pitch stays level for the entire horizontal sweep");
            near(tip(age, side)[1], HAND_SCALE, 1.0E-9, "The smaller blade remains in one height plane throughout the sweep");
            double yaw = ChaosSaberMotion.yaw(age, side);
            check(side * (yaw - previousYaw) <= 1.0E-9, "Each horizontal cut moves consistently inward");
            check(side * yaw <= 1.15D + 1.0E-9 && side * yaw >= -0.7D - 1.0E-9,
                    "The faster quintic cut has no angular overshoot");
            previousYaw = yaw;
        }
    }

    /** One-sided second derivatives with an exactly representable step avoid float-age noise. */
    private static double acceleration(float age, int side, boolean yaw, int direction) {
        float step = direction / 128.0F;
        double a = sample(age, side, yaw), b = sample(age + step, side, yaw);
        double c = sample(age + 2 * step, side, yaw), d = sample(age + 3 * step, side, yaw);
        return (2 * a - 5 * b + 4 * c - d) / (step * step);
    }

    private static double sample(float age, int side, boolean yaw) {
        return yaw ? ChaosSaberMotion.yaw(age, side) : ChaosSaberMotion.pitch(age, side);
    }

    private static double distance(double[] a, double[] b) {
        double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
        return Math.sqrt(x * x + y * y + z * z);
    }
}
