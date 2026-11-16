package net.kazi.kazimod.abilities.NagiAwakening;

/** Shared health gate for Illusion Shatter's melee, aura and passive fruit immunity. */
public final class IllusionShatterHealth {
    private IllusionShatterHealth() { }

    public static boolean active(float health, float maxHealth) {
        // Match Inner Dragons' strict below-30% threshold without rounding 30% upward as a float.
        return Float.isFinite(health) && Float.isFinite(maxHealth)
                && health > 0 && maxHealth > 0
                && health >= maxHealth * InnerDragonsSequence.HEALTH_FRACTION;
    }

    public static boolean immune(float health, float maxHealth) {
        return active(health, maxHealth);
    }
}
