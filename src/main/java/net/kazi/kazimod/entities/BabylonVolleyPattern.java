package net.kazi.kazimod.entities;

import java.util.Random;

/** Shared deterministic layout/timing for the server's shots and the client's treasury gates. */
public final class BabylonVolleyPattern {
    public static final int PORTALS = 100, MAX_PORTALS = 150, SHOTS = PORTALS;
    public static final double SHOT_INTERVAL = 1.5D;
    public static final double MIN_PORTAL_DISTANCE = 1.5D;
    public static final int GATE_OPEN_TICKS = 10, WEAPON_EMERGE_TICKS = 10, WEAPON_HOLD_TICKS = 10;
    public static final int PREPARATION_TICKS = GATE_OPEN_TICKS + WEAPON_EMERGE_TICKS + WEAPON_HOLD_TICKS;

    /** Cast-relative emergence, ending exactly half a second before each shot. */
    public static float weaponFormation(int shot, float castAge) {
        float emergeStart = PREPARATION_TICKS + shotTick(shot) - WEAPON_HOLD_TICKS - WEAPON_EMERGE_TICKS;
        return Math.max(0, Math.min(1, (castAge - emergeStart) / WEAPON_EMERGE_TICKS));
    }
    private BabylonVolleyPattern() { }
    /** One separated position per grid cell, reproducible from the synchronized cast seed. */
    public static double[][] layout(long seed) {
        return layout(seed, PORTALS);
    }
    public static int columns(int count) { return count == 75 ? 15 : count == 150 ? 15 : 20; }
    public static double[][] layout(long seed, int count) {
        int columns = columns(count);
        Random random = new Random(seed);
        double[][] positions = new double[count][3];
        for (int portal = 0; portal < count; portal++) {
            boolean placed = false;
            for (int attempt = 0; attempt < 256; attempt++) {
                double[] p = positions[portal];
                p[0] = -columns * 2 + (portal % columns + random.nextDouble()) * 4;
                p[1] = 2 + (portal / columns + random.nextDouble()) * 1.4;
                p[2] = -3 + random.nextDouble() * 2;
                if (separated(positions, portal)) { placed = true; break; }
            }
            if (!placed) {
                // Staggered rows guarantee separation even if random placement fails.
                for (int i = 0; i < count; i++) {
                    int row = i / columns;
                    positions[i][0] = -columns * 2 + 2 + (i % columns) * 4
                            + (row % 2) * .9 + (random.nextDouble() - .5) * .1;
                    positions[i][1] = 2.7 + row * 1.4 + (random.nextDouble() - .5) * .04;
                    positions[i][2] = -3 + random.nextDouble() * 2;
                }
                return positions;
            }
        }
        return positions;
    }

    private static boolean separated(double[][] positions, int index) {
        for (int i = 0; i < index; i++) {
            double x = positions[index][0] - positions[i][0];
            double y = positions[index][1] - positions[i][1];
            double z = positions[index][2] - positions[i][2];
            if (x * x + y * y + z * z < MIN_PORTAL_DISTANCE * MIN_PORTAL_DISTANCE) return false;
        }
        return true;
    }
    public static int portalForShot(int shot) { return Math.floorMod(shot * 7, PORTALS); }
    public static int portalForShot(int shot, int count) { return Math.floorMod(shot * 7, count); }
    public static int firstShotForPortal(int portal) { return firstShotForPortal(portal, PORTALS); }
    public static int firstShotForPortal(int portal, int count) {
        // Seven is coprime with all supported portal counts (75, 100, 150).
        for (int shot = 0; shot < count; shot++) if (portalForShot(shot, count) == portal) return shot;
        throw new IllegalArgumentException("Invalid portal index");
    }
    public static int nextShotForPortal(int portal, int fired) {
        int next = firstShotForPortal(portal);
        while (next < fired) next += PORTALS;
        return next;
    }
    public static int weaponForShot(int shot) { return Math.floorMod(shot, SHOTS); }
    public static int shotTick(int shot) { return (int)Math.floor(shot * SHOT_INTERVAL); }
}
