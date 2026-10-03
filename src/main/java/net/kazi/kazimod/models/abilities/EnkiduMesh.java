package net.kazi.kazimod.models.abilities;

/** Golden gates, interlocking metal chains and a tightening cage. World-up, feet at (0,0,0). */
public final class EnkiduMesh {
    private static final float TAU = (float) (Math.PI * 2);
    private static final int GOLD = 0xFFD14F, PALE = 0xFFF2B4, BRONZE = 0x886022;
    private static final Link[] LINKS = {new Link(10), new Link(8), new Link(6)};
    private static final ThreadLocal<Frame> FRAME = ThreadLocal.withInitial(Frame::new);
    private EnkiduMesh() { }

    @FunctionalInterface public interface Sink { void vertex(float x, float y, float z, int color, float alpha); }

    /** phase: 0 charge, 1 chains closing, 2 bound. Fade is controlled by opacity/retraction. */
    public static void target(Sink metal, Sink glow, int detail, float age, int phase, float progress,
                              float width, float height, float opacity, float retract, int seed) {
        if (!Float.isFinite(age) || !Float.isFinite(opacity) || !Float.isFinite(width)
                || !Float.isFinite(height) || opacity <= 0.001F || width <= 0 || height <= 0) return;
        int lod = Math.max(0, Math.min(2, detail));
        float alpha = clamp(opacity), closing = phase == 0 ? 0 : phase == 1 ? smooth(progress) : 1;
        closing *= 1 - smooth(retract);
        float appear = phase == 0 ? smooth(progress * 3) : 1;
        float extent = 3.2F + width * 0.5F;
        int chains = count(lod, 6, 4, 3);
        Frame frame = FRAME.get();
        for (int chain = 0; chain < chains; chain++) {
            float angle = chain * TAU / chains + (seed & 15) * .11F;
            float px = cos(angle) * extent, pz = sin(angle) * extent;
            float py = height * (chain % 2 == 0 ? .30F : .90F) + .30F;
            float tx = cos(angle + .8F) * (width * .52F + .12F);
            float tz = sin(angle + .8F) * (width * .52F + .12F);
            float ty = height * (.24F + (chain % 3) * .27F);
            frame.orient(px, py, pz, tx - px, ty - py, tz - pz);
            portal(glow, frame, lod, .64F * appear, age, alpha, chain);
            float reach = phase == 0 ? .06F + .04F * smooth(progress) : .10F + .90F * closing;
            float dx = tx - px, dy = ty - py, dz = tz - pz;
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            int links = Math.max(1, (int) (length * reach / (lod == 2 ? .34F : .25F)));
            links = Math.min(count(lod, 22, 18, 14), links);
            float slack = (1 - closing) * .34F;
            for (int link = 0; link < links; link++) {
                float u = (link + .5F) / links * reach;
                float sway = sin(u * 7 + age * .24F + chain) * sin(u * (float) Math.PI) * slack;
                float x = px + dx * u + cos(angle + 1.57F) * sway;
                float y = py + dy * u - sin(u * (float) Math.PI) * slack;
                float z = pz + dz * u + sin(angle + 1.57F) * sway;
                frame.orient(x, y, z, dx, dy, dz);
                LINKS[lod].draw(metal, frame, link % 2, alpha * appear, lod == 2 ? 1.25F : 1);
            }
            frame.orient(px + dx * reach, py + dy * reach, pz + dz * reach, dx, dy, dz);
            spear(metal, glow, frame, alpha * appear, age);
        }
        if (closing > .25F) {
            float tighten = smooth((closing - .25F) / .75F);
            float r = width * .57F + .14F + (1 - tighten) * .8F;
            for (int wrap = 0; wrap < 2; wrap++) {
                int links = Math.max(8, Math.min(count(lod, 22, 16, 12), (int) (r * TAU / .25F)));
                for (int link = 0; link < links; link++) {
                    float a = TAU * link / links + wrap * 1.8F;
                    float y = height * (.32F + wrap * .35F) + sin(a) * height * .12F;
                    frame.orient(cos(a) * r, y, sin(a) * r, -sin(a), cos(a) * .22F, cos(a));
                    LINKS[lod].draw(metal, frame, link % 2, alpha * tighten, Math.max(1, r * TAU / links / .25F));
                }
            }
            // The moment the restraints close: a brief golden burst, then sparse traveling glints.
            float burst = phase == 2 ? 1 - smooth(age / 10) : 0;
            ring(glow, 0, height * .5F, 0, r + .3F + (1 - burst) * 1.4F,
                    .07F, count(lod, 36, 24, 16), alpha * burst * .55F, age * .12F);
            for (int i = 0; i < count(lod, 14, 9, 5); i++) {
                float u = fract(i * .618F + age * .017F), a = i * 2.4F + age * .02F;
                float radius = r + .18F + u * .45F;
                star(glow, cos(a) * radius, height * u, sin(a) * radius,
                        .04F + burst * .05F, alpha * (1 - u) * .8F);
            }
        }
    }

    /** The area version's fixed telegraph remains visible throughout the cast. */
    public static void field(Sink glow, int detail, float age, float radius, float opacity, float progress) {
        if (!Float.isFinite(age) || !Float.isFinite(radius) || !Float.isFinite(opacity)
                || radius <= 0 || opacity <= .001F) return;
        int steps = count(detail, 96, 64, 40);
        float alpha = clamp(opacity) * (.3F + .4F * clamp(progress));
        ring(glow, 0, .06F, 0, radius, .055F, steps, alpha, age * .012F);
        ring(glow, 0, .07F, 0, radius - .32F, .025F, steps, alpha * .55F, -age * .015F);
        Frame frame = FRAME.get();
        for (int i = 0; i < 12; i++) {
            float angle = TAU * i / 12;
            float x = cos(angle) * radius, z = sin(angle) * radius;
            star(glow, x, .10F, z, .12F, alpha);
            // Twelve flat radial runes on the ground border, not an opaque disk.
            frame.orient(x, .09F, z, -cos(angle), 0, -sin(angle));
            quad(glow, frame, -.045F,0,-.22F, .045F,0,-.22F, .045F,0,.22F, -.045F,0,.22F, GOLD, alpha);
        }
    }

    private static void portal(Sink out, Frame f, int detail, float r, float age, float alpha, int seed) {
        if (r <= .001F) return;
        int steps = count(detail, 32, 24, 16);
        for (int i = 0; i < steps; i++) {
            float a = TAU * i / steps, b = TAU * (i + 1) / steps;
            float ca = cos(a), sa = sin(a), cb = cos(b), sb = sin(b);
            // A dark amber aperture and nested bright rims retain an open center.
            quad(out, f, 0,0,-.012F, ca*r*.88F,sa*r*.88F,-.012F,
                    cb*r*.88F,sb*r*.88F,-.012F, 0,0,-.012F, BRONZE, alpha * .19F);
            for (int band = 0; band < 3; band++) {
                float inner = r * (band == 0 ? .77F : band == 1 ? .94F : 1.04F);
                float outer = inner + r * (band == 1 ? .065F : .025F);
                quad(out, f, ca*inner,sa*inner,0, cb*inner,sb*inner,0,
                        cb*outer,sb*outer,0, ca*outer,sa*outer,0,
                        band == 1 ? PALE : GOLD, alpha * (band == 1 ? .9F : .55F));
            }
            if (i % 3 == 0) {
                float spin = age * .03F + seed;
                float aa = a + spin, bb = b + spin;
                quad(out, f, cos(aa)*r*1.16F,sin(aa)*r*1.16F,0,
                        cos(bb)*r*1.16F,sin(bb)*r*1.16F,0,
                        cos(bb)*r*1.23F,sin(bb)*r*1.23F,0,
                        cos(aa)*r*1.23F,sin(aa)*r*1.23F,0,GOLD,alpha*.65F);
            }
        }
    }

    private static void spear(Sink metal, Sink glow, Frame f, float alpha, float age) {
        for (int side = 0; side < 4; side++) {
            float a = side * TAU / 4, b = (side + 1) * TAU / 4;
            quad(metal, f, 0,0,.47F, cos(a)*.15F,sin(a)*.15F,.08F,
                    cos(b)*.15F,sin(b)*.15F,.08F, 0,0,.47F, side%2==0 ? PALE : GOLD, alpha);
            quad(metal, f, 0,0,-.13F, cos(b)*.15F,sin(b)*.15F,.08F,
                    cos(a)*.15F,sin(a)*.15F,.08F, 0,0,-.13F, BRONZE, alpha);
        }
        float pulse = .50F + .18F * sin(age * .3F);
        quad(glow, f, -.025F,0,-.08F, .025F,0,-.08F, .025F,0,.42F, -.025F,0,.42F, PALE, alpha*pulse);
    }

    private static void ring(Sink out, float x, float y, float z, float r, float w, int steps, float alpha, float spin) {
        if (alpha <= .001F) return;
        for (int i = 0; i < steps; i++) {
            float a = TAU*i/steps + spin, b = TAU*(i+1)/steps + spin;
            float ca=cos(a), sa=sin(a), cb=cos(b), sb=sin(b);
            out.vertex(x+ca*(r-w),y,z+sa*(r-w),GOLD,alpha);
            out.vertex(x+cb*(r-w),y,z+sb*(r-w),GOLD,alpha);
            out.vertex(x+cb*(r+w),y,z+sb*(r+w),PALE,alpha);
            out.vertex(x+ca*(r+w),y,z+sa*(r+w),PALE,alpha);
        }
    }
    private static void star(Sink out, float x, float y, float z, float size, float alpha) {
        out.vertex(x-size,y,z,PALE,0); out.vertex(x,y+size*2,z,PALE,alpha);
        out.vertex(x+size,y,z,PALE,0); out.vertex(x,y-size*2,z,PALE,alpha);
        out.vertex(x,y,z-size,PALE,0); out.vertex(x,y+size*2,z,PALE,alpha);
        out.vertex(x,y,z+size,PALE,0); out.vertex(x,y-size*2,z,PALE,alpha);
    }

    /** Cached faceted torus: oval links alternate by 90 degrees and have real holes. */
    private static final class Link {
        final float[] points;
        final int[] colors;
        Link(int segments) {
            points = new float[segments*4*4*3]; colors = new int[segments*4*4];
            int vertex = 0;
            for (int i=0;i<segments;i++) for (int face=0;face<4;face++) for (int corner=0;corner<4;corner++) {
                float a=TAU*(i+(corner==1||corner==2?1:0))/segments;
                float b=TAU*(face+(corner>=2?1:0))/4;
                float thickness=.029F*cos(b);
                points[vertex*3]=cos(a)*(.10F+thickness);
                points[vertex*3+1]=.029F*sin(b);
                points[vertex*3+2]=sin(a)*(.165F+thickness);
                colors[vertex++]=face==0 ? PALE : face==1 ? GOLD : face==2 ? 0xB7832A : BRONZE;
            }
        }
        void draw(Sink out, Frame f, int alternate, float alpha, float scale) {
            for (int i=0;i<colors.length;i++) {
                float x=points[i*3]*scale,y=points[i*3+1]*scale,z=points[i*3+2]*scale;
                if (alternate==0) f.vertex(out,x,y,z,colors[i],alpha);
                else f.vertex(out,-y,x,z,colors[i],alpha);
            }
        }
    }
    /** Reused basis; no temporary vectors or per-link objects during rendering. */
    private static final class Frame {
        float x,y,z,rx,ry,rz,ux,uy,uz,fx,fy,fz;
        void orient(float x,float y,float z,float dx,float dy,float dz) {
            this.x=x;this.y=y;this.z=z;
            float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            fx=dx/length;fy=dy/length;fz=dz/length;
            float horizontal=(float)Math.sqrt(fx*fx+fz*fz);
            if(horizontal<.00001F){rx=1;ry=0;rz=0;}else{rx=fz/horizontal;ry=0;rz=-fx/horizontal;}
            ux=fy*rz-fz*ry;uy=fz*rx-fx*rz;uz=fx*ry-fy*rx;
        }
        void vertex(Sink out,float a,float b,float c,int color,float alpha) {
            out.vertex(x+rx*a+ux*b+fx*c,y+ry*a+uy*b+fy*c,z+rz*a+uz*b+fz*c,color,alpha);
        }
    }
    private static void quad(Sink out,Frame f,float ax,float ay,float az,float bx,float by,float bz,
                              float cx,float cy,float cz,float dx,float dy,float dz,int color,float alpha) {
        f.vertex(out,ax,ay,az,color,alpha);f.vertex(out,bx,by,bz,color,alpha);
        f.vertex(out,cx,cy,cz,color,alpha);f.vertex(out,dx,dy,dz,color,alpha);
    }
    private static int count(int detail,int high,int medium,int low){return detail==0?high:detail==1?medium:low;}
    private static float clamp(float value){return Math.max(0,Math.min(1,value));}
    private static float smooth(float value){value=clamp(value);return value*value*(3-2*value);}
    private static float fract(float value){return value-(float)Math.floor(value);}
    private static float sin(float value){return (float)Math.sin(value);}
    private static float cos(float value){return (float)Math.cos(value);}
}
