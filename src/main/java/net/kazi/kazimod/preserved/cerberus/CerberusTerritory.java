package net.kazi.kazimod.preserved.cerberus;

/** Shared gameplay/render dimensions so the visible border matches the affected area. */
public final class CerberusTerritory {
    public static final double RADIUS = 50;
    public static final double HEIGHT = 4;
    public static boolean contains(double x, double y, double z) {
        return Math.abs(y) <= HEIGHT && x*x + z*z <= RADIUS*RADIUS;
    }
    private CerberusTerritory() {}
}
