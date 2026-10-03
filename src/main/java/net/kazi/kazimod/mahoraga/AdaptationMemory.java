package net.kazi.kazimod.mahoraga;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Server-side learning. A hit interrupts the wheel before resistance is committed. */
public final class AdaptationMemory {
    public static final int QUIET_TICKS = 200;
    public static final int TURN_TICKS = 20;
    public static final long ADAPTATION_TICKS = 30L * 60L * 20L;
    public final Map<String, Integer> attacks = new LinkedHashMap<>();
    public final Map<String, Integer> categories = new LinkedHashMap<>();
    public final Map<String, Long> attackExpires = new LinkedHashMap<>();
    public final Map<String, Long> categoryExpires = new LinkedHashMap<>();
    private final Set<String> pendingAttacks = new LinkedHashSet<>();
    private final Set<String> pendingCategories = new LinkedHashSet<>();
    private long quietUntil;
    private int turnProgress, turns;

    public void observe(String attack, String category, long now) {
        expire(now);
        quietUntil = now + QUIET_TICKS;
        turnProgress = 0;
        if (attack != null && !attack.isEmpty()) {
            if (attacks.getOrDefault(attack, 0) < 4) remember(pendingAttacks, attack, 128);
            if (category != null && !category.isEmpty() && categories.getOrDefault(category, 0) < 7)
                remember(pendingCategories, category, 32);
        }
    }

    public boolean tick(long now) {
        expire(now);
        if ((pendingAttacks.isEmpty() && pendingCategories.isEmpty()) || now < quietUntil) return false;
        if (++turnProgress < TURN_TICKS) return false;
        // Each distinct attack/category advances once per turn, regardless of hit count.
        for (String attack : pendingAttacks) increment(attacks, attackExpires, attack, 4, 128, now);
        for (String category : pendingCategories) increment(categories, categoryExpires, category, 7, 32, now);
        turns++;
        turnProgress = 0;
        quietUntil = now + QUIET_TICKS;
        pendingAttacks.removeIf(attack -> attacks.getOrDefault(attack, 0) >= 4);
        pendingCategories.removeIf(category -> categories.getOrDefault(category, 0) >= 7);
        return true;
    }

    private static void remember(Set<String> pending, String key, int limit) {
        pending.remove(key);
        if (pending.size() >= limit) pending.remove(pending.iterator().next());
        pending.add(key);
    }

    private static void increment(Map<String, Integer> map, Map<String, Long> expires, String key, int cap, int limit, long now) {
        if (!map.containsKey(key) && map.size() >= limit) {
            String evicted = map.keySet().iterator().next();
            map.remove(evicted);
            expires.remove(evicted);
        }
        expires.putIfAbsent(key, now + ADAPTATION_TICKS);
        map.put(key, Math.min(cap, map.getOrDefault(key, 0) + 1));
    }

    public void expire(long now) {
        expire(attacks, attackExpires, pendingAttacks, now);
        expire(categories, categoryExpires, pendingCategories, now);
    }

    private static void expire(Map<String, Integer> learned, Map<String, Long> expires, Set<String> pending, long now) {
        learned.keySet().removeIf(key -> {
            if (expiresAtOrBefore(expires.get(key), now)) {
                expires.remove(key);
                pending.remove(key);
                return true;
            }
            return false;
        });
    }

    private static boolean expiresAtOrBefore(Long expiry, long now) {
        return expiry != null && expiry <= now;
    }

    public float multiplier(String attack, String category) {
        if (attack == null || attack.isEmpty()) return 1;
        float exact = Math.min(1, attacks.getOrDefault(attack, 0) * .33F);
        float broad = category == null ? 0 : Math.min(1, categories.getOrDefault(category, 0) * .15F);
        // The same learned hit must not accidentally turn 33% into 48% resistance.
        return Math.max(0, 1 - Math.max(exact, broad));
    }

    public int progress() { return turnProgress; }
    public int turns() { return turns; }
    public void restoreTurns(int value) { turns = Math.max(0, value); }
}
