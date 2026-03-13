package net.kazi.kazimod.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;

/**
 * Client-side only store for active after-image snapshots.
 * No entity, no server sync, no renderer registration needed.
 * Completely sidesteps the EntityRendererManager NPE.
 */
public class AfterImageStore {

    public static final AfterImageStore INSTANCE = new AfterImageStore();

    public static class Snapshot {
        public final double x, y, z;
        public final float  yaw, pitch;
        public final String skinName;
        public int          ticksRemaining;
        public final int    maxTicks;

        public Snapshot(double x, double y, double z,
                        float yaw, float pitch,
                        String skinName, int ticks) {
            this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
            this.skinName = skinName;
            this.ticksRemaining = ticks;
            this.maxTicks = ticks;
        }

        public float getAlpha() {
            return maxTicks > 0 ? (float) ticksRemaining / (float) maxTicks : 0.0F;
        }
    }

    private final List<Snapshot> snapshots = new ArrayList<>();

    private AfterImageStore() {}

    public void addSnapshot(Snapshot s) {
        snapshots.add(s);
    }

    /** Called every client tick to decrement lifetimes and cull dead snapshots. */
    public void tick() {
        Iterator<Snapshot> it = snapshots.iterator();
        while (it.hasNext()) {
            Snapshot s = it.next();
            s.ticksRemaining--;
            if (s.ticksRemaining <= 0) {
                it.remove();
            }
        }
    }

    public List<Snapshot> getSnapshots() {
        return snapshots;
    }

    public void clear() {
        snapshots.clear();
    }
}