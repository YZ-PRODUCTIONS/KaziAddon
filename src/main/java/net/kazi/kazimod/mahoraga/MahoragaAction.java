package net.kazi.kazimod.mahoraga;

public enum MahoragaAction {
    IDLE("idle", 0), SUMMON("summon", 80),
    SLASH_RIGHT("slash_right", 24), SLASH_LEFT("slash_left", 24),
    OVERHEAD_SLAM("overhead_slam", 40), PUNCH_BARRAGE("punch_barrage", 42),
    BLOCK("block", 24), DODGE("dodge", 14),
    AIR_JUMP("air_jump", 20), AIR_TAKEDOWN("air_takedown", 30),
    THROW_BLOCK("throw_block", 36), AIR_BLAST("air_blast", 44),
    DISMISS("dismiss", 60);

    public final String clip;
    public final int ticks;
    MahoragaAction(String clip, int ticks) { this.clip = clip; this.ticks = ticks; }
    public boolean canRecoverIntoGuard(float age) {
        return this==IDLE || ((this==SLASH_LEFT || this==SLASH_RIGHT) && age>=16)
                || (this==OVERHEAD_SLAM && age>=29) || (this==PUNCH_BARRAGE && age>=33);
    }
    public static MahoragaAction from(int id) { return id >= 0 && id < values().length ? values()[id] : IDLE; }
}
