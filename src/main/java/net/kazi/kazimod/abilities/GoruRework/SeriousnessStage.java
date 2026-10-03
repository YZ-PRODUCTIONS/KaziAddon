package net.kazi.kazimod.abilities.GoruRework;

/** Mutually exclusive health stages, shared by Goru awakening abilities. */
public enum SeriousnessStage {
    NORMAL(0, "Normal"),
    STAGE_ONE(1, "Stage 1"),
    STAGE_TWO(2, "Stage 2"),
    STAGE_THREE(3, "Stage 3");

    private final int level;
    private final String displayName;

    SeriousnessStage(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return this.level;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static SeriousnessStage fromHealth(float health, float maxHealth) {
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth)
                || health <= 0.0F || maxHealth <= 0.0F) {
            return NORMAL;
        }
        // Use doubles so values immediately below a boundary do not round up to it.
        double fraction = (double) health / maxHealth;
        if (fraction < 0.25D) return STAGE_THREE;
        if (fraction < 0.50D) return STAGE_TWO;
        if (fraction < 0.75D) return STAGE_ONE;
        return NORMAL;
    }
}
