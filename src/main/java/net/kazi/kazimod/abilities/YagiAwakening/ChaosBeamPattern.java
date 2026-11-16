package net.kazi.kazimod.abilities.YagiAwakening;

/** Timing and tapered combat volume for the final-form, two-handed Chaos Beam. */
public final class ChaosBeamPattern {
    public static final int CHARGE_TICKS = 40;
    public static final int FIRE_TICKS = 240;
    public static final int DAMAGE_INTERVAL = 20;
    public static final float DAMAGE = 20F;
    public static final double RANGE = 150D;
    public static final double CORE_RADIUS = 8D;
    private static final double INITIAL_END_RADIUS = 32D;
    public static final double MAX_RADIUS = 80D;

    private ChaosBeamPattern() { }

    public static double endRadius(float age) {
        return INITIAL_END_RADIUS + (MAX_RADIUS - INITIAL_END_RADIUS) * smooth(age / FIRE_TICKS);
    }

    public static double radiusAt(double along, float age) {
        return CORE_RADIUS + (endRadius(age) - CORE_RADIUS) * Math.max(0, Math.min(1, along / RANGE));
    }

    public static boolean damageTick(int age) {
        return age >= 0 && age < FIRE_TICKS && age % DAMAGE_INTERVAL == 0;
    }

    public static boolean contains(double along, double radialSquared, double targetRadius, double length, float age) {
        if (!Double.isFinite(along + radialSquared + targetRadius + length) || !Float.isFinite(age)
                || age < 0 || age >= FIRE_TICKS || along < 0 || along > Math.min(RANGE, length)) return false;
        double radius = radiusAt(along, age) + Math.max(0, targetRadius);
        return radialSquared >= 0 && radialSquared <= radius * radius;
    }

    public static float fade(float age) {
        return smooth(age / 4F) * (1F - smooth((age - FIRE_TICKS + 14F) / 14F));
    }

    public static float smooth(float value) {
        float t = Math.max(0F, Math.min(1F, value));
        return t * t * (3F - 2F * t);
    }
}
