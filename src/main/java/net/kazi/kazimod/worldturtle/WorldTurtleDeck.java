package net.kazi.kazimod.worldturtle;

import net.kazi.kazimod.models.zoan.CerberusRig;

/** Plane matching the top of the ocean disc, including the turtle's root tilt. */
public final class WorldTurtleDeck {
    public final double[] origin,u,v,normal;
    public final double radius;
    public WorldTurtleDeck(CerberusRig.Pose pose,float yaw,float scale,double x,double y,double z){
        origin=point(new double[]{0,66.5,0},pose,yaw,scale);
        double[] a=point(new double[]{1,66.5,0},pose,yaw,scale),b=point(new double[]{0,66.5,1},pose,yaw,scale);
        u=new double[3];v=new double[3];for(int i=0;i<3;i++){u[i]=(a[i]-origin[i])*16/scale;v[i]=(b[i]-origin[i])*16/scale;}
        normal=new double[]{v[1]*u[2]-v[2]*u[1],v[2]*u[0]-v[0]*u[2],v[0]*u[1]-v[1]*u[0]};
        origin[0]+=x;origin[1]+=y;origin[2]+=z;radius=56*scale/16;
    }
    private static double[] point(double[] p,CerberusRig.Pose pose,float yaw,float scale){
        if(pose!=null){for(int axis=0;axis<3;axis++){double angle=Math.toRadians(pose.rotation[axis]),c=Math.cos(angle),s=Math.sin(angle);int a=(axis+1)%3,b=(axis+2)%3;double old=p[a];p[a]=old*c-p[b]*s;p[b]=old*s+p[b]*c;}
            for(int i=0;i<3;i++)p[i]+=pose.position[i];}
        double c=Math.cos(Math.toRadians(yaw)),s=Math.sin(Math.toRadians(yaw));
        return new double[]{(c*p[0]+s*p[2])*scale/16,(p[1]/16+1.001)*scale,(s*p[0]-c*p[2])*scale/16};
    }
    public double height(double x,double z){return origin[1]-(normal[0]*(x-origin[0])+normal[2]*(z-origin[2]))/normal[1];}
    public double[] local(double x,double y,double z){double X=x-origin[0],Y=y-origin[1],Z=z-origin[2];return new double[]{X*u[0]+Y*u[1]+Z*u[2],X*v[0]+Y*v[1]+Z*v[2]};}
    public boolean contains(double x,double z){double[] p=local(x,height(x,z),z);return p[0]*p[0]+p[1]*p[1]<=radius*radius;}
    public boolean overlaps(double minX,double maxX,double minZ,double maxZ){
        double x=Math.max(minX,Math.min(origin[0],maxX));
        double z=Math.max(minZ,Math.min(origin[2],maxZ));
        return contains(x,z);
    }
    public double[] at(double a,double b){return new double[]{origin[0]+a*u[0]+b*v[0],origin[1]+a*u[1]+b*v[1],origin[2]+a*u[2]+b*v[2]};}
}
