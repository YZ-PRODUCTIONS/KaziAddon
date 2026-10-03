package net.kazi.kazimod.preserved.sahur;

public final class SahurRigMath {
    public static float[] wingAnchor(boolean right) { return new float[]{right?2.13669F:-3.13669F,-9.89787F,8.22222F}; }
    public static float[] rotate(float x,float y,float z,float rx,float ry,float rz) {
        double yy=y*Math.cos(rx)-z*Math.sin(rx),zz=y*Math.sin(rx)+z*Math.cos(rx);
        double xx=x*Math.cos(ry)+zz*Math.sin(ry);zz=-x*Math.sin(ry)+zz*Math.cos(ry);
        return new float[]{(float)(xx*Math.cos(rz)-yy*Math.sin(rz)),(float)(xx*Math.sin(rz)+yy*Math.cos(rz)),(float)zz};
    }
    public static float[] anchorPosition(float[] rest,float[] anchor,float[] rotation,float[] liveAnchor){
        float[] d=rotate(rest[0]-anchor[0],rest[1]-anchor[1],rest[2]-anchor[2],rotation[0],rotation[1],rotation[2]);
        return new float[]{liveAnchor[0]+d[0],liveAnchor[1]+d[1],liveAnchor[2]+d[2]};
    }
}
