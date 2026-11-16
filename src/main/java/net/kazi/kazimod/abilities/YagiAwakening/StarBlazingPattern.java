package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.Arrays;
import java.util.Comparator;

/** Reference cadence rounded to 20 ticks/second, with simultaneous stars separated for individual aiming. */
public final class StarBlazingPattern {
    private static final int FIRST_VISIBLE_FRAME = 26;
    private static final int[] IMPACT_FRAMES = {
            36, 43, 48, 49, 55, 63, 64, 65, 69, 76, 77,
            83, 92, 92, 97, 100, 102, 111, 111, 115, 123
    };
    private static final int FINAL_IMPACT_FRAME = 139;
    private static final Launch[] LAUNCHES = createLaunches();

    private StarBlazingPattern() {}

    private static Launch[] createLaunches() {
        Launch[] result = new Launch[IMPACT_FRAMES.length + 1];
        for (int i = 0; i < IMPACT_FRAMES.length; i++) {
            result[i] = new Launch(impactTick(IMPACT_FRAMES[i]) - 12, 12, false);
        }
        result[IMPACT_FRAMES.length] = new Launch(impactTick(FINAL_IMPACT_FRAME) - 36, 36, true);
        Arrays.sort(result, Comparator.comparingInt(launch -> launch.tick));
        int previousTick = -1;
        for (int i = 0; i < result.length; i++) {
            Launch launch = result[i];
            // Keep the source order while giving each star its own live-aim sample.
            int tick = Math.max(launch.tick, previousTick + 1);
            result[i] = new Launch(tick, launch.travelTicks, launch.finisher);
            previousTick = tick;
        }
        return result;
    }

    private static int impactTick(int frame) { return Math.round((frame - FIRST_VISIBLE_FRAME) * 20.0F / 15.0F); }
    public static int count() { return LAUNCHES.length; }
    public static Launch launch(int index) { return LAUNCHES[index]; }
    public static int duration() {
        int lastArrival = 0;
        for (Launch launch : LAUNCHES) lastArrival = Math.max(lastArrival, launch.tick + launch.travelTicks);
        return lastArrival + 3;
    }

    public static final class Launch {
        public final int tick;
        public final int travelTicks;
        public final boolean finisher;

        private Launch(int tick, int travelTicks, boolean finisher) {
            this.tick = Math.max(0, tick);
            this.travelTicks = travelTicks;
            this.finisher = finisher;
        }
    }
}
