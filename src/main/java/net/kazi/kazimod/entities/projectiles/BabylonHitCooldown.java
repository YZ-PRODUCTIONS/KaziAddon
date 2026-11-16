package net.kazi.kazimod.entities.projectiles;

import java.util.Map;
import java.util.WeakHashMap;

/** Server-side hit history shared by all treasury swords, keyed by the enemy they damaged. */
final class BabylonHitCooldown<T> {
    static final int TICKS = 10;
    private final Map<T, Hit> hits = new WeakHashMap<>();

    boolean blocks(T target, int swordId, long now) {
        Hit last = hits.get(target);
        return last != null && last.swordId != swordId && now < last.nextHitTick;
    }

    void record(T target, int swordId, long now) {
        hits.put(target, new Hit(swordId, now + TICKS));
    }

    private static final class Hit {
        final int swordId;
        final long nextHitTick;
        Hit(int swordId, long nextHitTick) {
            this.swordId = swordId;
            this.nextHitTick = nextHitTick;
        }
    }
}
