package net.kazi.kazimod.mammoth;

/** Shared combat dimensions and bounded distance curves, independent of rendering. */
public final class MammothCombatMath {
    public static final int STAMPEDE_CHARGE=60,STAMPEDE_DURATION=100,STOMP_DURATION=96,VACUUM_DURATION=80;
    public static final double STAMPEDE_SPEED=2.3,STOMP_RADIUS=15,VACUUM_RADIUS=30;
    public static boolean inStomp(double x,double y,double z){return x*x+z*z<=STOMP_RADIUS*STOMP_RADIUS&&Math.abs(y)<=6;}
    public static float finalDamage(double distance){return (float)Math.max(66,80-Math.max(0,distance)*.5);}
    public static double pullSpeed(double distance){return Math.min(1.1,Math.max(0,distance-.8)*.12);}
    public static float reducedDamage(float damage,boolean full){return damage*(full?.75F:.9F);}
    public static double[] vacuumTip(boolean full,float age,float relativeYaw,float pitch){
        double phase=age/28.0*Math.PI*2;
        double[] p=full?new double[]{0,8.5,-61.05}:new double[]{0,12.3,-15.6};
        hinge(p,full?new double[]{0,16,-49}:new double[]{0,12.8,-9.3},-35+2*Math.sin(phase-Math.PI*.5),0);
        hinge(p,full?new double[]{0,32,-46}:new double[]{0,20,-8},20+1.2*Math.sin(phase-Math.PI*.3),0);
        hinge(p,full?new double[]{0,43,-40}:new double[]{0,26.6,-4.1},65+Math.sin(phase),0);
        hinge(p,full?new double[]{0,42,-23}:new double[]{0,24,0},-Math.max(-30,Math.min(30,pitch))*.7*(full?.55:1),Math.max(full?-28:-60,Math.min(full?28:60,relativeYaw))*.7);
        return p;
    }
    private static void hinge(double[]p,double[]o,double pitch,double yaw){
        double a=Math.toRadians(pitch),b=Math.toRadians(yaw),x=p[0]-o[0],y=p[1]-o[1],z=p[2]-o[2];
        double yy=y*Math.cos(a)-z*Math.sin(a),zz=y*Math.sin(a)+z*Math.cos(a);
        p[0]=o[0]+x*Math.cos(b)+zz*Math.sin(b);p[1]=o[1]+yy;p[2]=o[2]-x*Math.sin(b)+zz*Math.cos(b);
    }
    private MammothCombatMath(){}
}
