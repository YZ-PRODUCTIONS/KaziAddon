package net.kazi.kazimod.client.asriel;

import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;

/** Shared timeline for the expanding star field and its whiteout, in cutscene ticks. */
final class ChaosBeamFinaleMotion {
    static final int FULL_WHITE_TICK = 288;
    static final int RETURN_FADE_TICKS = AwakeningCutsceneType.CHAOS_BEAM_FINALE.returnFadeTicks;

    private ChaosBeamFinaleMotion() {}

    static double whiteout(double age) {
        return smooth((age - 146) / (FULL_WHITE_TICK - 146));
    }

    static double returnOpacity(double age) {
        return Double.isFinite(age) && age >= 0 ? 1 - smooth(age / RETURN_FADE_TICKS) : 0;
    }

    static double radius(double launchRadius, double flight, double whiteout) {
        return launchRadius + flight * (.026 + .061 * smooth(flight / 70) + .08 * whiteout);
    }

    static double size(double flight, double whiteout) {
        return .075 + (.25 + .25 * whiteout) * smooth(flight / 100);
    }

    static double smooth(double value) {
        double t = Math.max(0, Math.min(1, value));
        return t*t*(3-2*t);
    }
}
