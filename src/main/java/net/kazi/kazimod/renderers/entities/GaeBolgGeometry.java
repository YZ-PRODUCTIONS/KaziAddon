package net.kazi.kazimod.renderers.entities;

/** Procedural crimson spear meshes; pure math so the exact animation can be previewed offline. */
public final class GaeBolgGeometry {
    public interface Sink { void vertex(double x, double y, double z, float r, float g, float b, float a); }
    private static final double TAU = Math.PI * 2;
    private static final float[] RED = {1, 0.008F, 0.075F};
    private static final float[] PINK = {1, 0.08F, 0.28F};
    private static final float[] HOT = {1, 0.80F, 0.83F};
    private static final double[][] BURST = new double[24][3];
    static {
        java.util.Random random = new java.util.Random(0x6AE80L);
        for (int i = 0; i < BURST.length; i++) {
            double angle = i * TAU / BURST.length + random.nextDouble() * 0.2;
            double radius = 8 + random.nextDouble() * 9;
            BURST[i] = p(Math.cos(angle) * radius, 1.5 + random.nextDouble() * 12, Math.sin(angle) * radius);
        }
    }
    private GaeBolgGeometry() { }
    private static int count(int detail, int high, int medium, int low) { return detail == 0 ? high : detail == 1 ? medium : low; }
    private static double clamp(double t) { return Math.max(0, Math.min(1, t)); }
    private static double smooth(double t) { t = clamp(t); return t * t * (3 - 2 * t); }

    /** Local +Z is the spear's forward axis. */
    public static void charge(float age, int detail, Sink out) {
        float power = (float)clamp(age / 100.0);
        float reveal = (float)smooth(age / 7.0);
        spear(out, reveal, 0.75F + power * 0.25F);
        int segments = count(detail, 28, 18, 12);
        // Three open, tapered bands coil tightly around the spear as it finishes charging.
        for (int strand = 0; strand < count(detail, 3, 2, 1); strand++) {
            double[] prev = null;
            for (int i = 0; i <= segments; i++) {
                double t = i / (double)segments;
                double angle = strand * TAU / 3 + t * TAU * 1.15 - age * 0.22;
                double radius = (0.25 + Math.sin(t * Math.PI) * (1.25 - power * 0.6));
                double[] next = p(Math.cos(angle) * radius, Math.sin(angle) * radius, -3.2 + t * 5.3);
                if (prev != null) beam(out, prev, next, 0.055 + power * 0.075, RED,
                        reveal * (float)Math.sin(t * Math.PI) * 0.55F);
                prev = next;
            }
        }
        // Inward racing sparks, with bounded counts independent of frame rate.
        for (int i = 0; i < count(detail, 12, 8, 4); i++) {
            double t = ((age * 0.035 + i * 0.618) % 1);
            double angle = i * 2.4;
            double radius = 2.4 * (1 - t);
            double[] head = p(Math.cos(angle) * radius, Math.sin(angle) * radius, -1 + t * 2);
            double[] tail = p(head[0] * 1.15, head[1] * 1.15, head[2] - 0.22);
            beam(out, tail, head, 0.025, PINK, reveal * (float)Math.sin(t * Math.PI));
        }
        float pulse = (0.72F + 0.28F * (float)Math.sin(age * 0.7)) * power;
        spindle(out, p(0,0,0.7), p(0,0,2.9), 0.24 + power * 0.14, RED, pulse * 0.25F, 8);
    }

    public static void flight(float age, int detail, Sink out) {
        spear(out, 1, 1);
        double length = 4 + 14 * smooth(age / 7.0);
        spindle(out, p(0,0,-length), p(0,0,2.4), 0.42, RED, 0.34F, 8);
        beam(out, p(0,0,-length * 0.85), p(0,0,1.7), 0.055, HOT, 0.85F);
        int segments = count(detail, 22, 14, 9);
        for (int strand = 0; strand < count(detail, 4, 3, 2); strand++) {
            double[] previous = null;
            for (int i = 0; i <= segments; i++) {
                double t = i / (double)segments;
                double angle = strand * TAU / 4 + t * TAU * 1.6 + age * 0.21;
                double radius = (0.16 + (1 - t) * 1.15) * Math.sin(t * Math.PI);
                double[] point = p(Math.cos(angle) * radius, Math.sin(angle) * radius, -length + t * (length + 1));
                if (previous != null) beam(out, previous, point, 0.06 + t * 0.09, strand == 0 ? PINK : RED,
                        (float)(Math.sin(t * Math.PI) * 0.64));
                previous = point;
            }
        }
        // Compressed pressure crescents travel down the shaft instead of random lightning.
        for (int i = 0; i < count(detail, 3, 2, 1); i++) {
            double phase = (age / 8 + i / 3.0) % 1;
            ring(out, 0.35 + phase * 2, -phase * length, 0.12, PINK,
                    (float)((1 - phase) * 0.28), true, count(detail, 32, 24, 16));
        }
    }

    public static void arrival(float age, int detail, Sink out) {
        float fade = (float)(1 - smooth(age / 10.0));
        beam(out, p(0,0,-18), p(0,0,0.8), 0.34 * fade, RED, fade * 0.42F);
        beam(out, p(0,0,-17), p(0,0,0.5), 0.06 * fade, HOT, fade * 0.7F);
    }

    public static void impact(float age, int detail, Sink out) {
        if (age < 0 || age >= 42) return;
        // Violent opening impulse: most of the silhouette deploys in the first 3 ticks.
        // One local flash, not a repeating or full-screen strobe.
        double expansion = 1 - Math.pow(1 - clamp(age / 4.5), 4);
        float fade = (float)(1 - smooth((age - 8) / 34));
        float coreFade = (float)(1 - smooth((age - 4) / 18));
        float flash = (float)(smooth(age / 0.8) * (1 - smooth((age - 1.5) / 6.5)));
        eruptionCore(out, expansion, age, coreFade, detail);
        spindle(out, p(0,-0.2,0), p(0,13 * expansion,0), 4.1 * expansion, HOT, flash * 0.80F, 8);

        // Broad, torn pressure fronts punch outward quickly, each with a separate fading wake.
        for (int i = 0; i < count(detail, 4, 3, 2); i++) {
            double t = (age - i * 2.5) / 20;
            if (t <= 0 || t >= 1) continue;
            double radius = 0.6 + 20.2 * (1 - Math.pow(1 - t, 3));
            float opacity = (float)(smooth(t * 9) * (1 - smooth(t)));
            shockFront(out, radius, 0.22 + i * 0.35, 0.3 + (1 - t) * 1.4,
                    i, opacity * 0.78F, count(detail, 48, 32, 20));
            if (i == 0) ring(out, radius, 0.24, 0.12, HOT, opacity * 0.5F,
                    false, count(detail, 48, 32, 20));
        }

        // Wide jagged lobes tear away from the core, rather than staying rooted like crystals.
        int rays = count(detail, 24, 16, 10);
        for (int i = 0; i < rays; i++) {
            double[] vector = BURST[i * BURST.length / rays];
            double localAge = Math.max(0, age - (i % 4) * 0.45);
            double grow = (1 - Math.pow(1 - clamp(localAge / (4.5 + i % 3)), 3))
                    * (0.86 + 0.45 * clamp(localAge / 34));
            double[] end = p(vector[0] * grow, vector[1] * grow + expansion * (i % 3), vector[2] * grow);
            double detach = 0.03 + 0.77 * smooth((localAge - 4) / 31);
            double width = (0.95 + (i % 4) * 0.37) * expansion
                    * (0.15 + 0.85 * (1 - smooth((localAge - 5) / 30)));
            spindle(out, mul(end,detach), end, width, i % 4 == 0 ? PINK : RED, fade * 0.69F, 4);
            if (i % 3 == 0) spindle(out,mul(end,detach + 0.03),mul(end,0.93),width * 0.16,HOT,
                    fade * (0.3F + flash * 0.5F),4);
        }

        // Two deterministic waves of fast tracer fragments. Pure mesh: no particle/entity storm.
        for (int i = 0; i < count(detail, 36, 24, 12); i++) {
            double t = clamp((age - (i % 2) * 3) / 38);
            double[] v = BURST[(i * 7) % BURST.length];
            double travel = 1 - Math.pow(1 - t, 2);
            double[] head = p(v[0] * (0.08 + travel * 1.35),
                    v[1] * travel * 1.55 + 0.3 - t*t*7, v[2] * (0.08 + travel * 1.35));
            double tailLength = 0.06 + 0.16 * (1 - t);
            double[] tail = p(head[0]-v[0]*tailLength,head[1]-v[1]*tailLength,head[2]-v[2]*tailLength);
            float sparkFade = (float)(smooth(t * 12) * (1 - smooth(t)));
            beam(out, tail, head, 0.065 * (1 - t), i % 3 == 0 ? HOT : PINK, sparkFade);
        }
    }

    private static void eruptionCore(Sink out,double expansion,float age,float fade,int detail) {
        if (fade <= 0.001F || expansion <= 0) return;
        // A lopsided, serrated volume fills the space between the outward jets.
        int sides=count(detail,24,16,10);
        for(int band=0;band<3;band++) for(int i=0;i<sides;i++) {
            double a=i*TAU/sides,b=(i+1)*TAU/sides;
            double[] p0=corePoint(a,band,expansion,age),p1=corePoint(b,band,expansion,age);
            double[] p2=corePoint(b,band+1,expansion,age),p3=corePoint(a,band+1,expansion,age);
            float opacity=fade*(band==0?0.48F:band==1?0.36F:0.2F);
            vertex(out,p0,band==0?PINK:RED,opacity);vertex(out,p1,band==0?PINK:RED,opacity);
            vertex(out,p2,RED,opacity*0.6F);vertex(out,p3,RED,opacity*0.6F);
        }
    }
    private static double[] corePoint(double angle,int band,double expansion,float age) {
        double radius=band==0?2.7:band==1?7.2:band==2?4.6:0.3;
        double height=band==0?0.2:band==1?2.8:band==2?6.3:13;
        double irregular=1+0.2*Math.sin(angle*5+0.6)+0.13*Math.cos(angle*9-1.1);
        double lift=clamp((age-4)/18);
        radius*=expansion*irregular*(1+lift*0.25);
        height=height*expansion*(1+0.19*Math.cos(angle*7)+0.1*Math.sin(angle*3)) + lift*band*1.1;
        return p(Math.cos(angle)*radius+lift*1.1,height,Math.sin(angle)*radius-lift*0.7);
    }
    private static void shockFront(Sink out,double radius,double height,double width,int wave,float alpha,int sides) {
        for(int i=0;i<sides;i++) {
            // Stable gaps and teeth move with the front, not random noise every render frame.
            if((i+wave*3)%11==0) continue;
            double a=i*TAU/sides,b=(i+1)*TAU/sides;
            double ra=radius*(1+0.045*Math.sin(a*9+wave)),rb=radius*(1+0.045*Math.sin(b*9+wave));
            double ha=height+Math.abs(Math.sin(a*7+wave))*0.45,hb=height+Math.abs(Math.sin(b*7+wave))*0.45;
            vertex(out,p(Math.cos(a)*(ra-width),ha,Math.sin(a)*(ra-width)),RED,alpha*0.2F);
            vertex(out,p(Math.cos(b)*(rb-width),hb,Math.sin(b)*(rb-width)),RED,alpha*0.2F);
            vertex(out,p(Math.cos(b)*rb,hb,Math.sin(b)*rb),PINK,alpha);
            vertex(out,p(Math.cos(a)*ra,ha,Math.sin(a)*ra),PINK,alpha);
            vertex(out,p(Math.cos(a)*ra,ha,Math.sin(a)*ra),PINK,alpha);
            vertex(out,p(Math.cos(b)*rb,hb,Math.sin(b)*rb),PINK,alpha);
            vertex(out,p(Math.cos(b)*(rb+width*0.25),hb,Math.sin(b)*(rb+width*0.25)),RED,0);
            vertex(out,p(Math.cos(a)*(ra+width*0.25),ha,Math.sin(a)*(ra+width*0.25)),RED,0);
        }
    }

    private static void spear(Sink out, float alpha, float power) {
        beam(out, p(0,0,-3.7), p(0,0,1.3), 0.065, RED, alpha);
        beam(out, p(0,0,-3.65), p(0,0,1.2), 0.016, HOT, alpha * 0.75F);
        spindle(out, p(0,0,0.55), p(0,0,2.5), 0.24, RED, alpha, 6);
        spindle(out, p(0,0,1.05), p(0,0,2.6), 0.095, HOT, alpha * power, 4);
        // Hooked blades and collar prongs give the projectile a recognizable physical spear silhouette.
        for (int i = 0; i < 3; i++) {
            double angle = i * TAU / 3;
            double[] end = p(Math.cos(angle)*0.50, Math.sin(angle)*0.50,0.25);
            spindle(out,p(0,0,1.05),end,0.085,RED,alpha,4);
            spindle(out,end,p(end[0]*0.7,end[1]*0.7,0.75),0.07,PINK,alpha*0.85F,4);
        }
    }

    private static void ring(Sink out, double radius, double offset, double width, float[] color,
                             float alpha, boolean alongZ, int segments) {
        for (int i=0;i<segments;i++) {
            double a=i*TAU/segments, b=(i+1)*TAU/segments;
            double[] p0=ringPoint(a,radius-width,offset,alongZ), p1=ringPoint(b,radius-width,offset,alongZ);
            double[] p2=ringPoint(b,radius+width,offset,alongZ), p3=ringPoint(a,radius+width,offset,alongZ);
            vertex(out,p0,color,alpha); vertex(out,p1,color,alpha); vertex(out,p2,color,0); vertex(out,p3,color,0);
        }
    }
    private static double[] ringPoint(double a,double r,double h,boolean z) {
        return z ? p(Math.cos(a)*r,Math.sin(a)*r,h) : p(Math.cos(a)*r,h,Math.sin(a)*r);
    }
    private static void spindle(Sink out,double[] from,double[] to,double radius,float[] color,float alpha,int sides) {
        if (radius<=0 || alpha<=0.001) return;
        double[][] basis=basis(from,to);
        double[] middle=p(from[0]*0.65+to[0]*0.35,from[1]*0.65+to[1]*0.35,from[2]*0.65+to[2]*0.35);
        for(int i=0;i<sides;i++) {
            double a=i*TAU/sides,b=(i+1)*TAU/sides;
            double[] p0=offset(middle,basis,radius,a),p1=offset(middle,basis,radius,b);
            quad(out,from,p0,p1,from,color,alpha); quad(out,p0,to,to,p1,color,alpha);
        }
    }
    private static void beam(Sink out,double[] from,double[] to,double width,float[] color,float alpha) {
        if(width<=0 || alpha<=0.001) return;
        double[][] basis=basis(from,to);
        for(double[] axis:basis) {
            double[] side=mul(axis,width);
            quad(out,add(from,side),add(from,mul(side,-1)),add(to,mul(side,-1)),add(to,side),color,alpha);
        }
    }
    private static double[][] basis(double[] a,double[] b) {
        double[] f=p(b[0]-a[0],b[1]-a[1],b[2]-a[2]);
        double n=Math.sqrt(f[0]*f[0]+f[1]*f[1]+f[2]*f[2]);
        if(n<1e-8) return new double[][]{p(1,0,0),p(0,1,0)};
        f=mul(f,1/n);
        double[] s=Math.abs(f[1])>0.9?p(0,f[2],-f[1]):p(-f[2],0,f[0]);
        s=mul(s,1/Math.sqrt(s[0]*s[0]+s[1]*s[1]+s[2]*s[2]));
        return new double[][]{s,p(f[1]*s[2]-f[2]*s[1],f[2]*s[0]-f[0]*s[2],f[0]*s[1]-f[1]*s[0])};
    }
    private static double[] offset(double[] p,double[][] b,double r,double a) { return add(p,add(mul(b[0],Math.cos(a)*r),mul(b[1],Math.sin(a)*r))); }
    private static double[] p(double x,double y,double z) { return new double[]{x,y,z}; }
    private static double[] mul(double[] p,double n) { return p(p[0]*n,p[1]*n,p[2]*n); }
    private static double[] add(double[] a,double[] b) { return p(a[0]+b[0],a[1]+b[1],a[2]+b[2]); }
    private static void quad(Sink out,double[] a,double[] b,double[] c,double[] d,float[] color,float alpha) {
        vertex(out,a,color,alpha); vertex(out,b,color,alpha); vertex(out,c,color,alpha); vertex(out,d,color,alpha);
    }
    private static void vertex(Sink out,double[] p,float[] c,float a) { out.vertex(p[0],p[1],p[2],c[0],c[1],c[2],Math.max(0,Math.min(1,a))); }
}
