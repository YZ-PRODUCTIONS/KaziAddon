package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.entities.BabylonVolleyPattern;

/** Visual envelopes only; server shot timing and weapon damage live elsewhere. */
public final class BabylonVisualTimeline {
    public static final float SHOT_CLOSE_DELAY = 2F;
    public static final float SHOT_CLOSE_TICKS = 12F;
    public static final float CANCEL_TICKS = 10F;

    /** Reused by the renderer so sampling 25 gates produces no garbage. */
    public static final class Frame {
        public float open, emergence, recoil, collapse, opacity, weaponOpacity;
    }

    private BabylonVisualTimeline() { }

    public static void sample(Frame out, int portal, float castAge, int shotsFired,
                              boolean fading, float fadeAge) {
        sample(out, portal, castAge, shotsFired, fading, fadeAge, BabylonVolleyPattern.PORTALS);
    }
    public static void sample(Frame out, int portal, float castAge, int shotsFired,
                              boolean fading, float fadeAge, int portalCount) {
        int shot = BabylonVolleyPattern.firstShotForPortal(portal, portalCount);
        // A short wave breaks up the opening without delaying the first weapon.
        float delay = (portal * 7 % 5) * .5F;
        out.open = unit((castAge - delay) / BabylonVolleyPattern.GATE_OPEN_TICKS);
        out.emergence = smooth(BabylonVolleyPattern.weaponFormation(shot, castAge));
        float cancel = fading ? smooth(fadeAge / CANCEL_TICKS) : 0F;
        boolean fired = shot < shotsFired;
        float since = castAge - BabylonVolleyPattern.PREPARATION_TICKS
                - BabylonVolleyPattern.shotTick(shot) + (fading ? fadeAge : 0F);
        out.recoil = fired ? 1F - smooth(Math.max(0F, since) / 6F) : 0F;
        float spent = fired ? unit((since - SHOT_CLOSE_DELAY) / SHOT_CLOSE_TICKS) : 0F;
        // Combining closures retains the gate's exact shape when cancellation begins.
        out.collapse = 1F - (1F - spent) * (1F - cancel);
        out.opacity = 1F - cancel;
        out.weaponOpacity = fired ? 0F : 1F - cancel;
    }

    public static float smooth(float value) {
        float x = unit(value);
        return x * x * (3F - 2F * x);
    }

    private static float unit(float value) { return Math.max(0F, Math.min(1F, value)); }
}
