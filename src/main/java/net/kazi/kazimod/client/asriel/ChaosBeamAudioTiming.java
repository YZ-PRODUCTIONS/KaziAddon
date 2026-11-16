package net.kazi.kazimod.client.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamPattern;

/** Native 30 fps charge cues and hold-loop pitch steps, expressed in Minecraft ticks. */
public final class ChaosBeamAudioTiming {
    public static final int CHARGE_CUES = 6;
    private ChaosBeamAudioTiming() { }
    public static int chargeTick(int cue) { return Math.round((7F + cue * 5F) * 20F / 30F); }
    public static float chargePitch(int cue) { return .5F + cue / 6F; }
    public static float chargeVolume(int cue) { return .6F + cue / 8F; }
    public static int latestChargeCue(float age) {
        if (!Float.isFinite(age) || age < 0 || age >= ChaosBeamPattern.CHARGE_TICKS) return -1;
        int cue = -1;
        while (cue + 1 < CHARGE_CUES && age >= chargeTick(cue + 1)) cue++;
        return cue;
    }
    public static float holdPitch(float age) {
        // obj_lastbeam starts at frame 10 and raises pitch at absolute frames 120 and 240.
        if (age >= Math.round(230F * 20F / 30F)) return 1.8F;
        if (age >= Math.round(110F * 20F / 30F)) return 1.3F;
        return 1F;
    }
    public static float holdVolume(float age) {
        if (!Float.isFinite(age) || age < 0 || age >= ChaosBeamPattern.FIRE_TICKS) return 0;
        float rise = Math.min(.8F, age * .05F * 30F / 20F);
        float fade = 1F - ChaosBeamPattern.smooth((age - ChaosBeamPattern.FIRE_TICKS + 14F) / 14F);
        return rise * fade;
    }
}
