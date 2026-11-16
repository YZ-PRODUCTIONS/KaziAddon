package net.kazi.kazimod.ghoul;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;

public final class GhoulState {
    public static final String TAG = "KaziGhoul";
    public static final String[] TYPES = {"ukaku", "koukaku", "rinkaku", "bikaku"};
    public static CompoundNBT get(LivingEntity p) {
        if (!p.getPersistentData().contains(TAG, 10)) p.getPersistentData().put(TAG, new CompoundNBT());
        return p.getPersistentData().getCompound(TAG);
    }
    public static boolean ghoul(LivingEntity p) {
        return EntityStatsCapability.get(p).getRace() != null && "zombie".equals(EntityStatsCapability.get(p).getRace().getPath());
    }
    public static int type(LivingEntity p) {
        String sub = EntityStatsCapability.get(p).getSubRace();
        for (int i=0;i<4;i++) if (TYPES[i].equals(sub)) return i;
        return 0;
    }
    public static float rc(LivingEntity p) {
        float v = get(p).getFloat("RC"); return Float.isFinite(v) ? Math.max(0, Math.min(100,v)) : 0;
    }
    public static boolean spend(LivingEntity p, float cost) {
        if (rc(p) < cost) return false;
        get(p).putFloat("RC", rc(p)-cost); return true;
    }
    public static boolean active(LivingEntity p) { return get(p).getBoolean("Deployed"); }
    public static void pose(LivingEntity p, int action, int duration) {
        CompoundNBT n=get(p); n.putInt("Action",action); n.putLong("Start",p.level.getGameTime()); n.putLong("End",p.level.getGameTime()+duration);
    }
    public static int action(LivingEntity p) { return get(p).getLong("End") > p.level.getGameTime() ? get(p).getInt("Action") : 0; }
    private GhoulState() {}
}
