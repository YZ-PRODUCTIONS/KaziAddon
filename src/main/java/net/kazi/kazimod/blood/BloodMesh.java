package net.kazi.kazimod.blood;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Small closed meshes, not particle clouds; all callers share a bounded world buffer. */
public final class BloodMesh extends RenderState {
    public static final RenderType TYPE=RenderType.create("chi_crimson_blood",DefaultVertexFormats.POSITION_COLOR,7,262144,false,true,
            RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                    .setShadeModelState(SMOOTH_SHADE).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE).setOutputState(MAIN_TARGET).createCompositeState(false));
    private BloodMesh(){super("blood",()->{},()->{});}
    public static float smooth(float x){x=MathHelper.clamp(x,0,1);return x*x*(3-2*x);}
    static void vertex(IVertexBuilder b,Matrix4f m,Vector3d p,float shade,float alpha){
        float r=.055F+.81F*shade,g=.003F+.019F*shade,bl=.013F+.035F*shade;
        if(shade>1){float s=Math.min(1,(shade-1)*1.5F);r=.97F;g=.12F+s*.4F;bl=.18F+s*.36F;}
        b.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(r,g,bl,alpha).endVertex();
    }
    static void quad(IVertexBuilder b,Matrix4f m,Vector3d a,Vector3d c,Vector3d d,Vector3d e,float shade,float alpha){
        vertex(b,m,a,shade,alpha);vertex(b,m,c,shade,alpha);vertex(b,m,d,shade,alpha);vertex(b,m,e,shade,alpha);
    }
    static void tube(IVertexBuilder out,Matrix4f m,Vector3d a,Vector3d b,double r1,double r2,float alpha){
        tube(out,m,a,b,r1,r2,alpha,1);
    }
    static void tube(IVertexBuilder out,Matrix4f m,Vector3d a,Vector3d b,double r1,double r2,float alpha,float shade){
        Vector3d dir=b.subtract(a).normalize();if(dir.lengthSqr()<.01)return;
        Vector3d u=dir.cross(Math.abs(dir.y)>.9?new Vector3d(1,0,0):new Vector3d(0,1,0)).normalize(),v=dir.cross(u);
        for(int i=0;i<8;i++){
            double t=i*Math.PI/4,t2=(i+1)*Math.PI/4;
            Vector3d x=u.scale(Math.cos(t)).add(v.scale(Math.sin(t))),y=u.scale(Math.cos(t2)).add(v.scale(Math.sin(t2)));
            quad(out,m,a.add(x.scale(r1)),b.add(x.scale(r2)),b.add(y.scale(r2)),a.add(y.scale(r1)),shade*(i==2?1.12F:i<4?.78F:.12F),alpha);
        }
    }
    static Vector3d point(double lat,double lon,double radius,Vector3d center){return center.add(Math.cos(lat)*Math.cos(lon)*radius,Math.sin(lat)*radius,Math.cos(lat)*Math.sin(lon)*radius);}
    static void orb(IVertexBuilder b,Matrix4f m,Vector3d c,double radius,float alpha){
        drop(b,m,c,new Vector3d(0,1,0),radius,radius,0,alpha);
    }
    static void drop(IVertexBuilder b,Matrix4f m,Vector3d c,Vector3d axis,double radius,double length,float tension,float alpha){
        if(radius<.001||length<.001||alpha<=.001)return;
        Vector3d dir=axis.normalize(),u=side(dir),v=dir.cross(u);
        Vector3d light=new Vector3d(-.3,.8,-.5).normalize();
        Vector3d half=light.add(c.scale(-1).normalize()).normalize();
        for(int j=0;j<6;j++)for(int i=0;i<16;i++){
            double lat=-Math.PI/2+j*Math.PI/6,next=lat+Math.PI/6,lon=i*Math.PI/8;
            dropVertex(b,m,lat,lon,c,dir,u,v,radius,length,tension,light,half,alpha);
            dropVertex(b,m,next,lon,c,dir,u,v,radius,length,tension,light,half,alpha);
            dropVertex(b,m,next,lon+Math.PI/8,c,dir,u,v,radius,length,tension,light,half,alpha);
            dropVertex(b,m,lat,lon+Math.PI/8,c,dir,u,v,radius,length,tension,light,half,alpha);
        }
    }
    private static void dropVertex(IVertexBuilder b,Matrix4f m,double lat,double lon,Vector3d c,Vector3d dir,Vector3d u,Vector3d v,double radius,double length,float tension,Vector3d light,Vector3d half,float alpha){
        double y=Math.sin(lat),r=Math.cos(lat)*(1-tension*y*.32);
        Vector3d radial=u.scale(Math.cos(lon)).add(v.scale(Math.sin(lon)));
        Vector3d n=radial.scale(Math.cos(lat)).add(dir.scale(y));
        float diffuse=(float)Math.max(0,n.dot(light)),specular=(float)Math.pow(Math.max(0,n.dot(half)),18);
        float shade=diffuse<.22F?.08F:diffuse<.58F?.38F:.79F;
        vertex(b,m,c.add(radial.scale(r*radius)).add(dir.scale(y*length)),shade+specular*.72F,alpha);
    }
    static Vector3d side(Vector3d dir){return dir.cross(Math.abs(dir.y)>.92?new Vector3d(1,0,0):new Vector3d(0,1,0)).normalize();}
    static Vector3d bezier(Vector3d a,Vector3d b,Vector3d c,double t){return a.scale((1-t)*(1-t)).add(b.scale(2*t*(1-t))).add(c.scale(t*t));}
    static void filament(IVertexBuilder out,Matrix4f m,Vector3d a,Vector3d control,Vector3d end,double width,float alpha,float shade){
        Vector3d prev=a;
        for(int i=1;i<=10;i++){
            double t=i/10.0;Vector3d next=bezier(a,control,end,t);
            tube(out,m,prev,next,width*(1-t*.8),width*(1-t),alpha,shade);prev=next;
        }
    }
    static void airArc(IVertexBuilder out,Matrix4f m,Vector3d c,Vector3d dir,double radius,double width,float alpha,double start,double sweep){
        Vector3d u=side(dir),v=dir.cross(u);
        for(int i=0;i<20;i++){
            double t=i/20.0,a=start+sweep*t,z=start+sweep*(t+.05);
            Vector3d x=u.scale(Math.cos(a)).add(v.scale(Math.sin(a))),y=u.scale(Math.cos(z)).add(v.scale(Math.sin(z)));
            airVertex(out,m,c.add(x.scale(radius)),alpha*(float)Math.sin(t*Math.PI));
            airVertex(out,m,c.add(x.scale(radius-width)),alpha*(float)Math.sin(t*Math.PI));
            airVertex(out,m,c.add(y.scale(radius-width)),alpha*(float)Math.sin((t+.05)*Math.PI));
            airVertex(out,m,c.add(y.scale(radius)),alpha*(float)Math.sin((t+.05)*Math.PI));
        }
    }
    private static void airVertex(IVertexBuilder b,Matrix4f m,Vector3d p,float alpha){
        b.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(.82F,.77F,.78F,Math.max(0,alpha)).endVertex();
    }
    static void ring(IVertexBuilder b,Matrix4f m,Vector3d c,Vector3d normal,double radius,double width,float alpha,float age){
        Vector3d u=normal.cross(Math.abs(normal.y)>.9?new Vector3d(1,0,0):new Vector3d(0,1,0)).normalize(),v=normal.cross(u).normalize();
        for(int i=0;i<32;i++){
            double a=i*Math.PI/16+age*.08,z=a+Math.PI/16;
            Vector3d x=u.scale(Math.cos(a)).add(v.scale(Math.sin(a))),y=u.scale(Math.cos(z)).add(v.scale(Math.sin(z)));
            quad(b,m,c.add(x.scale(radius-width)),c.add(x.scale(radius)),c.add(y.scale(radius)),c.add(y.scale(radius-width)),i%7==0?1.35F:.85F,alpha);
        }
    }
    static void disc(IVertexBuilder b,Matrix4f m,Vector3d c,Vector3d dir,float age,float alpha){
        Vector3d normal=new Vector3d(dir.x*.18,1,dir.z*.18).normalize();
        ring(b,m,c,normal,1.08,.26,alpha,age*4);
        ring(b,m,c.add(0,.018,0),normal,.83,.07,alpha,age*-5);
        for(int i=0;i<7;i++){
            double a=age*.8+i*Math.PI*2/7;
            Vector3d tip=c.add(Math.cos(a)*1.07,.03,Math.sin(a)*1.07);
            Vector3d tangent=new Vector3d(-Math.sin(a),-.06,Math.cos(a));
            filament(b,m,tip,tip.add(tangent.scale(.7)),tip.add(tangent.scale(1)).subtract(dir.scale(.3)),.035,alpha*.8F,.8F);
            drop(b,m,tip.add(tangent.scale(.8)),tangent,.035,.12,1,alpha*.7F);
        }
    }
    static void tether(IVertexBuilder b,Matrix4f m,Vector3d start,Vector3d end,float age,float alpha){
        Vector3d delta=end.subtract(start),side=delta.cross(new Vector3d(0,1,0)).normalize();
        Vector3d prev=start;
        for(int i=1;i<=18;i++){
            double t=i/18.0,weight=Math.sin(t*Math.PI);
            Vector3d p=start.add(delta.scale(t)).add(side.scale(Math.sin(t*9-age*.4)*.075*weight)).add(0,-.22*weight,0);
            double radius=.045+.012*Math.sin(t*16-age*.8);
            tube(b,m,prev,p,radius,radius,alpha);prev=p;
        }
    }
    static void fist(IVertexBuilder b,Matrix4f m,Vector3d c,Vector3d dir,float alpha){
        Vector3d side=dir.cross(new Vector3d(0,1,0)).normalize(),up=side.cross(dir).normalize();
        tube(b,m,c.subtract(dir.scale(.3)),c.add(dir.scale(.15)),.2,.24,alpha);
        for(int i=0;i<4;i++)orb(b,m,c.add(side.scale((i-1.5)*.105)).add(dir.scale(.22)).add(up.scale(.12)),.08,alpha);
    }
}
