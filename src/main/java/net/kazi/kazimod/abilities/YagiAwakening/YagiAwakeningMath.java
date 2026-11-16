package net.kazi.kazimod.abilities.YagiAwakening;

/** Side-independent health and beam geometry rules, also exercised without a game client. */
public final class YagiAwakeningMath {
    public static final float LOW_HEALTH_VISUAL_BRIGHTNESS = .6F;
    private YagiAwakeningMath() {}

    public static boolean enhanced(float health, float maximum) {
        return Float.isFinite(health) && Float.isFinite(maximum) && maximum > 0
                && health >= 0 && health < maximum * 0.5F;
    }

    public static boolean usesChaosBeam(float health, float maximum) {
        return Float.isFinite(health) && Float.isFinite(maximum) && maximum > 0
                && health > 0 && (double) health / maximum <= 0.10D;
    }

    /** Direction must be normalized; the target center is relative to the beam origin. */
    public static boolean insideBeam(double x, double y, double z, double dx, double dy, double dz,
                                     double length, double radius, double targetHalfWidth) {
        if (!Double.isFinite(x + y + z + dx + dy + dz + length + radius + targetHalfWidth)
                || length < 0 || radius < 0 || targetHalfWidth < 0) return false;
        double along = x * dx + y * dy + z * dz;
        if (along < 0 || along > length) return false;
        double px = x - dx * along, py = y - dy * along, pz = z - dz * along;
        double reach = radius + targetHalfWidth;
        return px * px + py * py + pz * pz <= reach * reach;
    }
}
