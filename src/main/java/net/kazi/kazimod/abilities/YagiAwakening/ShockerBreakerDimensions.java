package net.kazi.kazimod.abilities.YagiAwakening;

/** Damage and warnings retain their footprint; lightning and charge effects scale independently. */
public final class ShockerBreakerDimensions {
    public static final float SCALE = 3F;
    public static final float VISUAL_SCALE = SCALE * 2.25F;
    public static final double DAMAGE_HEIGHT = 6D * SCALE;
    public static final double DAMAGE_BELOW = .3D * SCALE;
    public static final double VISUAL_HEIGHT = 24D * VISUAL_SCALE;
    public static final double CULL_HEIGHT = 26D * VISUAL_SCALE;
    public static final double RENDER_DISTANCE = 160D + CULL_HEIGHT;

    private ShockerBreakerDimensions() { }

    /** The authored mesh keeps its original proportions; the renderer scales it uniformly. */
    public static float originalRadius(boolean enhanced) { return enhanced ? 4F : 3F; }

    public static float damageRadius(boolean enhanced) { return originalRadius(enhanced) * SCALE; }

    public static float visualRadius(boolean enhanced) { return originalRadius(enhanced) * VISUAL_SCALE; }
}
