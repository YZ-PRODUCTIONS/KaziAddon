package net.kazi.kazimod.petal;

public final class HakuteikenTiming {
    public static final int CHARGE = 100;
    public static final int RUSH_TICKS = 24;
    public static final double RUSH_SPEED = 1.65;
    public static final int RUSH_END = CHARGE + RUSH_TICKS;
    public static final int DURATION = RUSH_END + 16;

    public static boolean rushing(float age) {
        return age >= CHARGE && age < RUSH_END;
    }

    private HakuteikenTiming() {}
}
