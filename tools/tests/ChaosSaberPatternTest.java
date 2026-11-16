import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberPattern;

/** Standalone source-timing and doubled-area contract checks, without a Minecraft client. */
public final class ChaosSaberPatternTest {
    private static int checks;

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private static void near(double actual, double expected, double tolerance, String reason) {
        check(Math.abs(actual - expected) <= tolerance, reason);
    }

    private static int sourceTick(int frame) { return Math.round(frame * 20.0F / 30.0F); }

    public static void main(String[] args) {
        // Original timer: 43-frame sword arrival, 14-frame pause, then five 27-frame cycles.
        check(ChaosSaberPattern.GRAB_TICK == sourceTick(43), "Swords settle at the original grab cue");
        check(ChaosSaberPattern.SINGLE_SLASHES == 5, "Five individual slashes precede the double finale");
        for (int i = 0; i < ChaosSaberPattern.SINGLE_SLASHES; i++) {
            int sourceWindup = 43 + 14 + i * 27;
            int sourceSlash = sourceWindup + 17;
            int windup = ChaosSaberPattern.windupTick(i);
            int slash = ChaosSaberPattern.slashTick(i);
            check(windup == sourceTick(sourceWindup), "Windup " + i + " matches the source alarm");
            check(slash == sourceTick(sourceSlash), "Slash " + i + " matches timer124");
            check(ChaosSaberPattern.hitTick(i) == sourceTick(sourceSlash + 1), "Damage begins at timer125");
            check(ChaosSaberPattern.side(i) == (i % 2 == 0 ? -1 : 1), "The swords alternate sides");
            if (i > 0) check(slash - ChaosSaberPattern.slashTick(i - 1) == 18,
                    "Each 27-frame source interval becomes exactly 18 ticks");
            near(ChaosSaberPattern.pullbackFraction(windup - 1, i), 0, 0, "No early pullback");
            near(ChaosSaberPattern.pullbackFraction(windup + 3, i), 0.5, 0, "Pullback halfway point");
            near(ChaosSaberPattern.pullbackFraction(slash - 1, i), 1, 0, "Raised sword holds before cutting");
            near(ChaosSaberPattern.pullbackFraction(slash, i), 0, 0, "Pullback ends when cutting starts");
            near(ChaosSaberPattern.swingFraction(slash, i), 0, 0, "Swing starts at its boundary");
            near(ChaosSaberPattern.swingFraction(slash + 1, i), 0.5, 0, "Sword deploys quickly");
            near(ChaosSaberPattern.swingFraction(slash + 2, i), 1, 0, "Swing completes after two ticks");
            near(ChaosSaberPattern.smearAlpha(slash - 0.01F, i), 0, 0, "Smear is absent before the strike");
            near(ChaosSaberPattern.smearAlpha(slash, i), 1, 0, "Strike begins with a solid white smear");
            near(ChaosSaberPattern.smearAlpha(slash + 2, i), 0.5, 0, "Smear becomes translucent");
            near(ChaosSaberPattern.smearAlpha(slash + 4, i), 0, 0, "Smear disappears before recovery finishes");
        }

        check(ChaosSaberPattern.FINAL_CHARGE_TICK == sourceTick(192), "Final charge follows the fifth cycle");
        check(ChaosSaberPattern.FINAL_SLASH_TICK == sourceTick(226), "Both swords cut at the original finale time");
        check(ChaosSaberPattern.FINAL_HIT_TICK == ChaosSaberPattern.SPARKLE_TICK,
                "Blade damage and shattering share the next-tick impact phase");
        check(ChaosSaberPattern.SPARKLE_TICK > ChaosSaberPattern.FINAL_SLASH_TICK,
                "Quantization preserves a visible slash before shattering");
        check(ChaosSaberPattern.SPARKLE_TICK - sourceTick(227) <= 1,
                "Shatter timing differs by at most one tick from the source");
        check(ChaosSaberPattern.SINGLE_SLASHES + 2 == 7, "Finale contributes two blade strikes");
        check(ChaosSaberPattern.sparkDamageTick(0) > ChaosSaberPattern.SWORD_FADE_END_TICK,
                "Spark damage follows the vanishing swords");
        check(ChaosSaberPattern.sparkDamageTick(1) - ChaosSaberPattern.sparkDamageTick(0) >= 10,
                "Separate spark pulses survive the ordinary hurt-immunity interval");
        check(ChaosSaberPattern.sparkDamageTick(1) < ChaosSaberPattern.SPARKLE_FADE_TICK,
                "Spark damage stops before the visual fade");

        near(ChaosSaberPattern.LENGTH, 88.0, 0, "Saber reach is 88 blocks");
        near(ChaosSaberPattern.SLASH_RADIUS, 7.2, 0, "Slash radius is 7.2 blocks");
        near(ChaosSaberPattern.SPARK_RADIUS, 20.0, 0, "Spark radius is 20 blocks");
        near(ChaosSaberPattern.arrivalFraction(0), 0, 0, "Arrival begins away from the hands");
        near(ChaosSaberPattern.arrivalFraction(14.5F), 0.5, 0, "Arrival has an even midpoint");
        near(ChaosSaberPattern.arrivalFraction(29), 1, 0, "Both swords are held at the grab cue");
        near(ChaosSaberPattern.finalChargeFraction(128), 0, 0, "Final charge begins at its cue");
        near(ChaosSaberPattern.finalChargeFraction(151), 1, 0, "Final charge finishes with the slash");
        near(ChaosSaberPattern.finalSmearAlpha(151), 1, 0, "Final double slash starts bright");
        near(ChaosSaberPattern.finalSmearAlpha(155), 0, 0, "Final double smear has a finite tail");
        near(ChaosSaberPattern.swordAlpha(152), 1, 0, "Swords remain visible at the shatter transition");
        near(ChaosSaberPattern.swordAlpha(155.5F), 0.5, 0, "Sword fragments fade evenly");
        near(ChaosSaberPattern.swordAlpha(159), 0, 0, "Sword models are gone before spark pulses");
        near(ChaosSaberPattern.sparkleAlpha(151), 0, 0, "No premature sparkles");
        near(ChaosSaberPattern.sparkleAlpha(152), 1, 0, "Sparkles appear with shattering");
        near(ChaosSaberPattern.sparkleAlpha(179), 1, 0, "Sparkles hold through their initial lifetime");
        near(ChaosSaberPattern.sparkleAlpha(185.5F), 0.5, 0, "Sparkles fade smoothly");
        near(ChaosSaberPattern.sparkleAlpha(192), 0, 0, "All effects end at the declared duration");
        for (float age = -2; age <= ChaosSaberPattern.DURATION + 2; age += 0.25F) {
            float[] values = {ChaosSaberPattern.arrivalFraction(age), ChaosSaberPattern.swordAlpha(age),
                    ChaosSaberPattern.sparkleAlpha(age), ChaosSaberPattern.finalChargeFraction(age),
                    ChaosSaberPattern.finalSmearAlpha(age)};
            for (float value : values) check(Float.isFinite(value) && value >= 0 && value <= 1,
                    "Visual fractions stay finite and bounded throughout the sequence");
        }
        near(ChaosSaberPattern.swordAlpha(Float.NaN), 0, 0, "Invalid age cannot create an opaque sword");
        near(ChaosSaberPattern.sparkleAlpha(Float.POSITIVE_INFINITY), 0, 0, "Invalid age cannot persist sparkles");
        System.out.println("Chaos Saber: " + checks + " checks passed; five singles, double finale, doubled area.");
    }
}
