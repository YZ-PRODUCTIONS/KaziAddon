package net.kazi.kazimod.abilities.NagiAwakening;

/** Distinct attacks used by the eight Dragon Strike summons. */
public enum DragonSpecial {
    NULLIFYING_ROAR("Nullifying Roar", 8, 16),
    RENDING_BITE("Rending Bite", 3.5, 10),
    VENOM_SPRAY("Venom Spray", 12, 16),
    MIND_BEAM("Mind-Piercing Beam", 32, 24),
    SOULFIRE_BREATH("Soulfire Breath", 12, 20),
    CRYSTAL_PRISON("Crystal Prison", 16, 20),
    THUNDER_LANCE("Thunder Lance", 22, 14),
    HEAVENLY_SLAM("Heavenly Slam", 4, 12);

    public final String displayName;
    public final double range;
    public final int windupTicks;

    DragonSpecial(String displayName, double range, int windupTicks) {
        this.displayName = displayName;
        this.range = range;
        this.windupTicks = windupTicks;
    }
}
