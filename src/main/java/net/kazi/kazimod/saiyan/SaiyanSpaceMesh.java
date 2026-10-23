package net.kazi.kazimod.saiyan;

import java.util.Random;
import net.kazi.kazimod.worldturtle.WorldTurtleMesh;
import net.kazi.kazimod.worldturtle.WorldTurtleMesh.Sink;

/** Cached satellite-textured globe and starfield, used only during Celestial Meteor. */
public final class SaiyanSpaceMesh {
    public interface TexturedSink {void vertex(float x,float y,float z,float u,float v,int color,float alpha);}
    private static final int LON=96,LAT=48;
    private static final float[][] VERTICES=new float[LON*LAT*4][5];
    private static final int[] COLORS=new int[LON*LAT*4];
    private static final float[][] STARS=new float[180][4];
    static {
        int v=0;
        for(int y=0;y<LAT;y++)for(int x=0;x<LON;x++){
            double a=-180+x*360.0/LON,b=a+360.0/LON,c=-90+y*180.0/LAT,d=c+180.0/LAT;
            for(double[] p:new double[][]{{a,c},{b,c},{b,d},{a,d}}){
                double longitude=Math.toRadians(p[0]),latitude=Math.toRadians(p[1]);
                float nx=(float)(Math.cos(latitude)*Math.sin(longitude)),ny=(float)Math.sin(latitude),nz=(float)(Math.cos(latitude)*Math.cos(longitude));
                VERTICES[v]=new float[]{nx,ny,nz,(float)((p[0]+180)/360),(float)((90-p[1])/180)};
                float sunlight=(float)Math.pow(Math.max(0,-nx*.52+ny*.28+nz*.806),.65);
                int red=(int)(255*(.035+.965*sunlight)),green=(int)(255*(.055+.945*sunlight)),blue=(int)(255*(.09+.91*sunlight));
                COLORS[v++]=(red<<16)|(green<<8)|blue;
            }
        }
        Random random=new Random(80931);
        for(float[] star:STARS){
            double z=random.nextDouble()*2-1,a=random.nextDouble()*Math.PI*2,r=Math.sqrt(1-z*z);
            star[0]=(float)(Math.cos(a)*r*150);star[1]=(float)(z*150);star[2]=(float)(Math.sin(a)*r*150);star[3]=.07F+random.nextFloat()*.18F;
        }
    }
    private SaiyanSpaceMesh(){}
    public static void backdrop(Sink s,float alpha){
        WorldTurtleMesh.sphere(s,0,0,0,175,175,175,24,12,0x01030b,alpha);
        for(float[] p:STARS){
            float x=p[0],y=p[1],z=p[2],r=p[3];
            double length=Math.sqrt(x*x+z*z);float ux=length<.01?1:(float)(-z/length),uz=length<.01?0:(float)(x/length);
            s.vertex(x-ux*r,y-r,z-uz*r,0xc6dcff,alpha);s.vertex(x+ux*r,y-r,z+uz*r,0xc6dcff,alpha);
            s.vertex(x+ux*r,y+r,z+uz*r,0xc6dcff,alpha);s.vertex(x-ux*r,y+r,z-uz*r,0xc6dcff,alpha);
        }
    }
    public static void earth(TexturedSink s,float alpha){
        for(int i=0;i<VERTICES.length;i++){
            float[] v=VERTICES[i];s.vertex(v[0],v[1],v[2],v[3],v[4],COLORS[i],alpha);
        }
    }
    public static void atmosphere(Sink s,float alpha){
        // Shared radial alpha at each boundary keeps the atmospheric glow free of ring stripes.
        for(int i=0;i<128;i++)for(int band=0;band<12;band++){
            double a=i*Math.PI/64,b=(i+1)*Math.PI/64;float inner=.97F+band*.008F,outer=inner+.008F;
            limbVertex(s,a,inner,alpha);limbVertex(s,b,inner,alpha);
            limbVertex(s,b,outer,alpha);limbVertex(s,a,outer,alpha);
        }
    }
    private static void limbVertex(Sink s,double angle,float radius,float alpha){
        float sunlight=(float)(.3+.7*Math.max(0,-Math.cos(angle)*.75+Math.sin(angle)*.4+.3));
        float glow=(float)Math.exp(-Math.pow((radius-1)/.022,2));
        s.vertex((float)Math.cos(angle)*radius,(float)Math.sin(angle)*radius,0,0x619fff,alpha*glow*sunlight*.55F);
    }
    public static void impact(Sink s,float age,float radius){
        float t=Math.min(1,age/28),fade=1-t;
        SaiyanMesh.ring(s,.12F,radius*(.2F+1.8F*t),.1F+fade*.22F,0xd8eeff,fade,0);
        SaiyanMesh.ring(s,.25F,radius*(.1F+1.25F*t),.08F,0xa0aaff,fade*.7F,.18F);
        for(int i=0;i<20;i++){
            double angle=i*Math.PI/10;float r=radius*(.3F+1.1F*t),x=(float)Math.cos(angle),z=(float)Math.sin(angle);
            SaiyanMesh.line(s,x*r,.1F,z*r,x*(r+2*fade),.2F+2*fade,z*(r+2*fade),.05F,0xe8f6ff,fade);
        }
    }
}
