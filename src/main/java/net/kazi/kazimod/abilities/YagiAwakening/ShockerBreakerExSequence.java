package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Shared five-round clock and cancellation, with an EX-only target roster. */
public final class ShockerBreakerExSequence {
    public static final int MAX_TARGETS = 3;
    public static final int MARK_ROUNDS = 5;
    public static final int MARK_INTERVAL_TICKS = 10;

    private final Map<UUID, Integer> marks = new HashMap<>();
    private final Set<UUID> markedThisRound = new HashSet<>();
    private int rounds;
    private long lastRoundTick = -1;
    private boolean cancelled;

    /**
     * Emits at ages 0, 10, 20, 30 and 40 when ticked normally. Late polling never batches
     * several rounds together: the next warning remains at least half a second later.
     */
    public boolean pollRound(long elapsedTicks) {
        if (!hasRoundPending() || elapsedTicks < 0 || rounds > 0
                && (elapsedTicks < lastRoundTick || elapsedTicks - lastRoundTick < MARK_INTERVAL_TICKS)) return false;
        rounds++;
        lastRoundTick = elapsedTicks;
        markedThisRound.clear();
        return true;
    }

    public boolean hasRoundPending() { return !cancelled && rounds < MARK_ROUNDS; }
    public boolean isComplete() { return !hasRoundPending(); }
    public boolean isCancelled() { return cancelled; }

    /** Existing targets retain their slots; a cast never spreads beyond three distinct enemies. */
    public boolean canMark(UUID target) {
        if (cancelled || rounds == 0 || target == null || markedThisRound.contains(target)) return false;
        Integer count = marks.get(target);
        return count == null ? marks.size() < MAX_TARGETS : count < MARK_ROUNDS;
    }

    /** Call only after the corresponding warning entity has actually been spawned. */
    public boolean recordMark(UUID target) {
        if (!canMark(target)) return false;
        marks.put(target, marks.getOrDefault(target, 0) + 1);
        markedThisRound.add(target);
        return true;
    }

    public void cancel() { cancelled = true; }
}
