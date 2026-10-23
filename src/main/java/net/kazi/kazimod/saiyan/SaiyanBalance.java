package net.kazi.kazimod.saiyan;

/** Shared progression rules, kept independent of client and world state. */
public final class SaiyanBalance {
    public static final int ULTRA_INSTINCT = 6;
    public static final int SPIRIT_COOLDOWN = 120 * 20;
    public static final int SPIRIT_WINDUP = 8 * 20;
    public static final float SPIRIT_SPEED = 2.8F;
    public static final int TRANSFORM_TICKS = 40;
    public static final int INSTINCT_TRANSFORM_TICKS = 120;
    public static final int[] MAX_KI = {100, 180, 260, 360, 500, 700, 900};
    public static final String[] FORMS = {"Base", "Super Saiyan", "Super Saiyan 2", "Super Saiyan 3", "Super Saiyan God", "Super Saiyan Blue", "Ultra Instinct"};
    public static final float[] POWER = {1, 1.3F, 1.5F, 1.8F, 2.1F, 2.5F, 3};
    public static final int[] TRANSMISSION_RANGE = {24, 32, 40, 48, 64, 80, 96};
    public static final float[] DRAIN = {0, 2, 3.6F, 8, 5, 9, 12};
    public static final int[] COLORS = {0x69dfff, 0xffd52b, 0xffe54b, 0xffcf32, 0xff334a, 0x26bfff, 0xd9eaff};
    private SaiyanBalance() {}
    public static int tier(int tier) { return Math.max(0, Math.min(FORMS.length-1, tier)); }
    public static boolean canDodge(float damage) { return Float.isFinite(damage) && damage > 0 && damage < 30; }
    public static int maxKi(int tier) { return MAX_KI[tier(tier)]; }
    public static float flightSpeedMultiplier(int form) { return POWER[tier(form)]; }
    public static int transmissionRange(int form) { return TRANSMISSION_RANGE[tier(form)]; }
    public static int transformationTicks(int form) { return form == ULTRA_INSTINCT ? INSTINCT_TRANSFORM_TICKS : TRANSFORM_TICKS; }
    public static float transformationTime(int form, float elapsed) { return elapsed * TRANSFORM_TICKS / transformationTicks(form); }
    public static float transformationShake(int form, float elapsed) {
        if(form <= 0 || form >= FORMS.length || elapsed < 0 || elapsed >= transformationTicks(form))return 0;
        float time=transformationTime(form,elapsed);
        float buildup=smooth(time/6),release=smooth((time-12)/7),fade=1-smooth((time-30)/10);
        return (form==ULTRA_INSTINCT?4.5F:.65F+form*.2F)*buildup*(.3F+.7F*release)*fade;
    }
    private static float smooth(float value) { value=Math.max(0,Math.min(1,value));return value*value*(3-2*value); }
    public static float clampKi(float ki, int tier) { return Float.isFinite(ki) ? Math.max(0, Math.min(maxKi(tier), ki)) : 0; }
}
