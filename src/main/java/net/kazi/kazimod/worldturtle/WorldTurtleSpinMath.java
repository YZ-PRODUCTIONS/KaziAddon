package net.kazi.kazimod.worldturtle;

/** Swept shell/target box intersection, including targets already inside the shell. */
public final class WorldTurtleSpinMath {
    private WorldTurtleSpinMath(){}
    public static boolean intersects(double[] shell,double dx,double dy,double dz,double[] target){
        double enter=0,exit=1;
        for(int axis=0;axis<3;axis++){
            double delta=axis==0?dx:axis==1?dy:dz;
            if(Math.abs(delta)<1E-8){
                if(shell[axis+3]<target[axis]||shell[axis]>target[axis+3])return false;
            }else{
                double a=(target[axis]-shell[axis+3])/delta,b=(target[axis+3]-shell[axis])/delta;
                enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));
                if(enter>exit)return false;
            }
        }
        return true;
    }
}
