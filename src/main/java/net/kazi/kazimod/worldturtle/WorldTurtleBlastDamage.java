package net.kazi.kazimod.worldturtle;

import java.util.*;

/** One damage opportunity per victim, deferred only for an existing vanilla hit cooldown. */
public final class WorldTurtleBlastDamage {
    public static final float RADIUS=96,MIN_DAMAGE=6,MAX_DAMAGE=80;
    public static final int WAVE_END=64,DAMAGE_END=84;
    private final Map<UUID,Float> pending=new HashMap<>();
    private final Set<UUID> resolved=new HashSet<>();
    public static double radius(int age){return Math.min(RADIUS,Math.max(0,age)*RADIUS/60.0);}
    public static float damage(double distance){
        if(!Double.isFinite(distance)||distance>RADIUS)return 0;
        float falloff=(float)Math.max(0,1-Math.max(0,distance)/RADIUS);
        return MIN_DAMAGE+(MAX_DAMAGE-MIN_DAMAGE)*falloff*falloff;
    }
    public void contact(UUID target,double distance,int age){
        if(age<0||age>WAVE_END||!Double.isFinite(distance)||distance>radius(age)||resolved.contains(target))return;
        pending.putIfAbsent(target,damage(distance));
    }
    public float ready(UUID target,int invulnerableTicks,int age){
        if(age>DAMAGE_END||invulnerableTicks>10)return 0;
        return pending.getOrDefault(target,0F);
    }
    public void resolve(UUID target){pending.remove(target);resolved.add(target);}
    public boolean resolved(UUID target){return resolved.contains(target);}
    public void clear(){pending.clear();resolved.clear();}
}
