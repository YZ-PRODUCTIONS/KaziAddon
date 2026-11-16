import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerExSequence;

/** Exercises cast timing, target selection budgets and shared damage across overlapping pillars. */
public final class ShockerBreakerExSequenceTest {
    private static int checks;
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3), D = new UUID(0, 4);

    public static void main(String[] args) {
        fullCast();
        partialCastAndTargetChanges();
        skippedTicksAndDuplicatePolls();
        cancelledAndIndependentCasts();
        System.out.println("Shocker Breaker EX sequence: " + checks + " timing, marker and cancellation checks passed.");
    }

    private static void fullCast() {
        ShockerBreakerExSequence cast = new ShockerBreakerExSequence();
        List<Integer> rounds = new ArrayList<>();
        int total = 0;
        check(!cast.canMark(A), "No marker can be recorded before the first round");
        check(!cast.pollRound(-1), "Negative cast age cannot start a round");
        for (int tick = 0; tick <= 200; tick++) {
            if (cast.pollRound(tick)) {
                rounds.add(tick);
                for (UUID target : new UUID[]{A, B, C}) {
                    check(cast.canMark(target), "Each of the three enemies can receive its next warning");
                    check(cast.recordMark(target), "A spawned warning consumes exactly one marker slot");
                    total++;
                    check(!cast.recordMark(target), "The same enemy cannot receive two warnings in one round");
                }
                check(!cast.canMark(D), "A fourth enemy cannot extend the three-enemy roster");
            }
            check(!cast.pollRound(tick), "Repeating the tick cannot emit a duplicate round");
        }
        check(rounds.equals(Arrays.asList(0, 10, 20, 30, 40)), "Five rounds must be exactly half a second apart");
        check(total == 15, "A full cast spawns exactly fifteen warnings/pillars");
        check(cast.isComplete() && !cast.hasRoundPending(), "Sequence ends after its fifth marking round");
        check(!cast.canMark(A) && !cast.canMark(B) && !cast.canMark(C), "Five marks per enemy is a hard cap");

    }

    private static void partialCastAndTargetChanges() {
        ShockerBreakerExSequence single = new ShockerBreakerExSequence();
        int marks = 0;
        for (int round = 0; round < 5; round++) {
            check(single.pollRound(round * 10), "Single-target cast keeps all five rounds");
            if (single.recordMark(A)) marks++;
        }
        check(marks == 5, "A lone enemy receives five pillars, not fifteen");

        ShockerBreakerExSequence changing = new ShockerBreakerExSequence();
        check(changing.pollRound(0) && changing.recordMark(A), "The first available target occupies one slot");
        check(changing.canMark(B), "Checking a possible target must not consume its slot");
        check(changing.canMark(C) && changing.canMark(D), "Failed spawns must leave roster capacity unchanged");
        check(changing.pollRound(10) && changing.recordMark(B) && changing.recordMark(C), "Two later enemies can fill the remaining slots");
        check(!changing.canMark(D), "Switching nearby enemies cannot exceed three distinct targets per cast");
        check(changing.pollRound(20) && changing.recordMark(A), "An original enemy can be marked again at its new position");
        check(!changing.recordMark(null), "Missing targets cannot consume marker slots");
    }

    private static void skippedTicksAndDuplicatePolls() {
        ShockerBreakerExSequence cast = new ShockerBreakerExSequence();
        check(cast.pollRound(0), "First round is immediate");
        check(cast.pollRound(65), "A late update emits one pending round");
        for (long tick = 65; tick < 75; tick++) check(!cast.pollRound(tick), "Late updates cannot cause catch-up bursts");
        check(cast.pollRound(75), "After a delayed round the next still waits ten ticks");
        check(!cast.pollRound(70), "A backwards clock cannot repeat a round");
        check(cast.pollRound(85) && cast.pollRound(95), "Remaining delayed rounds complete normally");
        check(!cast.pollRound(Long.MAX_VALUE), "Huge ages cannot produce a sixth round");

        ShockerBreakerExSequence overflow = new ShockerBreakerExSequence();
        check(overflow.pollRound(Long.MAX_VALUE - 5), "Very late first polling is bounded to one round");
        check(!overflow.pollRound(Long.MAX_VALUE), "Extreme time values cannot shorten the ten-tick interval");
        check(!overflow.pollRound(Long.MAX_VALUE), "Extreme time values cannot duplicate a round");
    }

    private static void cancelledAndIndependentCasts() {
        ShockerBreakerExSequence first = new ShockerBreakerExSequence(), second = new ShockerBreakerExSequence();
        check(first.pollRound(0) && first.recordMark(A), "First cast can begin marking");
        first.cancel();
        first.cancel();
        check(first.isCancelled() && first.isComplete() && !first.hasRoundPending(), "Cancellation is idempotent and ends scheduling");
        check(!first.pollRound(10) && !first.canMark(B) && !first.recordMark(B), "Cancelled casts cannot create additional warnings");
        check(second.pollRound(0) && second.recordMark(A), "Cancellation does not affect another caster or cast");
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
}
