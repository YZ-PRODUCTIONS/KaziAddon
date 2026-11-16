import java.util.HashSet;
import java.util.Set;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterPattern;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningMath;

/** Checks the source pattern against the GIF, without launching Minecraft. */
public final class ChaosBusterPatternTest {
    private static int checks;

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private static void near(double actual, double expected, double tolerance, String reason) {
        check(Math.abs(actual - expected) <= tolerance, reason);
    }

    public static void main(String[] args) {
        int[] warningTicks = {8, 43, 61, 77, 91, 106, 118, 129, 138};
        int[] firstTicks = {21, 50, 66, 82, 97, 111, 122, 133, 142};
        int[] lastTicks = {28, 57, 71, 87, 101, 115, 126, 137, 151};
        int[] volleys = {6, 6, 5, 5, 4, 4, 4, 4, 8};
        int[] visibleGifFrames = {16, 38, 50, 61, 73, 84, 91, 100, 107};
        Set<Integer> firingTicks = new HashSet<>();
        int totalVolleys = 0, totalShots = 0;
        check(ChaosBusterPattern.groupCount() == 9, "The reference has nine bursts");
        for (int i = 0; i < ChaosBusterPattern.groupCount(); i++) {
            ChaosBusterPattern.Group group = ChaosBusterPattern.group(i);
            check(group.warningTick == warningTicks[i], "Warning onset " + i);
            check(group.firstShotTick == firstTicks[i], "First-shot onset " + i);
            check(group.volleyCount == volleys[i], "Volley count " + i);
            check(group.lanes == (i % 2 == 0 ? 3 : 4), "Alternate three and four lanes");
            check(group.volleyTick(0) == group.firstShotTick, "First volley matches the firing transition");
            check(group.volleyTick(group.volleyCount - 1) == lastTicks[i], "No accumulated rounding drift");
            check(ChaosBusterPattern.telegraphGroup(group.warningTick) == i, "Warning begins on its first tick");
            check(ChaosBusterPattern.telegraphGroup(group.firstShotTick - 1) == i, "Warning lasts until firing");
            check(ChaosBusterPattern.telegraphGroup(group.firstShotTick) == -1, "Warning is hidden during firing");
            near(group.firstShotTick / 20.0, visibleGifFrames[i] / 15.0, 1.0 / 15.0,
                    "Burst " + i + " agrees with the GIF within one sampled frame");
            for (int volley = 0; volley < group.volleyCount; volley++) {
                int tick = group.volleyTick(volley);
                check(firingTicks.add(tick), "Quantization must not merge separate firing events");
                if (volley > 0) {
                    int gap = tick - group.volleyTick(volley - 1);
                    check(gap == 1 || gap == 2, "Two source frames become alternating one/two-tick gaps");
                }
                totalVolleys++;
                totalShots += group.lanes;
            }
            if (i + 1 < ChaosBusterPattern.groupCount()) {
                check(lastTicks[i] < ChaosBusterPattern.group(i + 1).warningTick,
                        "A new warning cannot overwrite an unfinished burst");
            }
            double[] shotAngles = group.shotAnglesRadians();
            double[] warningAngles = group.warningAnglesRadians();
            check(shotAngles.length == group.lanes && warningAngles.length == group.lanes,
                    "Every firing lane has a warning");
            for (int lane = 0; lane < group.lanes; lane++) {
                near(shotAngles[lane], -shotAngles[group.lanes - lane - 1], 1.0E-9,
                        "The actual firing fan is symmetric");
                check(Math.abs(warningAngles[lane]) <= Math.abs(shotAngles[lane]),
                        "Warning rays use the narrower source angles");
                if (lane > 0) near(shotAngles[lane] - shotAngles[lane - 1], Math.toRadians(20), 1.0E-9,
                        "Firing lanes are twenty degrees apart");
            }
            double original = shotAngles[0];
            shotAngles[0] = 123;
            near(group.shotAnglesRadians()[0], original, 0, "Callers cannot mutate the shared firing pattern");
        }
        check(totalVolleys == 46, "The source sequence has 46 volleys");
        check(totalShots == 157, "The source sequence has 157 individual pellets");
        for (int tick = -1; tick <= ChaosBusterPattern.BEAM_START_TICK; tick++) {
            int active = 0, expected = -1;
            for (int i = 0; i < warningTicks.length; i++) {
                if (tick >= warningTicks[i] && tick < firstTicks[i]) { active++; expected = i; }
            }
            check(active <= 1, "At most one telegraph can be active");
            check(ChaosBusterPattern.telegraphGroup(tick) == expected, "Telegraph schedule has no gaps or stale rays");
        }
        check(lastTicks[8] == ChaosBusterPattern.CHARGE_START_TICK,
                "Rainbow filling deliberately overlaps the very last pellet volley");
        near(ChaosBusterPattern.CHARGE_START_TICK / 20.0, 113.0 / 15.0, 1.0 / 15.0,
                "Rainbow charge agrees with the GIF");
        near(ChaosBusterPattern.BEAM_START_TICK / 20.0, 138.0 / 15.0, 1.0 / 15.0,
                "Beam onset agrees with the GIF");
        check(ChaosBusterPattern.BEAM_LIFETIME == 26 + 40,
                "Both finishing beams last exactly two seconds longer after firing");
        check(ChaosBusterPattern.BEAM_SHRINK_TICKS == 16, "The original narrowing tail stays sixteen ticks");
        for (float age : new float[]{0, 10, 26, 40, 49.75F, 50})
            near(ChaosBusterPattern.beamRadiusScale(age, ChaosBusterPattern.BEAM_LIFETIME), 1, 0,
                    "The beam stays full width through the extra two seconds");
        near(ChaosBusterPattern.beamRadiusScale(58, ChaosBusterPattern.BEAM_LIFETIME), .5, 0,
                "The original narrowing tail now reaches half width at tick fifty-eight");
        check(ChaosBusterPattern.beamRadiusScale(65.75F, ChaosBusterPattern.BEAM_LIFETIME) > 0,
                "The extended beam remains visible until expiry");
        for (float age : new float[]{66, 100, -1, Float.NaN, Float.POSITIVE_INFINITY})
            near(ChaosBusterPattern.beamRadiusScale(age, ChaosBusterPattern.BEAM_LIFETIME), 0, 0,
                    "Expired and invalid beam ages have no radius");
        for (float age = 10; age < 26; age += .25F)
            near(ChaosBusterPattern.beamRadiusScale(age + 40, ChaosBusterPattern.BEAM_LIFETIME),
                    1 - (age - 10) / 16.0, 1.0E-9,
                    "The original narrowing tail is delayed by exactly forty ticks");
        check(ChaosBusterPattern.AIM_LOCK_TICK > 171
                        && ChaosBusterPattern.AIM_LOCK_TICK < ChaosBusterPattern.BEAM_START_TICK,
                "Aim locks after the twirl and before the beam");
        near(ChaosBusterPattern.BOLT_SPEED, 48.0 / (485.0 / (20.0 * 30.0)) / 20.0, 1.0E-9,
                "Pellets preserve the source travel time across the battlefield");
        near(ChaosBusterPattern.BEAM_RADIUS * 2.0 / 48.0, 70.0 / 485.0, 1.0E-9,
                "Beam damage width preserves its original proportion despite the wider visuals");
        near(ChaosBusterPattern.BEAM_RANGE, 96, 0,
                "Both finishing beams have double their original forty-eight-block range");
        near(ChaosBusterPattern.BEAM_RADIUS, 48.0 / 485.0 * 70.0 / 2.0, 1.0E-9,
                "Doubling range must not enlarge the damage cross section");
        for (double radius : new double[]{ChaosBusterPattern.BEAM_RADIUS, ChaosBusterPattern.BLASTER_BEAM_RADIUS}) {
            double range = ChaosBusterPattern.BEAM_RANGE, targetHalfWidth = .3;
            check(YagiAwakeningMath.insideBeam(0, 0, 80, 0, 0, 1, range, radius, targetHalfWidth),
                    "Both variants must hit targets in the added forty-eight-to-ninety-six-block section");
            check(YagiAwakeningMath.insideBeam(0, 0, 96, 0, 0, 1, range, radius, targetHalfWidth),
                    "The new endpoint is included in the damage length");
            check(!YagiAwakeningMath.insideBeam(0, 0, 96.01, 0, 0, 1, range, radius, targetHalfWidth),
                    "The beam must not hit beyond ninety-six blocks");
            check(YagiAwakeningMath.insideBeam(radius + .29, 0, 80, 0, 0, 1, range, radius, targetHalfWidth),
                    "Targets intersecting the original damage edge remain hittable at the longer range");
            check(!YagiAwakeningMath.insideBeam(radius + .31, 0, 80, 0, 0, 1, range, radius, targetHalfWidth),
                    "Wider beam visuals must not enlarge the horizontal damage cross section");
            check(!YagiAwakeningMath.insideBeam(0, radius + .31, 80, 0, 0, 1, range, radius, targetHalfWidth),
                    "Taller beam visuals must not enlarge the vertical damage cross section");
            check(YagiAwakeningMath.insideBeam(0, 80, 0, 0, 1, 0, range, radius, targetHalfWidth),
                    "The added damage range works when the beam is aimed vertically");
            check(!YagiAwakeningMath.insideBeam(0, 0, 80, 0, 0, 1, 48, radius, targetHalfWidth),
                    "An intervening wall must still shorten the damage length passed to the hit test");
        }

        near(ChaosBusterPattern.chargeFraction(150, false), 0, 0, "No rainbow charge during earlier bursts");
        near(ChaosBusterPattern.chargeFraction(151, false), 0, 0, "Normal fill starts at its boundary");
        near(ChaosBusterPattern.chargeFraction(153.33333F, false), 0.5, 0.00001,
                "Normal fill progresses over seven source frames");
        near(ChaosBusterPattern.chargeFraction(156, false), 1, 0, "Normal fill is complete before the twirl");
        near(ChaosBusterPattern.chargeFraction(0, true), 0, 0, "Enhanced fill starts immediately");
        near(ChaosBusterPattern.chargeFraction(10, true), 0.5, 0, "Enhanced fill lasts one second");
        near(ChaosBusterPattern.chargeFraction(20, true), 1, 0, "Enhanced fill completes after twenty ticks");
        near(ChaosBusterPattern.chargeFraction(1000, true), 1, 0, "Fill is clamped after completion");
        near(ChaosBusterPattern.chargeFraction(Float.NaN, false), 0, 0, "Invalid visual ages remain safe");
        near(ChaosBusterPattern.cannonSpinDegrees(160), 0, 0, "No premature spin");
        near(ChaosBusterPattern.cannonSpinDegrees(161), 0, 0, "Spin begins at the original pose");
        near(ChaosBusterPattern.cannonSpinDegrees(166), 180, 0, "Spin passes through half a turn");
        check(ChaosBusterPattern.cannonSpinDegrees(170.9F) > 359,
                "Spin completes a full turn rather than snapping back early");
        near(ChaosBusterPattern.cannonSpinDegrees(171), 0, 0, "A full turn restores the original pose");
        near(ChaosBusterPattern.cannonSpinDegrees(185), 0, 0, "Cannon stays steady while firing");
        System.out.println("Chaos Buster: " + checks + " checks passed; nine bursts, 46 volleys, 157 pellets.");
    }
}
