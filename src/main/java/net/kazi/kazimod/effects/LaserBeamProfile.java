package net.kazi.kazimod.effects;

import java.util.Locale;

public final class LaserBeamProfile {
    private LaserBeamProfile() {
    }

    public static boolean isLaser(String parent, int segments, int branches, boolean vanilla) {
        String name = parent == null ? "" : parent.toLowerCase(Locale.ROOT);
        // Explicit exclusions must run before the generic straight-beam fallback.
        if (name.contains("damned") || name.contains("bolo")) return false;
        if (name.contains("gastille") || name.contains("radical") || name.contains("amaterasu") || name.contains("rho") || name.contains("maser") || name.contains("fuoco") || name.contains("bazooka_burn") || name.contains("yata")) {
            return true;
        }
        return !vanilla && segments == 1 && branches == 1;
    }

    public static float radius(float damage) {
        float safe = Float.isFinite(damage) ? Math.max(0.0f, Math.min(5000.0f, damage)) : 0.0f;
        return 0.08f + 0.07f * (float)Math.sqrt(safe);
    }

    public static float widthScale(float originalSize, float damage) {
        float size = Float.isFinite(originalSize) ? Math.max(0.0F, originalSize) : 0.0F;
        // Original renderer: four shells, outer radius = size / 2 + 3 * size.
        // New mesh: widest entry ring radius = (3.9 + 0.8) * radius(damage).
        return size * 3.5F / (radius(damage) * 4.7F);
    }

    public static float opacity(float remainingLife, int maxLife) {
        return Math.max(0.0f, Math.min(1.0f, remainingLife / Math.max(1.0f, Math.min(6.0f, (float)maxLife * 0.2f))));
    }
}
