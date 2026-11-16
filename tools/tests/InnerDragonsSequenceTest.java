import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.kazi.kazimod.client.nagi.InnerDragonsEmergenceMesh;

/** Executes the production cinematic timing; does not simulate a running Minecraft client. */
public final class InnerDragonsSequenceTest {
    private static int checks;

    public static void main(String[] args) {
        timing();
        emergence();
        invalidAges();
        healthThreshold();
        lifecycle();
        transformedArmLifecycle();
        independentUsers();
        if (args.length == 0 || !"lifecycle-only".equals(args[0])) geometry();
        System.out.println("Inner Dragons cinematic: " + checks + " timing and health-lifecycle checks passed.");
    }

    private static void timing() {
        check(InnerDragonsSequence.DURATION_TICKS == 160, "scene must finish after eight seconds");
        check(InnerDragonsSequence.ARM_BREAK_TICK < InnerDragonsSequence.EMERGENCE_TICK,
                "the arm must break before the first dragon emerges");
        check(InnerDragonsSequence.STAGGER_TICKS > 0, "dragons must not all emerge at once");
        check(InnerDragonsSequence.EMERGENCE_DURATION >= 20, "each emergence must be a slow reveal");
        check(InnerDragonsSequence.EMERGENCE_TICK + 7 * InnerDragonsSequence.STAGGER_TICKS
                        + InnerDragonsSequence.EMERGENCE_DURATION <= InnerDragonsSequence.DURATION_TICKS,
                "all eight dragons must finish appearing before the scene ends");
        check(!InnerDragonsSequence.active(-1), "negative ages must not activate a scene");
        check(InnerDragonsSequence.active(0), "scene must start at age zero");
        check(InnerDragonsSequence.active(159.99F), "scene must survive its last partial tick");
        check(!InnerDragonsSequence.active(160), "scene must release the camera at its deadline");
        check(!InnerDragonsSequence.active(100000), "expired scenes must not reactivate");
    }

    private static void emergence() {
        for (int kind = 0; kind < 8; kind++) {
            float start = InnerDragonsSequence.EMERGENCE_TICK + kind * InnerDragonsSequence.STAGGER_TICKS;
            float end = start + InnerDragonsSequence.EMERGENCE_DURATION;
            check(InnerDragonsSequence.emergence(kind, 0) == 0, "dragon visible before the rupture");
            check(InnerDragonsSequence.emergence(kind, start) == 0, "dragon visible before its stagger");
            check(InnerDragonsSequence.emergence(kind, start + .5F) > 0,
                    "dragon must begin appearing after its stagger");
            check(InnerDragonsSequence.emergence(kind, end - .5F) < 1,
                    "dragon must not snap to full size early");
            check(InnerDragonsSequence.emergence(kind, end) == 1,
                    "dragon must be fully revealed when its emergence ends");
            float previous = 0;
            for (float age = 0; age < InnerDragonsSequence.DURATION_TICKS; age += .25F) {
                float progress = InnerDragonsSequence.emergence(kind, age);
                check(Float.isFinite(progress) && progress >= 0 && progress <= 1,
                        "emergence progress escaped its finite unit range");
                check(progress >= previous, "dragon reveal must not shrink or restart mid-scene");
                if (kind > 0) {
                    check(progress <= InnerDragonsSequence.emergence(kind - 1, age),
                            "later dragons must never overtake an earlier reveal");
                }
                previous = progress;
            }
        }
    }

    private static void invalidAges() {
        for (float age : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            check(!InnerDragonsSequence.active(age), "non-finite ages must never capture the camera");
            for (int kind = 0; kind < 8; kind++) {
                check(Float.isFinite(InnerDragonsSequence.emergence(kind, age)),
                        "invalid network age must not create non-finite dragon geometry");
            }
        }
        check(InnerDragonsSequence.emergence(-1, 100) == 0, "negative dragon ID must not render");
        check(InnerDragonsSequence.emergence(8, 100) == 0, "unknown dragon ID must not render");
    }

    private static void healthThreshold() {
        check(!InnerDragonsSequence.belowThreshold(30, 100), "exactly thirty percent must remain inactive");
        check(InnerDragonsSequence.belowThreshold(29.999, 100), "health below thirty percent must activate");
        check(!InnerDragonsSequence.belowThreshold(31, 100), "health above thirty percent must remain inactive");
        check(!InnerDragonsSequence.belowThreshold(0, 100), "dead users must not awaken dragons");
        check(!InnerDragonsSequence.belowThreshold(-1, 100), "invalid negative health must not trigger");
        check(!InnerDragonsSequence.belowThreshold(1, 0), "invalid maximum health must not trigger");
        check(!InnerDragonsSequence.belowThreshold(1, -100), "negative maximum health must not trigger");
        check(InnerDragonsSequence.belowThreshold(5, 20), "threshold must scale with maximum health");
        check(!InnerDragonsSequence.belowThreshold(5, 10), "same current health above threshold must stay idle");
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            check(!InnerDragonsSequence.belowThreshold(invalid, 100), "non-finite health must not trigger");
            check(!InnerDragonsSequence.belowThreshold(1, invalid), "non-finite max health must not trigger");
        }
    }

    private static void lifecycle() {
        InnerDragonsSequence scene = new InnerDragonsSequence();
        check(!scene.isPlaying(0), "a new passive must not capture the camera");
        check(!scene.update(100, 100, 1000, true), "healthy user must not start a scene");
        check(scene.update(29, 100, 1001, true), "first low-health tick must start exactly once");
        check(scene.startTick() == 1001 && scene.isPlaying(1001), "start time must be synchronized");
        check(!scene.isPlaying(1000), "a scene must not play before its start");
        for (int tick = 1002; tick < 1161; tick++) {
            check(!scene.update(20, 100, tick, true), "staying low must not restart the scene");
            check(scene.isPlaying(tick), "scene was cut short before its deadline");
        }
        check(!scene.isPlaying(1161), "scene must end exactly eight seconds after activation");
        for (int tick = 1161; tick < 1261; tick++) {
            check(!scene.update(10, 100, tick, true), "completed scene must not replay while health stays low");
        }

        scene.stop();
        check(!scene.update(10, 100, 1300, true), "cleanup must preserve the low-health latch");
        check(!scene.update(30, 100, 1301, true), "healing to threshold must only rearm");
        check(scene.update(29, 100, 1302, true), "a new downward crossing must trigger again");
        check(!scene.update(29, 100, 1303, false) && !scene.isPlaying(1303),
                "suppression must immediately stop the current scene");
        check(!scene.update(29, 100, 1304, true), "ending suppression must not spam another scene");
        check(!scene.update(30, 100, 1305, false), "healing while suppressed must rearm without starting");
        check(!scene.update(29, 100, 1306, false), "low health alone must not bypass suppression");
        check(scene.update(29, 100, 1307, true), "first eligible low-health tick must start after suppression");
        check(!scene.update(0, 100, 1308, true) && !scene.isPlaying(1308), "death must stop a scene");
        check(!scene.update(100, 100, 1309, true), "respawning healthy must not start a scene");
        check(scene.update(1, 100, 1310, true), "new life must have an independent threshold crossing");
        check(!scene.update(30, 100, 1311, true) && scene.isPlaying(1311), "healing must preserve a running scene");
        check(!scene.update(29, 100, 1312, true) && scene.startTick() == 1310, "health oscillation must not restart a running scene");
        check(!scene.update(29, 100, 1470, true), "a low-health crossing during a scene must not queue a replay");
        for (InnerDragonsSequence other : new InnerDragonsSequence[]{new InnerDragonsSequence(.5), new InnerDragonsSequence(.5, true, 260)}) {
            check(other.update(40, 100, 2000, true), "half-health cinematic starts");
            check(!other.update(100, 100, 2001, true) && other.isPlaying(2001), "full healing preserves cinematic");
            check(!other.update(100, 100, 2002, false) && !other.isPlaying(2002), "suppression still cancels healed cinematic");
        }
    }

    private static void transformedArmLifecycle() {
        InnerDragonsSequence scene = new InnerDragonsSequence();
        long start = 2000;
        boolean eligible = InnerDragonsSequence.belowThreshold(29, 100);
        check(scene.update(29, 100, start, eligible), "arm transformation must use the actual scene timeline");
        for (int offset = 0; offset < InnerDragonsSequence.ARM_BREAK_TICK; offset++) {
            check(!scene.shouldTransformArm(start + offset, eligible),
                    "right arm must remain intact before the rupture");
        }
        check(scene.shouldTransformArm(start + InnerDragonsSequence.ARM_BREAK_TICK, eligible),
                "right arm must transform at the rupture tick");
        check(scene.shouldTransformArm(start + InnerDragonsSequence.DURATION_TICKS - 1, eligible),
                "dragon head must remain throughout the emergence");
        check(scene.shouldTransformArm(start + InnerDragonsSequence.DURATION_TICKS, eligible),
                "ending the cinematic must not restore the right arm");
        check(scene.shouldTransformArm(start + 10000, eligible),
                "dragon head must persist while Inner Dragons stays active");

        eligible = InnerDragonsSequence.belowThreshold(30, 100);
        scene.update(30, 100, start + 10001, eligible);
        check(!scene.shouldTransformArm(start + 10001, eligible),
                "healing to exactly thirty percent must restore the right arm");
        scene.update(29, 100, start + 10002, true);
        check(!scene.shouldTransformArm(start + 10002, true),
                "a new cinematic must preserve its intact-arm opening");
        scene.update(29, 100, start + 10003, false);
        check(!scene.shouldTransformArm(start + 10003, false),
                "suppression must restore the ordinary arm immediately");
        check(!scene.update(29, 100, start + 10004, true),
                "resuming below threshold must preserve the consumed scene latch");
        check(scene.shouldTransformArm(start + 10004, true),
                "resuming Inner Dragons without another scene must restore the dragon head immediately");

        scene.update(30, 100, start + 10005, false);
        check(scene.update(29, 100, start + 10006, true),
                "cooldown-skipped activation must still consume its health edge");
        scene.stop();
        check(scene.shouldTransformArm(start + 10006, true),
                "skipping the cinematic on cooldown must transform the arm immediately");
        check(!scene.shouldTransformArm(start + 10007, false),
                "removing eligibility must restore an arm after a skipped cinematic too");
    }

    private static void independentUsers() {
        InnerDragonsSequence first = new InnerDragonsSequence();
        InnerDragonsSequence second = new InnerDragonsSequence();
        check(first.update(20, 100, 100, true), "first user's scene failed");
        check(second.update(20, 100, 120, true), "second user's scene failed");
        first.stop();
        check(!first.isPlaying(130) && second.isPlaying(130), "canceling one user must not affect another");
        check(second.startTick() == 120, "users must not share a cinematic clock");
    }

    private static void geometry() {
        int[] peak = new int[3];
        boolean mediumReduced = false, lowReduced = false;
        for (float age = 0; age < InnerDragonsSequence.DURATION_TICKS; age += .25F) {
            int previous = Integer.MAX_VALUE;
            for (int detail = 0; detail < 3; detail++) {
                Probe solid = new Probe(), glow = new Probe();
                InnerDragonsEmergenceMesh.render(solid, glow, age, detail);
                check(solid.valid && glow.valid, "mesh emitted non-finite/unbounded coordinates, color or opacity");
                check(solid.count % 4 == 0 && glow.count % 4 == 0, "mesh must retain complete material-local quads");
                int total = solid.count + glow.count;
                check(total <= previous, "reducing visual detail must never emit more vertices");
                if (detail == 1 && total < previous) mediumReduced = true;
                if (detail == 2 && total < previous) lowReduced = true;
                previous = total;
                peak[detail] = Math.max(peak[detail], total);
                check(total <= 8000, "cinematic geometry exceeded its per-frame budget");
                Probe repeatSolid = new Probe(), repeatGlow = new Probe();
                InnerDragonsEmergenceMesh.render(repeatSolid, repeatGlow, age, detail);
                check(solid.hash == repeatSolid.hash && glow.hash == repeatGlow.hash,
                        "identical timeline inputs must produce stable geometry on every viewer");
            }
            for (int kind = 0; kind < 8; kind++) {
                double[] position = InnerDragonsEmergenceMesh.dragonPosition(kind,
                        InnerDragonsSequence.emergence(kind, age), age);
                check(position.length == 3, "dragon emergence requires a three-dimensional position");
                for (double axis : position) check(Double.isFinite(axis) && Math.abs(axis) < 8,
                        "dragon model escaped its bounded cinematic fan");
            }
        }
        check(mediumReduced && lowReduced, "each reduced detail setting must save vertices");
        for (float age : new float[]{-1, 160, 1000, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            Probe solid = new Probe(), glow = new Probe();
            InnerDragonsEmergenceMesh.render(solid, glow, age, 0);
            check(solid.count == 0 && glow.count == 0, "inactive scenes must not leave geometry behind");
        }
        for (int kind = 0; kind < 8; kind++) {
            double[] start = InnerDragonsEmergenceMesh.dragonPosition(kind, 0, 0);
            check(start[0] == 0 && start[1] == 0 && start[2] == 0,
                    "every dragon must emerge from the same shoulder origin");
        }
        System.out.println("Cinematic geometry peak vertices (high/medium/low): "
                + peak[0] + "/" + peak[1] + "/" + peak[2]);
    }

    private static final class Probe implements InnerDragonsEmergenceMesh.Sink {
        int count;
        boolean valid = true;
        long hash = 1;

        @Override public void vertex(double x, double y, double z, int color, float alpha) {
            count++;
            valid &= Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                    && Math.abs(x) < 8 && Math.abs(y) < 8 && Math.abs(z) < 8
                    && Float.isFinite(alpha) && alpha >= 0 && alpha <= 1
                    && (color & 0xFF000000) == 0;
            hash = hash * 31 + Double.doubleToLongBits(x);
            hash = hash * 31 + Double.doubleToLongBits(y);
            hash = hash * 31 + Double.doubleToLongBits(z);
            hash = hash * 31 + color;
            hash = hash * 31 + Float.floatToIntBits(alpha);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
}
