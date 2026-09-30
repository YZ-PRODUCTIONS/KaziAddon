package net.kazi.kazimod.renderers.entities;

/** Deterministic projection meshes: no particle entities or per-frame random state. Local +Z is forward. */
public final class ReplicatedSwordGeometry {
    public interface Sink { void vertex(double x, double y, double z, float r, float g, float b, float a); }
    private static final double TAU = Math.PI * 2;
    private static final float[] CYAN = {0.12F, 0.64F, 1};
    private static final float[] ICE = {0.65F, 0.93F, 1};
    private static final float[] BLUE = {0.05F, 0.28F, 0.9F};
    private static final float[] GRIP = {0.10F, 0.20F, 0.33F};
    private static final float[] SILVER = {0.53F, 0.76F, 0.89F};
    private static final double[][][] CIRCLES = new double[3][][];
    private static final int[][] BOX_FACES = {{0,1,2,3},{4,7,6,5},{0,4,5,1},{3,2,6,7},{0,3,7,4},{1,5,6,2}};
    static {
        for (int detail = 0; detail < 3; detail++) {
            int sides = count(detail, 32, 24, 12);
            CIRCLES[detail] = new double[sides + 1][2];
            for (int i = 0; i <= sides; i++) {
                CIRCLES[detail][i][0] = Math.cos(i * TAU / sides);
                CIRCLES[detail][i][1] = Math.sin(i * TAU / sides);
            }
        }
    }
    private ReplicatedSwordGeometry() { }
    private static int count(int detail, int high, int medium, int low) { return detail == 0 ? high : detail == 1 ? medium : low; }
    private static double clamp(double t) { return Math.max(0, Math.min(1, t)); }
    private static double smooth(double t) { t = clamp(t); return t * t * (3 - 2 * t); }
    public static float formation(float age) { return (float)smooth((age - 2) / 9.0); }

    /** Recessed veil, counter-rotating seals and crystalline rune marks. */
    public static void portal(float age, int seed, int detail, Sink out) {
        if (age <= 0) return;
        double open = 1 - Math.pow(1 - clamp(age / 9.0), 3);
        double phase = Math.floorMod(seed, 97) * 0.371;
        float fade = (float)smooth(age / 5.0);
        float breath = (float)(0.82 + 0.12 * Math.sin(age * 0.07 + phase));
        double rotation = age * 0.012 + phase;
        // A dim veil leaves the metallic silhouette legible through overlapping seals.
        disc(out, detail, 0.92 * open, -0.50, BLUE, fade * 0.13F);
        ring(out, detail, 1.04 * open, 0.023 * open, -0.46, rotation, true, CYAN, fade * breath);
        ring(out, detail, 0.78 * open, 0.016 * open, -0.42, -rotation * 0.7, false, ICE, fade * 0.65F);
        if (detail < 2) ring(out, detail, 0.57 * open, 0.013 * open, -0.38, rotation, true, CYAN, fade * 0.46F);
        int marks = count(detail, 8, 6, 4);
        for (int i = 0; i < marks; i++) {
            double a = i * TAU / marks - rotation * 0.7;
            double c = Math.cos(a), s = Math.sin(a), r = 0.90 * open;
            diamond(out, c*r, s*r, -0.40, 0.033 * open, 0.095 * open, a, ICE, fade * 0.75F);
            line(out, c*0.61*open,s*0.61*open,-0.395,c*0.69*open,s*0.69*open,-0.395,
                    0.012*open,CYAN,fade*0.6F,fade*0.6F);
        }
        // Sparse glints feed into the blade with no hard, synchronized flashing.
        for (int i = 0; i < count(detail, 6, 3, 0); i++) {
            double t = (age * 0.023 + i * 0.618 + phase) % 1;
            double a = i * 2.4 + phase, r = (1 - t) * 0.65 * open;
            float alpha = fade * (float)Math.sin(t * Math.PI) * 0.5F;
            diamond(out, Math.cos(a)*r, Math.sin(a)*r, -0.30+t*0.5,
                    0.018,0.055,a,ICE,alpha);
        }
        float formed = formation(age);
        line(out,0,0,-0.25,0,0,1.75*formed-0.25,0.027,ICE,formed*0.26F,formed*0.5F);
        // A single reconstruction scan traverses the blade, then fades completely.
        if (formed > 0 && formed < 1) ring(out, detail, 0.18, 0.025,
                -0.4+2.2*formed, 0, false, ICE, (float)Math.sin(formed*Math.PI)*0.7F);
    }

    /** Faceted steel, swept quillons and three subtle blade silhouettes across the volley. */
    public static void sword(float formed, int seed, Sink out) {
        if (formed <= 0) return;
        int variant = Math.floorMod(seed, 3);
        double length = 1.65 + variant * 0.12;
        for (int side = 0; side < 4; side++) {
            float shade = side == 0 ? 0.94F : side == 1 ? 0.72F : side == 2 ? 0.43F : 0.61F;
            for (int i = 0; i < 3; i++) {
                double za = bladeZ(i,length), zb = bladeZ(i+1,length);
                double wa = bladeWidth(i,variant), wb = bladeWidth(i+1,variant);
                bladeVertex(out,side,wa,za,formed,shade);
                bladeVertex(out,(side+1)%4,wa,za,formed,shade);
                bladeVertex(out,(side+1)%4,wb,zb,formed,shade);
                bladeVertex(out,side,wb,zb,formed,shade);
            }
        }
        double reveal = formed;
        box(out,-0.046,-0.045,-0.40*reveal,0.046,0.045,-0.02*reveal,GRIP);
        for (int side = -1; side <= 1; side += 2) {
            double x = side * (0.29 + variant * 0.025) * reveal;
            line(out,0,0,-0.04*reveal,x,0,0.09*reveal,0.042*reveal,SILVER,1,1);
            line(out,x,0,0.09*reveal,x+side*0.065*reveal,0,0.18*reveal,0.022*reveal,SILVER,1,1);
        }
        box(out,-0.066*reveal,-0.060*reveal,-0.49*reveal,0.066*reveal,0.060*reveal,-0.40*reveal,SILVER);
        if (formed > 0.3F) {
            line(out,0,0.062*reveal,0.11*reveal,0,0.021*reveal,(length-0.25)*reveal,0.009*reveal,CYAN,0.8F,0.8F);
        }
    }

    /** Crossed tapered ribbons remain visible in third person without a cloud of particle entities. */
    public static void flight(float age, int seed, int detail, Sink out) {
        if (age < 0) return;
        double length = 1.2 + 4.5 * smooth(age / 4.0);
        line(out,0,0,-length,0,0,1.55,0.14,CYAN,0,0.34F);
        line(out,0,0,-length*0.82,0,0,1.72,0.032,ICE,0,0.85F);
        double phase = Math.floorMod(seed,97) * 0.371;
        for (int i = 0; i < count(detail,6,3,0); i++) {
            double t = (age*0.06+i*0.618) % 1;
            double a = phase+i*2.4, radius = 0.16+t*0.22;
            double x=Math.cos(a)*radius,y=Math.sin(a)*radius,z=-t*length;
            line(out,x,y,z-0.42,x,y,z,0.012,ICE,0,(float)(1-t)*0.55F);
        }
        if (age < 6) {
            float fade = (float)(Math.sin(Math.PI * clamp(age / 6.0)) * 0.65);
            ring(out,detail,0.28+age*0.13,0.027,-0.48-age*0.16,phase,true,CYAN,fade);
        }
    }

    private static double bladeZ(int i,double length) { return i==0?0:i==1?0.2:i==2?length*0.76:length; }
    private static double bladeWidth(int i,int variant) { return i==0?0.12:i==1?0.145+variant*0.018:i==2?0.09:0; }
    private static void bladeVertex(Sink out,int side,double width,double z,float formed,float shade) {
        double x=side==0?-width:side==2?width:0;
        double y=side==1?width*0.42:side==3?-width*0.42:0;
        out.vertex(x*formed,y*formed,-0.44+(z+0.44)*formed,shade*0.84F,shade*0.95F,shade,1);
    }
    private static void ring(Sink out,int detail,double radius,double width,double z,double rotation,
                              boolean broken,float[] color,float alpha) {
        if (alpha <= 0.001F || radius <= 0) return;
        double[][] circle=CIRCLES[detail];int sides=circle.length-1;
        double c=Math.cos(rotation),s=Math.sin(rotation);
        for(int i=0;i<sides;i++) {
            if(broken && i%4==3)continue;
            double ax=circle[i][0]*c-circle[i][1]*s,ay=circle[i][0]*s+circle[i][1]*c;
            double bx=circle[i+1][0]*c-circle[i+1][1]*s,by=circle[i+1][0]*s+circle[i+1][1]*c;
            for(int band=0;band<2;band++) {
                double inner=radius+(band==0?-width*2:0),outer=radius+(band==0?0:width*2);
                float a=band==0?0:alpha,b=band==0?alpha:0;
                vertex(out,ax*inner,ay*inner,z,color,a);vertex(out,bx*inner,by*inner,z,color,a);
                vertex(out,bx*outer,by*outer,z,color,b);vertex(out,ax*outer,ay*outer,z,color,b);
            }
        }
    }
    private static void disc(Sink out,int detail,double radius,double z,float[] color,float alpha) {
        double[][] circle=CIRCLES[detail];
        for(int i=0;i<circle.length-1;i+=2) {
            vertex(out,0,0,z,color,alpha);
            vertex(out,circle[i][0]*radius,circle[i][1]*radius,z,color,0);
            vertex(out,circle[i+2][0]*radius,circle[i+2][1]*radius,z,color,0);
            vertex(out,0,0,z,color,alpha);
        }
    }
    private static void diamond(Sink out,double x,double y,double z,double width,double height,
                                  double angle,float[] color,float alpha) {
        double c=Math.cos(angle),s=Math.sin(angle);
        vertex(out,x-c*height,y-s*height,z,color,alpha);
        vertex(out,x-s*width,y+c*width,z,color,alpha);
        vertex(out,x+c*height,y+s*height,z,color,alpha);
        vertex(out,x+s*width,y-c*width,z,color,alpha);
    }
    private static void line(Sink out,double x1,double y1,double z1,double x2,double y2,double z2,
                               double width,float[] color,float a1,float a2) {
        double dx=x2-x1,dy=y2-y1,dz=z2-z1,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(len<0.00001 || width<=0)return;
        dx/=len;dy/=len;dz/=len;
        double sx=Math.abs(dy)>0.9?0:dz,sy=Math.abs(dy)>0.9?dz:0,sz=Math.abs(dy)>0.9?-dy:-dx;
        double sl=Math.sqrt(sx*sx+sy*sy+sz*sz);sx=sx/sl*width;sy=sy/sl*width;sz=sz/sl*width;
        for(int i=0;i<2;i++) {
            vertex(out,x1+sx,y1+sy,z1+sz,color,a1);vertex(out,x1-sx,y1-sy,z1-sz,color,a1);
            vertex(out,x2-sx,y2-sy,z2-sz,color,a2);vertex(out,x2+sx,y2+sy,z2+sz,color,a2);
            double tx=dy*sz-dz*sy,ty=dz*sx-dx*sz,tz=dx*sy-dy*sx;sx=tx;sy=ty;sz=tz;
        }
    }
    private static void box(Sink out,double x1,double y1,double z1,double x2,double y2,double z2,float[] color) {
        for(int[] face:BOX_FACES)for(int i:face) {
            double x=i==0||i==3||i==4||i==7?x1:x2;
            double y=i==0||i==1||i==4||i==5?y1:y2;
            vertex(out,x,y,i<4?z1:z2,color,1);
        }
    }
    private static void vertex(Sink out,double x,double y,double z,float[] color,float alpha) {
        out.vertex(x,y,z,color[0],color[1],color[2],alpha);
    }
}
