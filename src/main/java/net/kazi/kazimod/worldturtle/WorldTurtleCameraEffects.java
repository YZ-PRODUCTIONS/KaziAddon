package net.kazi.kazimod.worldturtle;

public final class WorldTurtleCameraEffects {
    public static final double RANGE=256;
    private WorldTurtleCameraEffects(){}
    private static double falloff(double distance){
        double edge=Math.max(0,1-Math.max(0,distance)/RANGE);
        return edge*edge;
    }
    public static float flash(double age,double distance,double visibility){
        if(age<0||age>=70)return 0;
        // Fast thermal flash followed by a longer exposure recovery near the blast.
        double exposure=Math.min(1,2.4*falloff(distance));
        double recovery=age<3?1:Math.exp(-(age-3)/(5+15*exposure));
        double tail=Math.min(1,(70-age)/10);
        return (float)(.98*exposure*recovery*tail*Math.max(0,Math.min(1,visibility)));
    }
    public static float destructionShake(double age,double distance){
        double arrival=age-Math.max(0,distance)/1.6;
        if(arrival<0||arrival>55)return 0;
        double impact=Math.min(1,arrival/2)*Math.exp(-arrival/17);
        return (float)(3.2*falloff(distance)*impact);
    }
    public static float supernovaShake(double age,double distance){
        if(age<0||age>=120)return 0;
        double power=age<40?.35*Math.pow(age/40,2):1.15+.35*Math.exp(-(age-40)/5);
        return (float)(power*falloff(distance)*Math.min(1,(120-age)/10));
    }
}
