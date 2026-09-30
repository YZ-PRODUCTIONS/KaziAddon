package net.kazi.kazimod.entities;

public final class EnteiGeometry {
    private EnteiGeometry() {
    }

    public static float fireballRadius(float projectileSize) {
        return projectileSize * 0.25f;
    }

    public static double overheadOffset(float projectileSize) {
        return Math.max(7.5, (double)EnteiGeometry.fireballRadius(projectileSize) * 1.4 + 2.0) + 8.0;
    }
}
