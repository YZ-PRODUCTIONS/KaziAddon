package net.kazi.kazimod.renderers.entities.asriel;

/** Shared smooth ink ribbons for Astral Flight's wings and the low-health awakening effects. */
public final class AsrielFlowPattern {
    private AsrielFlowPattern() { }

    public static float phase(float age) {
        float phase = age / 280F;
        return phase - (float) Math.floor(phase);
    }

    public static float shade(float flow, float y) {
        float warp = (float) Math.sin(y * 3.5F + flow * Math.PI * 2) * .12F
                + (float) Math.sin(y * 7 - flow * Math.PI * 4) * .035F;
        float band = flow * 8 + warp;
        band -= Math.floor(band);
        float edge = Math.max(0, Math.min(1, (Math.abs(band - .5F) - .06F) / .30F));
        return 1 - .96F * (1 - smooth(edge));
    }

    /** Continuous first and second derivatives avoid snapping at band and hue boundaries. */
    public static float smooth(float t) { return t*t*t*(t*(t*6-15)+10); }
}
