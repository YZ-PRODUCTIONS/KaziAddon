import net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterHealth;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;

/** Exercises the production health policy without loading Minecraft or client rendering classes. */
public final class IllusionShatterHealthTest {
    private static int checks;

    public static void main(String[] args) {
        boundaries(100, 30);
        boundaries(20, 6);
        boundaries(200, 60);
        boundaries(40, 12);
        invalidHealth();
        healthChanges();
        System.out.println("Illusion Shatter health policy: " + checks + " checks passed.");
    }

    private static void boundaries(float max, float lower) {
        check(!IllusionShatterHealth.active(Math.nextDown(lower), max), "melee/aura must stop immediately below 30%");
        check(!IllusionShatterHealth.immune(Math.nextDown(lower), max), "fruit immunity must stop immediately below 30%");
        check(IllusionShatterHealth.active(lower, max), "exactly 30% must keep melee/aura active");
        check(IllusionShatterHealth.immune(lower, max), "exactly 30% must keep fruit immunity active");
        check(IllusionShatterHealth.active(Math.nextUp(lower), max), "just above 30% must remain active");
        check(IllusionShatterHealth.immune(Math.nextUp(lower), max), "just above 30% must keep fruit immunity active");
        check(IllusionShatterHealth.immune(max * .5F, max), "50% must grant fruit immunity");
        check(IllusionShatterHealth.immune(Math.nextDown(max * .75F), max), "fruit immunity must remain active just below 75%");
        check(IllusionShatterHealth.immune(max * .75F, max), "the former 75% limit must now grant fruit immunity");
        check(IllusionShatterHealth.immune(Math.nextUp(max * .75F), max), "above 75% must grant fruit immunity");
        check(IllusionShatterHealth.active(max, max), "melee/aura must remain active at full health");
        check(IllusionShatterHealth.immune(max, max), "full health must grant fruit immunity");

        // At the handoff boundary, neither a floating-point gap nor an overlap may occur.
        for (float health : new float[]{Math.nextDown(lower), lower, Math.nextUp(lower)}) {
            check(InnerDragonsSequence.belowThreshold(health, max) != IllusionShatterHealth.active(health, max),
                    "Inner Dragons and Illusion Shatter must hand off at the same 30% boundary");
            check(InnerDragonsSequence.belowThreshold(health, max) != IllusionShatterHealth.immune(health, max),
                    "fruit immunity must hand off at the same exact 30% boundary");
        }
    }

    private static void invalidHealth() {
        for (float bad : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            check(!IllusionShatterHealth.active(bad, 100), "dead/invalid health must not activate the passive");
            check(!IllusionShatterHealth.immune(bad, 100), "dead/invalid health must not grant immunity");
            check(!IllusionShatterHealth.active(30, bad), "invalid maximum health must not activate the passive");
            check(!IllusionShatterHealth.immune(30, bad), "invalid maximum health must not grant immunity");
        }
    }

    private static void healthChanges() {
        for (int cycle = 0; cycle < 3; cycle++) {
            check(IllusionShatterHealth.active(30, 100), "recovery to 30% must reactivate immediately");
            check(IllusionShatterHealth.immune(30, 100), "recovery to exactly 30% must restore immunity immediately");
            check(IllusionShatterHealth.immune(100, 100), "healing to full must preserve immunity");
            check(!IllusionShatterHealth.active(29, 100), "a new health drop must disable immediately");
            check(!IllusionShatterHealth.immune(29, 100), "immunity must not persist below threshold");
        }
        check(IllusionShatterHealth.active(30, 100), "30 of 100 should be active");
        check(!IllusionShatterHealth.active(30, 200), "raising max health must update the lower cutoff");
        check(IllusionShatterHealth.immune(30, 100), "30 of 100 should grant fruit immunity");
        check(!IllusionShatterHealth.immune(30, 200), "raising max health must immediately disable immunity below 30%");
        check(IllusionShatterHealth.immune(30, 40), "lowering max health to put health at 75% must keep immunity");
        check(IllusionShatterHealth.immune(30, 30), "lowering max health to full health must keep immunity");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
