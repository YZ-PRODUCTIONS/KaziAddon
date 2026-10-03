package net.kazi.kazimod.models.abilities;

/**
 * Weapon gates reuse the contact effect's fine expanding ripples and center glint.
 * Four staggered waves keep the opening visible while a weapon emerges or waits.
 * There is no filled portal basin, molten rim or separate surrounding halo.
 * Local +Z points along the emerging weapon, with the ripple plane near Z = 0.
 */
public final class BabylonPortalMesh {
    @FunctionalInterface
    public interface Sink extends BabylonFlightMesh.Sink { }

    public static final double MAX_RADIUS = 1.35D;
    public static final double MIN_Z = -.30D;
    public static final double MAX_Z = .95D;

    private static final int WAVE_COUNT = 4;
    // Four ripple waves, at most two of which are young enough to emit their flash.
    private static final int[] VERTEX_LIMITS = {5952, 3008, 1888};

    private BabylonPortalMesh() { }

    public static int maxVertices(int detail) {
        return VERTEX_LIMITS[Math.max(0, Math.min(2, detail))];
    }

    public static void draw(Sink surface, Sink glow, int detail, float age, float open,
                            float emergence, float recoil, float collapse, float opacity, int seed) {
        open = smooth(unit(open));
        collapse = smooth(unit(collapse));
        emergence = unit(emergence);
        recoil = unit(recoil);
        opacity = unit(opacity);
        if (open <= .0001F || collapse >= .9999F || opacity <= .0001F) return;
        if (!Float.isFinite(age)) age = 0F;
        detail = Math.max(0, Math.min(2, detail));

        // Use the actual impact ripple, scaled to the weapon opening. Existing
        // impacts still use the very same geometry at their original size.
        double scale = .50D * (.5D + .5D * open);
        float alpha = opacity * open * (1F - collapse)
                * (.44F + .08F * emergence + .18F * recoil);
        double clock = age * .18D + fraction(seed) * BabylonFlightMesh.IMPACT_TICKS;
        for (int wave = 0; wave < WAVE_COUNT; wave++) {
            double cycle = (clock + wave * BabylonFlightMesh.IMPACT_TICKS / (double) WAVE_COUNT)
                    % BabylonFlightMesh.IMPACT_TICKS;
            if (cycle < 0D) cycle += BabylonFlightMesh.IMPACT_TICKS;
            float rippleAge = (float) cycle;
            // A short fade-in also makes the loop boundary continuous.
            float fadeIn = smooth(unit(rippleAge / .8F));
            BabylonFlightMesh.ripple(surface, glow, detail, rippleAge, scale,
                    alpha * fadeIn, seed);
        }
    }

    private static float unit(float value) {
        return Float.isNaN(value) ? 0F : Math.max(0F, Math.min(1F, value));
    }
    private static float smooth(float value) { return value * value * (3F - 2F * value); }
    private static double fraction(int value) {
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        value ^= value >>> 16;
        return (value & 0xFFFFFF) / 16777216D;
    }
}
