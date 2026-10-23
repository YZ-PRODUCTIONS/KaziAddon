package net.kazi.kazimod.saiyan;

/** Timing shared by the server carry and its client presentation. */
public final class SaiyanFinisherMotion {
    public static final int LUNGE=0, BITE=1, ASCEND=2, APEX=3, DIVE=4;
    public static final float DRAGON_SCALE=5;
    public static final double DRAGON_HEIGHT=200;
    public static final int SPACE_SETTLE=12, SPACE_HOLD=20;
    private SaiyanFinisherMotion() {}
    public static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    public static int ascentTicks(boolean dragon){return dragon?18:30;}
    public static double apex(double start,boolean dragon){return dragon?start+DRAGON_HEIGHT:Math.max(512,start+384);}
    public static double ascent(double start,boolean dragon,int tick){return start+(apex(start,dragon)-start)*smooth((double)tick/ascentTicks(dragon));}
    public static double diveSpeed(int tick){return Math.min(40,10+tick*4);}
    public static float space(double height,double start){return (float)smooth((height-start-100)/140);}
    public static int apexTicks(boolean dragon){return dragon?6:SPACE_SETTLE+SPACE_HOLD;}
    public static float meteorPitch(int phase,double age){
        if(phase==ASCEND)return (float)(-65+70*smooth((age-22)/8));
        if(phase==APEX)return (float)(5+30*smooth(age/SPACE_SETTLE));
        return (float)(35+45*smooth(age/4));
    }
    public static float spaceOpacity(int phase,float heightBlend){
        if(phase==APEX)return 1;
        return phase==ASCEND||phase==DIVE?Math.max(0,Math.min(1,heightBlend)):0;
    }
    public static float earthRadius(int phase,double age){
        return (float)(50+(phase==DIVE?52*smooth(age/10):0));
    }
    public static double dragonReach(int tick){return (6.9+36.0/16)*DRAGON_SCALE*(.15+.85*smooth(tick/7.0));}
}
