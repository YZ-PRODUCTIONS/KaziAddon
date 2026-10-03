package net.kazi.kazimod.renderers.entities;

/** Cheap camera fades for Ea's primitive batches; independent of Minecraft state. */
public final class EaVfxVisibility {
    private EaVfxVisibility() { }

    public static float proximity(double dx, double dy, double dz) {
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        // Conservative thresholds keep the original float behavior at fade boundaries.
        if (distanceSquared >= 6.01) return 1;
        if (distanceSquared <= 0.2024) return 0;
        return smooth(((float) Math.sqrt(distanceSquared) - 0.45F) / 2.0F);
    }

    public static float centerOpacity(double dx, double dy, double dz, float axisFade) {
        if (axisFade <= 0) return 1;
        double denominator = Math.max(1, Math.abs(dz));
        double radialSquared = dx * dx + dy * dy;
        if (radialSquared >= denominator * denominator * 0.0626) return 1;
        float cone = smooth((float) (Math.sqrt(radialSquared) / denominator) * 4);
        return 1 - axisFade * (1 - cone) * 0.8F;
    }

    public static double distanceToBeam(double x, double y, double z, double length, double radius) {
        double radial = Math.max(0, Math.sqrt(x * x + y * y) - radius);
        double axial = Math.max(-z, Math.max(0, z - length));
        return Math.sqrt(radial * radial + axial * axial);
    }

    private static float smooth(float value) {
        value = Math.max(0, Math.min(1, value));
        return value * value * (3 - 2 * value);
    }
}
