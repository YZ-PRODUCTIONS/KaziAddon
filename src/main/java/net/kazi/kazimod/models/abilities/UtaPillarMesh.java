package net.kazi.kazimod.models.abilities;

/** Chain-free, fluted gold pillar with a crimson-inlaid black grip and crowned cap. */
public final class UtaPillarMesh {
    public static final float HEIGHT = 32.5F;
    public static final float MAX_RADIUS = 3.4F;
    public static final float PORTAL_HEIGHT = 74;
    public static final int DROP_TICK = 52;
    public static final int DROP_DURATION = 4;
    public static final int IMPACT_TICK = DROP_TICK + DROP_DURATION;
    public static float bottomAt(float age) {
        float emergence = clamp((age - 12) / 40);
        float fall = clamp((age - DROP_TICK) / DROP_DURATION);
        return PORTAL_HEIGHT - HEIGHT * emergence
                - (PORTAL_HEIGHT - HEIGHT) * (.2F * fall + .8F * fall * fall);
    }
    private UtaPillarMesh() { }
    public static void draw(EaVfxMesh.Sink body, EaVfxMesh.Sink glow, float age) {
        float fade = 1 - clamp((age - (IMPACT_TICK + 20)) / 25);
        portal(glow, age);
        if (age < 12) return;
        // The long gold shaft emerges first, with the crown above the black grip.
        float bottom = bottomAt(age);
        staffTube(body, bottom, 0, 21, 1.4F, 1.4F, 0xE6B749, fade, true);
        staffTube(body, bottom, 21, 22, 1.65F, 1.65F, 0xFFE29A, fade, false);
        staffTube(body, bottom, 22, 29, 1.45F, 1.45F, 0x100D12, fade, false);
        for (int i = 0; i < 12; i++) {
            float angle = i * (float)Math.PI / 6;
            for (int j = 0; j < 7; j++) {
                float offset = j == 3 ? .19F : 0;
                stripe(glow, bottom + 22 + j, bottom + 23 + j,
                        angle + offset, angle + (j == 2 ? .19F : 0),
                        2.94F, 0xED1935, fade);
            }
        }
        staffTube(body, bottom, 29, 29.7F, MAX_RADIUS / 2, MAX_RADIUS / 2, 0xFFE29A, fade, false);
        staffTube(body, bottom, 29.7F, 31, 1.5F, .8F, 0xD8A639, fade, true);
        staffTube(body, bottom, 31, 32, .8F, .8F, 0x125E58, fade, true);
        staffTube(body, bottom, 32, HEIGHT, 1.2F, .8F, 0xFFE29A, fade, true);
        if (age >= DROP_TICK && age < IMPACT_TICK) {
            for (int i = 0; i < 5; i++) ring(glow, bottom + i * 4, 2 + i * .3F,
                    .12F, 0xFFE6AA, .35F, age);
        }
        if (age >= IMPACT_TICK) {
            float t = age - IMPACT_TICK;
            for (int i = 0; i < 3; i++) ring(glow, .15F + i * .15F,
                    Math.max(0, t - i * 3) * .75F, .22F, 0xFFD475,
                    clamp(1 - t / 35) * .7F, 0);
        }
    }
    private static float clamp(float f) { return Math.max(0, Math.min(1, f)); }
    private static void staffTube(EaVfxMesh.Sink out, float bottom, float lo, float hi,
                                  float r0, float r1, int color, float alpha, boolean fluted) {
        tube(out, bottom + lo, bottom + hi, r0 * 2, r1 * 2, color, alpha, fluted);
    }

    /** Soft amber opening and irregular white-gold ripples, inspired by the supplied animation. */
    private static void portal(EaVfxMesh.Sink glow, float age) {
        float opening = clamp(age / 12);
        opening = opening * opening * (3 - 2 * opening);
        float fade = 1 - clamp((age - IMPACT_TICK) / 20);
        if (opening <= 0 || fade <= 0) return;
        // Broad, low-opacity bands overlap into a haze instead of an opaque disk.
        for (int i = 0; i < 10; i++) {
            float radius = (1 + i * .95F) * opening;
            portalBand(glow, radius, 1.25F * opening, age, i * .53F,
                    0xFFAF35, .025F * fade, false);
        }
        // Each ripple has a bright narrow ridge and several softer shoulders.
        // Moving radial waves and uneven angular brightness keep it from looking like a fixed object.
        for (int i = 0; i < 6; i++) {
            float cycle = (age * .013F + i / 6F) % 1;
            float radius = (2.1F + cycle * 6.5F) * opening;
            float alpha = (float)Math.sin(cycle * Math.PI) * fade;
            for (int layer = 3; layer >= 0; layer--) {
                portalBand(glow, radius, (.065F + layer * .19F) * opening,
                        age, i * 1.7F, layer == 0 ? 0xFFF4C2 : 0xFFCC65,
                        alpha * (layer == 0 ? .7F : .07F), true);
            }
        }
    }

    private static void portalBand(EaVfxMesh.Sink out, float radius, float width, float age,
                                   float phase, int color, float alpha, boolean ripple) {
        for (int i = 0; i < 128; i++) {
            float a = i * (float)Math.PI / 64, b = (i + 1) * (float)Math.PI / 64;
            float ra = portalRadius(radius, a, age, phase, ripple);
            float rb = portalRadius(radius, b, age, phase, ripple);
            float aa = alpha * (.6F + .4F * (float)Math.sin(a * 3 + phase - age * .10F));
            float ab = alpha * (.6F + .4F * (float)Math.sin(b * 3 + phase - age * .10F));
            float y = PORTAL_HEIGHT + phase * .002F;
            v(out, a, Math.max(0, ra - width), y, color, aa);
            v(out, b, Math.max(0, rb - width), y, color, ab);
            v(out, b, rb + width, y, color, ab);
            v(out, a, ra + width, y, color, aa);
        }
    }

    private static float portalRadius(float r, float angle, float age, float phase, boolean ripple) {
        if (!ripple) return r;
        return r * (1 + .045F * (float)Math.sin(angle * 3 + phase + age * .065F)
                + .022F * (float)Math.sin(angle * 7 - phase - age * .09F));
    }
    private static void tube(EaVfxMesh.Sink out, float lo, float hi, float r0, float r1,
                             int color, float alpha, boolean fluted) {
        if (lo >= PORTAL_HEIGHT) return;
        float top = Math.min(hi, PORTAL_HEIGHT);
        r1 = r0 + (r1 - r0) * (top - lo) / (hi - lo);
        for (int i = 0; i < 48; i++) {
            float a = i * (float)Math.PI / 24, b = (i + 1) * (float)Math.PI / 24;
            float f0 = fluted && i % 4 == 0 ? .86F : 1;
            float f1 = fluted && (i + 1) % 4 == 0 ? .86F : 1;
            int shade = i % 4 == 0 && fluted ? 0xB77E24 : color;
            v(out,a,r0*f0,lo,shade,alpha); v(out,b,r0*f1,lo,shade,alpha);
            v(out,b,r1*f1,top,shade,alpha); v(out,a,r1*f0,top,shade,alpha);
            v(out,a,r0*f0,lo,shade,alpha); out.vertex(0,lo,0,shade,alpha);
            out.vertex(0,lo,0,shade,alpha); v(out,b,r0*f1,lo,shade,alpha);
        }
    }
    private static void stripe(EaVfxMesh.Sink out,float lo,float hi,float a,float b,float r,int c,float f) {
        if (lo >= PORTAL_HEIGHT) return;
        hi = Math.min(hi,PORTAL_HEIGHT);
        v(out,a-.025F,r,lo,c,f); v(out,a+.025F,r,lo,c,f);
        v(out,b+.025F,r,hi,c,f); v(out,b-.025F,r,hi,c,f);
    }
    private static void ring(EaVfxMesh.Sink out,float y,float r,float width,int c,float alpha,float phase) {
        for (int i=0;i<96;i++) {
            float a=i*(float)Math.PI/48,b=(i+1)*(float)Math.PI/48;
            float ra=r+(float)Math.sin(a*7+phase)*.10F,rb=r+(float)Math.sin(b*7+phase)*.10F;
            v(out,a,Math.max(0,ra-width),y,c,alpha); v(out,b,Math.max(0,rb-width),y,c,alpha);
            v(out,b,rb+width,y,c,alpha); v(out,a,ra+width,y,c,alpha);
        }
    }
    private static void v(EaVfxMesh.Sink out,float a,float r,float y,int c,float alpha) {
        out.vertex((float)Math.cos(a)*r,y,(float)Math.sin(a)*r,c,alpha);
    }
}
