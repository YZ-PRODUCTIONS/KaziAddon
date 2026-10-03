package net.kazi.kazimod.projectiles;

public final class ClashMath {
    private ClashMath() {}

    public static float remaining(float health, float incomingDamage) {
        return Math.max(0.0F, health - Math.max(0.0F, incomingDamage));
    }

    public static boolean beamDestroys(float beamDamage, float projectileHealth) {
        return beamDamage > projectileHealth;
    }
}
