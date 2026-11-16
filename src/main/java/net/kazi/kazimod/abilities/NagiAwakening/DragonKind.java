package net.kazi.kazimod.abilities.NagiAwakening;

/** Gameplay adaptations of the eight released dragons; shared by the client mesh. */
public enum DragonKind {
    FIRST("first", 0xE4F3FF, 7, 44, 3, 32, 4, .48, DragonSpecial.NULLIFYING_ROAR),
    BLIND("blind", 0x617A9B, 5, 56, 3, 28, 4, .50, DragonSpecial.RENDING_BITE),
    COBRA("cobra", 0x70CEB9, 5, 50, 3, 28, 4, .48, DragonSpecial.VENOM_SPRAY),
    FOUR_EYED("four_eyed", 0xBA8EDD, 4, 64, 9, 24, 2, .45, DragonSpecial.MIND_BEAM),
    CORPSE_FLAME("corpse_flame", 0xE29161, 8, 48, 3, 30, 4, .52, DragonSpecial.SOULFIRE_BREATH),
    ICE_CRYSTAL("ice_crystal", 0xA0DFFA, 6, 60, 10, 48, 12, .38, DragonSpecial.CRYSTAL_PRISON),
    SPEARHEAD("spearhead", 0xDBCE75, 9, 48, 14, 24, 2, .68, DragonSpecial.THUNDER_LANCE),
    ANGEL("angel", 0xF0D6E8, 5, 72, 10, 30, 4, .44, DragonSpecial.HEAVENLY_SLAM);

    public final String id;
    public final int color, attackTicks;
    public final float damage;
    public final double reach, health, armor, speed;
    public final DragonSpecial special;
    DragonKind(String id, int color, float damage, int attackTicks, double reach,
               double health, double armor, double speed, DragonSpecial special) {
        this.id = id;
        this.color = color;
        this.damage = damage;
        this.attackTicks = attackTicks;
        this.reach = reach;
        this.health = health;
        this.armor = armor;
        this.speed = speed;
        this.special = special;
    }
    private static final DragonKind[] KINDS = values();
    public static DragonKind byId(int id) { return KINDS[Math.floorMod(id, KINDS.length)]; }
    public boolean ranged() { return reach > 3; }
}
