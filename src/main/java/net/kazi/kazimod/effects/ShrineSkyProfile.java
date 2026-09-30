package net.kazi.kazimod.effects;

public final class ShrineSkyProfile {
    private ShrineSkyProfile() {
    }

    public static float strength(double distance, float radius) {
        if (!Double.isFinite(distance) || !Float.isFinite(radius) || radius <= 0.0f || distance >= (double)radius) {
            return 0.0f;
        }
        float edge = (float)Math.max(0.0, Math.min(1.0, ((double)radius - distance) / (double)Math.min(8.0f, radius * 0.2f)));
        return edge * edge * (3.0f - 2.0f * edge);
    }

    public static float approach(float current, float target) {
        return current + Math.max(-0.1f, Math.min(0.1f, target - current));
    }

    public static double tint(double original, double target, float strength) {
        return original + (target - original) * (double)Math.max(0.0f, Math.min(1.0f, strength));
    }
}
