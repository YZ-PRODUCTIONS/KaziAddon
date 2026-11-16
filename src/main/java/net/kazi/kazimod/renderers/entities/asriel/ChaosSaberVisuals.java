package net.kazi.kazimod.renderers.entities.asriel;

import java.util.Arrays;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberPattern;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Source-shaped white sabers and ribbons sampled from their actual motion, without particle entities. */
public final class ChaosSaberVisuals {
    private static final double TAU=Math.PI*2;
    private static final double[] PROFILE_T={0,.07,.26,.44,.49,.57,.63,.75,.86,.94,1};
    private static final double[] CENTER={-.05,-.05,-.12,-.10,.25,.30,-.22,-.40,-.55,-.80,-1.02};
    private static final double[] WIDTH={.62,1.28,1.34,1.30,.96,.82,1.25,1.50,1.15,.54,0};
    private static final double[][] SAMPLES={samples(22),samples(16),samples(10)};
    private ChaosSaberVisuals() { }

    /** Dimensions are visual-only; the caller supplies the requested scaled length and radius. */
    public static void render(Sink out,float age,float visualLength,float visualRadius,int detail) {
        if(!Float.isFinite(age+visualLength+visualRadius) || age<0 || age>=ChaosSaberPattern.DURATION
                || visualLength<=0 || visualRadius<0)return;
        detail=Math.max(0,Math.min(2,detail));
        double unit=visualLength/22.0;
        float alpha=ChaosSaberMotion.bladeAlpha(age);
        for(int side=-1;side<=1;side+=2) {
            trail(out,age,side,visualLength,detail);
            if(alpha<=0)continue;
            Sink hand=pose(out,ChaosSaberMotion.handX(age,side)*unit,ChaosSaberMotion.handY(age)*unit,
                    ChaosSaberMotion.handZ(age)*unit,ChaosSaberMotion.yaw(age,side),ChaosSaberMotion.pitch(age,side));
            final int mirror=side;
            Sink mirrored=(x,y,z,r,g,b,a)->hand.vertex(x*mirror,y,z,r,g,b,a);
            blade(mirrored,visualLength,alpha,detail);
            hilt(mirrored,unit,alpha,detail);
        }
        sparkles(out,age,visualLength,visualRadius,detail);
    }

    /** Smooth connected blade: shallow diamond bevel, curved waist, broad shoulder and integrated eye. */
    private static void blade(Sink out,double length,float opacity,int detail) {
        double unit=length/22.0,base=3.45*unit,span=length*.80-base;
        double[] ts=SAMPLES[detail];
        for(int i=0;i<ts.length-1;i++) {
            double ta=ts[i],tb=ts[i+1],ca=profile(CENTER,ta)*unit,cb=profile(CENTER,tb)*unit;
            double wa=profile(WIDTH,ta)*unit,wb=profile(WIDTH,tb)*unit;
            double za=base+span*ta,zb=base+span*tb;
            double ha=eye(ta)*unit,hb=eye(tb)*unit;
            double da=.11*unit*Math.min(1,wa/(.55*unit)),db=.11*unit*Math.min(1,wb/(.55*unit));
            for(int face=-1;face<=1;face+=2) {
                float faceAlpha=opacity*(face==1?.84F:.55F);
                quad(out,p(ca-wa,0,za),p(cb-wb,0,zb),p(cb-hb,face*db,zb),p(ca-ha,face*da,za),faceAlpha);
                quad(out,p(ca+ha,face*da,za),p(cb+hb,face*db,zb),p(cb+wb,0,zb),p(ca+wa,0,za),faceAlpha);
            }
            if(ha>0 || hb>0) {
                quad(out,p(ca-ha,da,za),p(cb-hb,db,zb),p(cb-hb,-db,zb),p(ca-ha,-da,za),opacity*.62F);
                quad(out,p(ca+ha,-da,za),p(cb+hb,-db,zb),p(cb+hb,db,zb),p(ca+ha,da,za),opacity*.62F);
            }
        }
    }

    /** Curled crossguard and a bowed grip follow the source silhouette rather than a rectangular cross. */
    private static void hilt(Sink out,double unit,float opacity,int detail) {
        int turns=detail==0?8:detail==1?6:4;
        for(int side=-1;side<=1;side+=2) {
            double[] a={side*.18,.35},b={side*1.95,-.1},c={side*3.35,1.15},d={side*2.27,2.25};
            double[] e={side*3.12,1.82},f={side*3.20,-.25},g={side*2.35,-.70};
            curve(out,a,b,c,d,unit,.25,.13,opacity,turns/2);
            curve(out,d,e,f,g,unit,.13,.035,opacity,turns/2);
        }
        curve(out,new double[]{0,3.47},new double[]{-.18,2.7},new double[]{-.12,1.95},new double[]{0,1.4},
                unit,.58,.36,opacity,detail==0?4:2);
        // A small central guard loop and curved grip remain substantial at the reduced visual scale.
        curve(out,new double[]{0,1.45},new double[]{1.06,.96},new double[]{1.06,.28},new double[]{0,-.12},
                unit,.16,.16,opacity,detail==0?4:2);
        curve(out,new double[]{0,-.12},new double[]{-1.06,.28},new double[]{-1.06,.96},new double[]{0,1.45},
                unit,.16,.16,opacity,detail==0?4:2);
        curve(out,new double[]{0,-.1},new double[]{.42,-1.13},new double[]{.08,-2.23},new double[]{-.16,-3.05},
                unit,.27,.18,opacity,detail==0?6:4);
        // Small teardrop pommel, kept rounded and connected to the grip.
        int n=detail==0?8:detail==1?6:4;
        for(int i=0;i<n;i++) {
            double a=TAU*i/n,b=TAU*(i+1)/n;
            quad(out,p(-.16*unit,0,-3.22*unit),p((-.16+Math.sin(a)*.30)*unit,0,(-3.22+Math.cos(a)*.43)*unit),
                    p((-.16+Math.sin(b)*.30)*unit,0,(-3.22+Math.cos(b)*.43)*unit),p(-.16*unit,0,-3.22*unit),opacity*.9F);
        }
    }

    private static void curve(Sink out,double[] a,double[] b,double[] c,double[] d,double unit,
                              double startWidth,double endWidth,float opacity,int segments) {
        for(int i=0;i<segments;i++) {
            double t0=i/(double)segments,t1=(i+1)/(double)segments;
            double[] p0=bezier(a,b,c,d,t0),p1=bezier(a,b,c,d,t1);
            double dx=p1[0]-p0[0],dz=p1[1]-p0[1],len=Math.max(.0001,Math.sqrt(dx*dx+dz*dz));
            double nx=-dz/len,nz=dx/len,w0=startWidth+(endWidth-startWidth)*t0,w1=startWidth+(endWidth-startWidth)*t1;
            for(int face=-1;face<=1;face+=2)
                quad(out,p((p0[0]+nx*w0)*unit,face*w0*.35*unit,(p0[1]+nz*w0)*unit),
                        p((p1[0]+nx*w1)*unit,face*w1*.35*unit,(p1[1]+nz*w1)*unit),
                        p((p1[0]-nx*w1)*unit,face*w1*.35*unit,(p1[1]-nz*w1)*unit),
                        p((p0[0]-nx*w0)*unit,face*w0*.35*unit,(p0[1]-nz*w0)*unit),opacity*(face==1?.85F:.55F));
        }
    }

    /** Feathered ribbon connecting fractional historical blade positions; it has no independent pose. */
    private static void trail(Sink out,float age,int side,double length,int detail) {
        float intensity=ChaosSaberMotion.trailAlpha(age,side);
        if(intensity<=0)return;
        int segments=detail==0?24:detail==1?18:12;
        double history=1.7;
        for(int i=0;i<segments;i++) {
            double u0=i/(double)segments,u1=(i+1)/(double)segments;
            float a0=age-(float)(history*u0),a1=age-(float)(history*u1);
            if(a1<0)continue;
            double yaw0=ChaosSaberMotion.yaw(a0,side),yaw1=ChaosSaberMotion.yaw(a1,side);
            // Only the inward horizontal sweep leaves a trail, never the drop or outward recovery.
            if(side*(yaw0-yaw1)>-.0002)continue;
            // A history interval can straddle the first recovery frame while still having net inward yaw.
            if(Math.abs(ChaosSaberMotion.pitch(a0,side))>1e-9 || Math.abs(ChaosSaberMotion.pitch(a1,side))>1e-9)continue;
            float f0=(float)(Math.pow(1-u0,1.7)*intensity),f1=(float)(Math.pow(1-u1,1.7)*intensity);
            double[] inner0=trailPoint(a0,side,length,.42),inner1=trailPoint(a1,side,length,.42);
            double[] middle0=trailPoint(a0,side,length,.82),middle1=trailPoint(a1,side,length,.82);
            double[] outer0=trailPoint(a0,side,length,1.015),outer1=trailPoint(a1,side,length,1.015);
            gradient(out,inner0,inner1,middle1,middle0,0,0,f1*.42F,f0*.42F);
            gradient(out,middle0,middle1,outer1,outer0,f0*.42F,f1*.42F,0,0);
        }
    }

    private static double[] trailPoint(float age,int side,double length,double along) {
        double unit=length/22.0,x=side*profile(CENTER,Math.min(1,along))*unit;
        double z=3.45*unit+(length*.80-3.45*unit)*along;
        double pitch=ChaosSaberMotion.pitch(age,side),yaw=ChaosSaberMotion.yaw(age,side);
        double py=-z*Math.sin(pitch),pz=z*Math.cos(pitch);
        return p(ChaosSaberMotion.handX(age,side)*unit+x*Math.cos(yaw)+pz*Math.sin(yaw),
                ChaosSaberMotion.handY(age)*unit+py,ChaosSaberMotion.handZ(age)*unit+pz*Math.cos(yaw)-x*Math.sin(yaw));
    }

    private static void sparkles(Sink out,float age,double length,double radius,int detail) {
        float alpha=ChaosSaberMotion.sparkleAlpha(age);
        if(alpha<=0)return;
        double unit=length/22.0,elapsed=age-ChaosSaberPattern.SPARKLE_TICK;
        int count=detail==2?2:4;
        for(int side=-1;side<=1;side+=2)for(int i=0;i<count;i++) {
            double row=(i+.5)/count,approach=Math.max(.48,1-elapsed*.009);
            double x=side*radius*(.2+row*.62)*approach,z=length*(.2+row*.72);
            double y=unit*(.15+Math.max(0,1.1-elapsed*.045));
            float pulse=.45F+.55F*(float)Math.pow(Math.sin(elapsed*.45+i*1.8),2);
            double size=unit*(.25+.07*pulse);
            for(int v=0;v<8;v++) {
                double a=TAU*v/8,b=TAU*(v+1)/8,ra=size*(v%2==0?1:.19),rb=size*(v%2==0?.19:1);
                quad(out,p(x,y,z),p(x+Math.sin(a)*ra,y+Math.cos(a)*ra,z),
                        p(x+Math.sin(b)*rb,y+Math.cos(b)*rb,z),p(x,y,z),alpha*pulse*.82F);
            }
        }
    }

    private static Sink pose(Sink out,double x,double y,double z,double yaw,double pitch) {
        double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
        return (vx,vy,vz,r,g,b,a)->{
            double py=vy*cp-vz*sp,pz=vy*sp+vz*cp;
            out.vertex(x+vx*cy+pz*sy,y+py,z+pz*cy-vx*sy,r,g,b,a);
        };
    }
    private static double eye(double t) {
        double a=(t-.86)/.035;
        return Math.abs(a)>=1?0:.38*Math.sqrt(1-a*a);
    }
    private static double profile(double[] values,double t) {
        t=Math.max(0,Math.min(1,t));int i=0;
        while(i<PROFILE_T.length-2 && t>PROFILE_T[i+1])i++;
        double width=PROFILE_T[i+1]-PROFILE_T[i],u=(t-PROFILE_T[i])/width;
        double m0=(values[i+1]-values[Math.max(0,i-1)])/(PROFILE_T[i+1]-PROFILE_T[Math.max(0,i-1)]);
        double m1=(values[Math.min(values.length-1,i+2)]-values[i])/(PROFILE_T[Math.min(values.length-1,i+2)]-PROFILE_T[i]);
        double v=(2*u*u*u-3*u*u+1)*values[i]+(u*u*u-2*u*u+u)*m0*width
                +(-2*u*u*u+3*u*u)*values[i+1]+(u*u*u-u*u)*m1*width;
        return values==WIDTH?Math.max(0,v):v;
    }
    private static double[] samples(int steps) {
        double[] values=new double[steps+6];
        for(int i=0;i<=steps;i++)values[i]=i/(double)steps;
        for(int i=0;i<5;i++)values[steps+1+i]=.825+i*.0175;
        Arrays.sort(values);return values;
    }
    private static double[] bezier(double[] a,double[] b,double[] c,double[] d,double t) {
        double s=1-t;
        return new double[]{s*s*s*a[0]+3*s*s*t*b[0]+3*s*t*t*c[0]+t*t*t*d[0],
                s*s*s*a[1]+3*s*s*t*b[1]+3*s*t*t*c[1]+t*t*t*d[1]};
    }
    private static double[] p(double x,double y,double z) { return new double[]{x,y,z}; }
    private static void quad(Sink out,double[] a,double[] b,double[] c,double[] d,float alpha) {
        gradient(out,a,b,c,d,alpha,alpha,alpha,alpha);
    }
    private static void gradient(Sink out,double[] a,double[] b,double[] c,double[] d,float aa,float ab,float ac,float ad) {
        vertex(out,a,aa);vertex(out,b,ab);vertex(out,c,ac);vertex(out,d,ad);
    }
    private static void vertex(Sink out,double[] p,float alpha) { out.vertex(p[0],p[1],p[2],1,.99F,.97F,alpha); }
}
