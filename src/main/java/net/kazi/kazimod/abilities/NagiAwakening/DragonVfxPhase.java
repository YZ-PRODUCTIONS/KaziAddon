package net.kazi.kazimod.abilities.NagiAwakening;

/** Visual phases only; combat durations and hitboxes remain owned by the dragon AI. */
public enum DragonVfxPhase {
    NORMAL(12), SPECIAL(22), CHARGE(30), BIND(54), SLAM(20);

    public final int lifetime;

    DragonVfxPhase(int lifetime) { this.lifetime = lifetime; }

    private static final DragonVfxPhase[] PHASES = values();
    public static DragonVfxPhase byId(int id) { return PHASES[Math.floorMod(id, PHASES.length)]; }
}
