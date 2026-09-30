package net.kazi.kazimod.effects;

import net.MrMagicalCart.cartaddon.effects.SleepyEffect;

/** Keeps the original sleep animation and restrictions without obscuring vision. */
public final class KirinSleepEffect extends SleepyEffect {
    @Override
    public float[] getViewOverlayColor(int duration, int amplifier) {
        return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    }
}
