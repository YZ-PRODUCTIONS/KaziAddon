import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kazi.kazimod.abilities.YagiAwakening.StarBlazingPattern;

/** Checks individually aimed launch slots, reference cadence and bounded overlap without Minecraft. */
public final class StarBlazingPatternTest {
    private static int checks;
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        check(StarBlazingPattern.count() == 22, "Reference has 21 ordinary stars and one finisher");
        int previous = -1, finishers = 0, finalStart = -1, finalImpact = -1;
        int finalRegularLaunch = -1, maxFlying = 0, lastArrival = 0;
        List<Integer> regularImpacts = new ArrayList<>();
        for (int i = 0; i < StarBlazingPattern.count(); i++) {
            StarBlazingPattern.Launch launch = StarBlazingPattern.launch(i);
            check(launch.tick > previous, "Every star must have a separate ordered tick for its live-aim sample");
            check(launch.tick >= 0 && launch.tick + launch.travelTicks < StarBlazingPattern.duration(),
                    "Every star arrives before the cast ends");
            previous = launch.tick;
            lastArrival = Math.max(lastArrival, launch.tick + launch.travelTicks);
            if (launch.finisher) {
                finishers++;
                finalStart = launch.tick;
                finalImpact = launch.tick + launch.travelTicks;
                check(launch.travelTicks == 36, "The final star gets a visibly slower descent");
            } else {
                regularImpacts.add(launch.tick + launch.travelTicks);
                finalRegularLaunch = launch.tick;
                check(launch.travelTicks == 12, "Normal stars share a consistent flight time");
            }
        }
        Collections.sort(regularImpacts);
        check(finishers == 1, "Only one oversized finishing impact");
        check(StarBlazingPattern.duration() == lastArrival + 3, "The cast covers the actual final arrival plus cleanup time");
        check(finalStart < finalRegularLaunch, "The finisher must already be descending during the last normal launch");
        check(finalStart == 115 && finalImpact == 151, "The finisher keeps its original launch and slow descent");
        check(finalRegularLaunch == 117, "Independent aiming does not delay the end of the regular volley");
        int[] sourceImpactFrames = {36, 43, 48, 49, 55, 63, 64, 65, 69, 76, 77,
                83, 92, 92, 97, 100, 102, 111, 111, 115, 123};
        int staggeredStars = 0;
        for (int i = 0; i < sourceImpactFrames.length; i++) {
            int sourceImpact = Math.round((sourceImpactFrames[i] - 26) * 20.0F / 15.0F);
            int delay = regularImpacts.get(i) - sourceImpact;
            check(delay >= 0 && delay <= 1, "Individual aiming may delay a source beat by at most one tick");
            if (delay > 0) staggeredStars++;
        }
        check(staggeredStars == 2, "Only the second star of each simultaneous pair is delayed");
        check(regularImpacts.get(13) - regularImpacts.get(12) == 1
                && regularImpacts.get(18) - regularImpacts.get(17) == 1,
                "Both former double-impact beats become consecutive individually aimed stars");
        // Measured GIF impact seconds; the opening title sequence is outside the active volley.
        double[] seconds = {2.40, 2.87, 3.20, 3.27, 3.67, 4.20, 4.27, 4.33, 4.60,
                5.07, 5.13, 5.53, 6.13, 6.13, 6.47, 6.67, 6.80, 7.40, 7.40, 7.67, 8.20};
        for (int i = 0; i < seconds.length; i++) {
            check(Math.abs(regularImpacts.get(i) / 20.0 + 26.0 / 15.0 - seconds[i]) <= 0.09,
                    "Normal impact " + i + " drifted from the sampled GIF rhythm");
        }
        check(Math.abs(finalImpact / 20.0 + 26.0 / 15.0 - 9.27) <= 0.04, "Final impact matches the reference");
        for (int tick = 0; tick <= StarBlazingPattern.duration(); tick++) {
            int flying = 0, launches = 0;
            for (int i = 0; i < StarBlazingPattern.count(); i++) {
                StarBlazingPattern.Launch launch = StarBlazingPattern.launch(i);
                if (tick >= launch.tick && tick < launch.tick + launch.travelTicks) flying++;
                if (tick == launch.tick) launches++;
            }
            check(launches <= 1, "No tick dispatches more than one star");
            maxFlying = Math.max(maxFlying, flying);
        }
        check(maxFlying <= 5, "Main-star overlap stays bounded for lower-end clients");
        check(StarBlazingPattern.duration() < 160, "The active volley is bounded below eight seconds");
        System.out.println("Star Blazing: " + checks + " cadence checks passed; " + StarBlazingPattern.count()
                + " stars over " + StarBlazingPattern.duration() + " ticks, maximum " + maxFlying + " falling stars at once.");
    }
}
