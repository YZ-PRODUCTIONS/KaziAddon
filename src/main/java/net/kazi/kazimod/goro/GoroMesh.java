package net.kazi.kazimod.goro;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Bounded, deterministic geometry. Noise interpolates between frames instead of flickering randomly. */
public final class GoroMesh extends RenderType {
    private static final RenderType SOLID = create("kazi_storm_core", DefaultVertexFormats.POSITION_COLOR, 7, 262144,
            false, false, State.builder().setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
    private static final RenderType GLOW = create("kazi_electricity", DefaultVertexFormats.POSITION_COLOR, 7, 262144,
            false, false, State.builder().setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static final Vector3d[][] SPHERE = sphereGrid(64, 32);

    private GoroMesh(String name, Runnable setup, Runnable clear) {
        super(name, DefaultVertexFormats.POSITION_COLOR, 7, 256, false, false, setup, clear);
    }
    private static Vector3d[][] sphereGrid(int lon, int lat) {
        Vector3d[][] result = new Vector3d[lat + 1][lon + 1];
        for (int y=0;y<=lat;y++) for(int x=0;x<=lon;x++) result[y][x]=sphere(x*Math.PI*2/lon,y*Math.PI/lat);
        return result;
    }
    private static Vector3d sphere(double longitude, double latitude) {
        return new Vector3d(Math.cos(longitude)*Math.sin(latitude), Math.cos(latitude), Math.sin(longitude)*Math.sin(latitude));
    }
    private static double hash(int key) { return Math.sin(key*127.1+311.7)*43758.5453 % 1; }
    private static double noise(int key, float time) {
        int frame=(int)Math.floor(time); double f=time-frame; f=f*f*(3-2*f);
        return MathHelper.lerp(f,hash(key+frame*7919),hash(key+(frame+1)*7919));
    }
    private static void vertex(IVertexBuilder out, Matrix4f m, Vector3d p, float r,float g,float b,float a) {
        out.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(r,g,b,a).endVertex();
    }
    private static void quad(IVertexBuilder out,Matrix4f m,Vector3d a,Vector3d b,Vector3d c,Vector3d d,float r,float g,float blue,float alpha) {
        vertex(out,m,a,r,g,blue,alpha);vertex(out,m,b,r,g,blue,alpha);
        vertex(out,m,c,r,g,blue,alpha);vertex(out,m,d,r,g,blue,alpha);
    }
    private static void line(IVertexBuilder out,Matrix4f m,Vector3d a,Vector3d b,double width,float alpha,int layer) {
        Vector3d direction=b.subtract(a).normalize();
        Vector3d side=direction.cross(Math.abs(direction.y)>.9?new Vector3d(1,0,0):new Vector3d(0,1,0)).normalize().scale(width);
        Vector3d other=direction.cross(side).normalize().scale(width);
        float red=layer==2?.84F:.12F,green=layer==2?.98F:.48F;
        if(layer==2) {
            quad(out,m,a.add(side),b.add(side),b.subtract(side),a.subtract(side),red,green,1,alpha);
            quad(out,m,a.add(other),b.add(other),b.subtract(other),a.subtract(other),red,green,1,alpha);
        } else {
            softStrip(out,m,a,b,side,red,green,alpha);
            softStrip(out,m,a,b,other,red,green,alpha);
        }
    }
    private static void softStrip(IVertexBuilder out,Matrix4f m,Vector3d a,Vector3d b,Vector3d side,float red,float green,float alpha) {
        vertex(out,m,a.add(side),red,green,1,0);vertex(out,m,b.add(side),red,green,1,0);
        vertex(out,m,b,red,green,1,alpha);vertex(out,m,a,red,green,1,alpha);
        vertex(out,m,a,red,green,1,alpha);vertex(out,m,b,red,green,1,alpha);
        vertex(out,m,b.subtract(side),red,green,1,0);vertex(out,m,a.subtract(side),red,green,1,0);
    }
    private static void arc(IVertexBuilder out,Matrix4f m,Vector3d start,Vector3d end,int seed,float time,double jitter,double width,float alpha) {
        Vector3d delta=end.subtract(start),dir=delta.normalize();
        Vector3d side=dir.cross(Math.abs(dir.y)>.9?new Vector3d(1,0,0):new Vector3d(0,1,0)).normalize();
        Vector3d up=dir.cross(side).normalize();
        Vector3d[] points=new Vector3d[13];
        for(int i=0;i<points.length;i++) {
            double f=i/12.0, envelope=Math.sin(Math.PI*f);
            points[i]=start.add(delta.scale(f)).add(side.scale(noise(seed+i*53,time*.22F)*jitter*envelope))
                    .add(up.scale(noise(seed+i*97+101,time*.22F)*jitter*envelope));
        }
        for(int layer=0;layer<3;layer++) {
            double w=width*(layer==0?5:layer==1?2: .45);
            float a=alpha*(layer==0?.09F:layer==1?.25F:.95F);
            for(int i=1;i<points.length;i++) line(out,m,points[i-1],points[i],w,a,layer);
        }
    }
    private static void ring(IVertexBuilder out,Matrix4f m,double radius,double y,double width,float alpha,float time) {
        Vector3d last=new Vector3d(radius,y,0);
        for(int i=1;i<=96;i++) {
            double angle=i*Math.PI/48, r=radius*(1+.012*Math.sin(i*2.6+time));
            Vector3d next=new Vector3d(Math.cos(angle)*r,y,Math.sin(angle)*r);
            line(out,m,last,next,width,alpha,1);last=next;
        }
    }
    public static void raigo(MatrixStack stack,IRenderTypeBuffer buffers,float radius,float age,Vector3d eye,int seed,boolean far) {
        Matrix4f matrix=stack.last().pose();
        IVertexBuilder out=buffers.getBuffer(SOLID);
        int step=far?2:1;
        for(int y=0;y<32;y+=step) for(int x=0;x<64;x+=step) {
            Vector3d[] corners={SPHERE[y][x],SPHERE[y+step][x],SPHERE[y+step][x+step],SPHERE[y][x+step]};
            for(Vector3d p:corners) {
                double cloud=.5+.2*Math.sin(p.x*19+p.y*13+Math.sin(p.z*23)+age*.007)
                        +.15*Math.sin(p.y*31-p.z*17+Math.sin(p.x*27));
                float shade=(float)cloud;
                vertex(out,matrix,p.scale(radius),.012F+shade*.025F,.016F+shade*.028F,.055F+shade*.065F,1);
            }
        }
        // Complete the opaque pass before acquiring the additive buffer.
        out=buffers.getBuffer(GLOW);
        Vector3d view=eye.normalize();
        halo(out,matrix,radius,view);
        for(int y=0;y<32;y+=step) for(int x=0;x<64;x+=step) {
            Vector3d[] corners={SPHERE[y][x],SPHERE[y+step][x],SPHERE[y+step][x+step],SPHERE[y][x+step]};
            for(Vector3d p:corners) {
                float fresnel=(float)Math.pow(1-Math.abs(p.dot(view)),5);
                vertex(out,matrix,p.scale(radius*1.012),.1F,.42F,1,fresnel*.38F);
            }
        }
        int count=far?24:48;
        for(int n=0;n<count;n++) {
            double longitude=n*2.39996+seed*.015,latitude=.25+(n%9)/9.0*2.6;
            Vector3d last=sphere(longitude,latitude).scale(radius*1.025);
            for(int j=1;j<=6;j++) {
                double lon=longitude+j*.09+noise(n*41+j,age*.16F)*.07;
                double lat=latitude+j*.055+noise(n*61+j,age*.16F)*.055;
                Vector3d next=sphere(lon,lat).scale(radius*1.026);
                for(int layer=0;layer<3;layer++) line(out,matrix,last,next,radius*(layer==0?.032:layer==1?.012:.003),layer==0?.1F:layer==1?.38F:.95F,layer);
                if(j==3 && n%2==0) arc(out,matrix,next,sphere(lon+.16,lat-.12).scale(radius*1.03),n+seed,age,radius*.025,radius*.004,.85F);
                last=next;
            }
            if(n%3==0) {
                Vector3d end=last.normalize().scale(radius*(1.4+.5*Math.abs(hash(n+seed))));
                arc(out,matrix,last,end,n*101+seed,age,radius*.22,radius*.012,.85F);
                if(!far) arc(out,matrix,last.add(end.subtract(last).scale(.55)),end.add(radius*hash(n+21)*.3,radius*hash(n+37)*.3,radius*hash(n+59)*.3),n+7,age,radius*.1,radius*.005,.6F);
            }
        }
    }
    private static void halo(IVertexBuilder out,Matrix4f m,float radius,Vector3d view) {
        Vector3d right=view.cross(Math.abs(view.y)>.9?new Vector3d(1,0,0):new Vector3d(0,1,0)).normalize();
        Vector3d up=view.cross(right).normalize();
        for(int ring=0;ring<12;ring++)for(int i=0;i<64;i++) {
            double a=i*Math.PI/32,b=(i+1)*Math.PI/32;
            Vector3d u=right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vector3d v=right.scale(Math.cos(b)).add(up.scale(Math.sin(b)));
            double inner=radius*(1+ring*.014),outer=radius*(1+(ring+1)*.014);
            float alpha=.25F*(1-ring/12F)*(1-ring/12F),next=.25F*(1-(ring+1)/12F)*(1-(ring+1)/12F);
            vertex(out,m,u.scale(inner),.08F,.4F,1,alpha);vertex(out,m,u.scale(outer),.08F,.4F,1,next);
            vertex(out,m,v.scale(outer),.08F,.4F,1,next);vertex(out,m,v.scale(inner),.08F,.4F,1,alpha);
        }
    }
    /** Local +Y axis runs from the source to the live, clash-limited beam endpoint. */
    public static void beam(MatrixStack stack,IRenderTypeBuffer buffers,float radius,float length,float age,float alpha,int seed,boolean pillar,boolean far) {
        IVertexBuilder out=buffers.getBuffer(GLOW);Matrix4f m=stack.last().pose();
        if(length<=0 || alpha<=0)return;
        int sides=far?12:24,sections=Math.min(far?12:32,Math.max(2,(int)(length/4)));
        for(int shell=0;shell<3;shell++) {
            double r=radius*(shell==0?1.05:shell==1?.58:.2);
            for(int j=0;j<sections;j++) for(int i=0;i<sides;i++) {
                double a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides;
                double y0=length*j/sections,y1=length*(j+1)/sections;
                double r0=r*(1+.065*Math.sin(age*.5-j*.8)),r1=r*(1+.065*Math.sin(age*.5-(j+1)*.8));
                float pulse=.85F+.15F*MathHelper.sin(age*.8F-j*.75F);
                quad(out,m,new Vector3d(Math.cos(a)*r0,y0,Math.sin(a)*r0),new Vector3d(Math.cos(a)*r1,y1,Math.sin(a)*r1),
                        new Vector3d(Math.cos(b)*r1,y1,Math.sin(b)*r1),new Vector3d(Math.cos(b)*r0,y0,Math.sin(b)*r0),
                        shell==2?.85F:.1F,shell==2?.97F:.45F,1,alpha*pulse*(shell==0?.065F:shell==1?.12F:.55F));
            }
        }
        for(int n=0;n<(far?4:pillar?9:6);n++) {
            double angle=n*2.39996;
            Vector3d last=new Vector3d(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
            int spans=Math.min(far?4:8,Math.max(1,(int)(length/12)));
            for(int j=1;j<=spans;j++) {
                double turn=angle+j*.32+noise(n*71+j,age*.1F)*.3;
                Vector3d end=new Vector3d(Math.cos(turn)*radius*(.7+.3*hash(n+j)),length*j/spans,Math.sin(turn)*radius);
                arc(out,m,last,end,seed+n*117+j,age,radius*.7,radius*.025,alpha);
                last=end;
            }
        }
        ring(out,m,radius*1.7,length,.035*radius,alpha*.45F,age);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4+seed;
            arc(out,m,new Vector3d(0,length,0),new Vector3d(Math.cos(a)*radius*3,length-.2,Math.sin(a)*radius*3),seed+i,age,radius*.35,radius*.025,alpha*.6F);
        }
    }
    public static void effect(MatrixStack stack,IRenderTypeBuffer buffers,int kind,float radius,float age,float progress,float alpha,int seed) {
        IVertexBuilder out=buffers.getBuffer(GLOW);Matrix4f m=stack.last().pose();
        boolean burst=kind==GoroVfxEntity.BURST || kind==GoroVfxEntity.IMPACT;
        if(burst) {
            double r=radius*(.15+.85*Math.sqrt(Math.min(1,progress*2)));
            for(int i=0;i<3;i++)ring(out,m,r*(1-i*.13),.12+i*.09,radius*.006,alpha*(1-i*.2F),age);
            for(int i=0;i<24;i++) {
                Vector3d end=sphere(i*2.39996,.25+(i%8)*.28).scale(r);
                if(kind==GoroVfxEntity.IMPACT)end=new Vector3d(end.x,Math.abs(end.y)*.5,end.z);
                arc(out,m,Vector3d.ZERO,end,seed+i*83,age,radius*.18,radius*.013,alpha);
            }
        } else if(kind==GoroVfxEntity.AIM) {
            ring(out,m,radius,.15,.025,.7F,age);ring(out,m,radius*.75,.18,.012,.45F,age);
            for(int i=0;i<8;i++) {
                double a=i*Math.PI/4+age*.015;
                Vector3d start=new Vector3d(Math.cos(a)*radius,.2,Math.sin(a)*radius);
                arc(out,m,start,start.scale(.85).add(0,1.5,0),seed+i,age,.25,.012,.8F);
            }
        } else {
            boolean amaru=kind==GoroVfxEntity.AMARU;
            int count=amaru?16:kind==GoroVfxEntity.CAGE?12:8;
            for(int n=0;n<count;n++) {
                double lon=n*2.39996+age*.013,lat=.3+(n%7)*.4;
                Vector3d start=sphere(lon,lat).scale(radius),end=sphere(lon+.7,lat+.55).scale(radius);
                if(amaru){start=start.multiply(.65,1.1,.65);end=end.multiply(.65,1.1,.65);}
                arc(out,m,start,end,seed+n*41,age,radius*.17,radius*.012,alpha);
            }
        }
    }
}
