package net.kazi.kazimod.abilities.NagiAwakening;

/** A separate, finite special-attack clock for each summon. */
public final class DragonSpecialCycle {
    public static final int COOLDOWN_TICKS = 15 * 20;
    private int cooldown;
    private int windup = -1;

    public DragonSpecialCycle(int initialDelay) { cooldown = Math.max(0, initialDelay); }

    public void tick() {
        if (cooldown > 0) cooldown--;
        if (windup > 0) windup--;
    }

    public boolean ready() { return cooldown == 0 && windup < 0; }
    public boolean windingUp() { return windup >= 0; }
    public int cooldownRemaining() { return cooldown; }

    public boolean start(DragonSpecial special) {
        if (!ready() || special == null) return false;
        cooldown = COOLDOWN_TICKS;
        windup = special.windupTicks;
        return true;
    }

    public boolean consumeRelease() {
        if (windup != 0) return false;
        windup = -1;
        return true;
    }

    /** A missed/invalid target consumes the attempt, rather than retrying every tick. */
    public void abandonTarget() { windup = -1; }
}
