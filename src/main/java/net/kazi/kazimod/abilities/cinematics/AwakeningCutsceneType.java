package net.kazi.kazimod.abilities.cinematics;

/** Stable wire IDs and shared deadlines. Contains no client classes. */
public enum AwakeningCutsceneType {
    INNER_DRAGONS(0, 160), EA_DRAW(1, 160), HYPER_GONER(2, 260), CHAOS_BEAM_FINALE(3, 312, 40);
    public final int wireId;
    public final int durationTicks;
    public final int returnFadeTicks;
    public final int protectionTicks;
    AwakeningCutsceneType(int wireId, int durationTicks) {
        this(wireId, durationTicks, 0);
    }
    AwakeningCutsceneType(int wireId, int durationTicks, int returnFadeTicks) {
        this.wireId = wireId;
        this.durationTicks = durationTicks;
        this.returnFadeTicks = returnFadeTicks;
        this.protectionTicks = durationTicks + returnFadeTicks;
    }
    public boolean active(float age) { return Float.isFinite(age) && age >= 0 && age < durationTicks; }
    public static AwakeningCutsceneType fromWire(int id) {
        for (AwakeningCutsceneType type : values()) if (type.wireId == id) return type;
        throw new IllegalArgumentException("Unknown awakening cinematic: " + id);
    }
}
