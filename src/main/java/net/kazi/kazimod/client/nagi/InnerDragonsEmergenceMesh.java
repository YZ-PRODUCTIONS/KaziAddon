package net.kazi.kazimod.client.nagi;

import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;

/**
 * Bounded cinematic geometry, in a frame centered on the right shoulder: right/up/forward.
 * StrictMath transcendental functions keep vertices identical across JVM compilation tiers;
 * strictfp alone does not guarantee identical Math.sin/cos results.
 */
public strictfp final class InnerDragonsEmergenceMesh {
    public interface Sink { void vertex(double x, double y, double z, int color, float alpha); }
    private static final double[][] FAN = {
            {-4.2, 1.1, 3.0}, {-2.5, 2.7, 4.0}, {-.6, 3.8, 4.6}, {1.5, 4.1, 4.0},
            {3.4, 2.8, 3.5}, {5.1, 1.0, 1.7}, {3.7, .15, -1.2}, {1.5, 2.2, -3.1}
    };
    private InnerDragonsEmergenceMesh() { }

    public static double[] dragonPosition(int kind, float progress, float age) {
        double p = clamp(progress), swell = p * p * (3 - 2 * p);
        double[] end = FAN[Math.floorMod(kind, FAN.length)];
        return new double[]{end[0] * swell + .15 * StrictMath.sin(age * .07 + kind) * p,
                end[1] * swell + .12 * StrictMath.sin(age * .09 + kind * .8) * p,
                end[2] * swell};
    }

    public static void render(Sink solid, Sink glow, float age, int detail) {
        if (!Float.isFinite(age) || !InnerDragonsSequence.active(age)) return;
        int d = Math.max(0, Math.min(2, detail));
        double broken = age - InnerDragonsSequence.ARM_BREAK_TICK;
        if (broken < 0) {
            // Jagged white fissures climb the arm before its silhouette breaks apart.
            float buildup = (float) clamp(age / InnerDragonsSequence.ARM_BREAK_TICK);
            for (int strand = 0; strand < 5; strand++) {
                double angle = strand * Math.PI * 2 / 5;
                double oldX = StrictMath.cos(angle) * .13, oldY = -.73, oldZ = StrictMath.sin(angle) * .13;
                for (int i = 1; i <= 7; i++) {
                    double x = StrictMath.cos(angle + StrictMath.sin(i * 2.7 + strand) * .35) * .145;
                    double y = -.73 + i * .107 * buildup;
                    double z = StrictMath.sin(angle + StrictMath.sin(i * 2.7 + strand) * .35) * .145;
                    line(glow, oldX, oldY, oldZ, x, y, z, .007 + buildup * .012, 0xBEF4FF, buildup);
                    oldX = x; oldY = y; oldZ = z;
                }
            }
            return;
        }
        float pulse = (float) (.82 + .18 * StrictMath.sin(age * .4));
        float finish = (float) clamp((InnerDragonsSequence.DURATION_TICKS - age) / 12);
        float opening = (float) clamp(broken / 7);
        // The stump is an irregular, dimensional tear rather than a flat portal disc.
        int sides = d == 2 ? 10 : 18;
        for (int layer = 0; layer < 3; layer++) {
            double radius = (.14 + layer * .09) * opening;
            for (int i = 0; i < sides; i++) {
                double a = i * Math.PI * 2 / sides + age * .018 * (layer % 2 == 0 ? 1 : -1);
                double b = (i + 1) * Math.PI * 2 / sides + age * .018 * (layer % 2 == 0 ? 1 : -1);
                double ra = radius * (1 + .2 * StrictMath.sin(i * 3 + age * .2));
                double rb = radius * (1 + .2 * StrictMath.sin((i + 1) * 3 + age * .2));
                double x = -.02 + layer * .035;
                line(glow, x, StrictMath.cos(a) * ra - .08, StrictMath.sin(a) * ra,
                        x, StrictMath.cos(b) * rb - .08, StrictMath.sin(b) * rb,
                        .014 + layer * .004, layer == 0 ? 0xFFFFFF : 0x8AD9FF, pulse * finish * .8F);
            }
        }
        box(solid, -.09, -.23, -.13, .09, .07, .13, 0x0D182B, opening * finish);
        if (broken < 36) rupture(solid, glow, broken, d);
        for (DragonKind kind : DragonKind.values()) {
            float p = InnerDragonsSequence.emergence(kind.ordinal(), age);
            if (p <= 0) continue;
            double[] end = dragonPosition(kind.ordinal(), p, age);
            int segments = d == 0 ? 22 : d == 1 ? 16 : 10;
            // Three counter-rotating strands visibly connect each emerging dragon to the arm.
            int threads = d == 2 ? 2 : 3;
            for (int strand = 0; strand < threads; strand++) {
                double oldX = 0, oldY = -.08, oldZ = 0;
                for (int i = 1; i <= segments; i++) {
                    double t = i / (double) segments;
                    double curl = StrictMath.sin(t * Math.PI) * (.11 + p * .16);
                    double angle = t * Math.PI * 4 - age * .10 + strand * Math.PI * 2 / threads;
                    double x = end[0] * t + StrictMath.cos(angle) * curl;
                    double y = end[1] * t - .08 * (1 - t) + StrictMath.sin(angle) * curl + StrictMath.sin(t * Math.PI) * .5;
                    double z = end[2] * t + StrictMath.cos(angle + 1.1) * curl;
                    float alpha = (float) ((.55 - t * .24) * Math.min(1, p * 5) * finish);
                    line(glow, oldX, oldY, oldZ, x, y, z, (.025 + .018 * p) * (1 - t * .6), kind.color, alpha);
                    oldX = x; oldY = y; oldZ = z;
                }
            }
            // Deterministic motes ride the stream; no allocations, packets, or particle entities.
            int motes = d == 0 ? 10 : d == 1 ? 7 : 4;
            for (int mote = 0; mote < motes; mote++) {
                double t = (age * .014 + mote / (double) motes + kind.ordinal() * .09) % 1;
                double curl = StrictMath.sin(t * Math.PI) * .42;
                double a = mote * 2.4 + age * .08;
                double x = end[0] * t + StrictMath.cos(a) * curl;
                double y = end[1] * t + StrictMath.sin(a) * curl + .2;
                double z = end[2] * t + StrictMath.sin(a + .7) * curl;
                double size = .024 + .018 * StrictMath.sin(t * Math.PI);
                float alpha = (float) (StrictMath.sin(t * Math.PI) * finish * Math.min(1, p * 4));
                box(glow, x - size, y - size, z - size, x + size, y + size, z + size, kind.color, alpha);
            }
        }
    }

    /** A sharp detonation, followed by thinning debris; the shoulder stays readable for the dragons. */
    private static void rupture(Sink solid, Sink glow, double age, int detail) {
        // The flash blooms in the first few frames, rather than slowly growing with the debris.
        if (age < 8) {
            double p = age / 8;
            double radius = .28 + .95 * (1 - StrictMath.pow(1 - clamp(age / 3), 3));
            float alpha = (float) (.85 * StrictMath.pow(1 - p, 1.6));
            int spokes = detail == 0 ? 24 : detail == 1 ? 18 : 12;
            for (int i = 0; i < spokes; i++) {
                double a = i * 2.399963229728653;
                double y = 1 - 2 * (i + .5) / spokes;
                double r = StrictMath.sqrt(1 - y * y);
                double reach = radius * (i % 3 == 0 ? 1.6 : 1);
                line(glow, 0, -.3, 0, StrictMath.cos(a) * r * reach,
                        -.3 + y * reach, StrictMath.sin(a) * r * reach,
                        .16 * (1 - p) + .025, i % 3 == 0 ? 0xC0F4FF : 0xFFFFFF, alpha);
            }
            // Three overlapping axial flashes form a bright core without an opaque sphere.
            for (int axis = 0; axis < 3; axis++)
                shockRing(glow, axis, .06, radius * .38, .21, 12,
                        0xF0FDFF, alpha * .8F, age * .11);
        }

        // Three staggered, differently oriented fronts rapidly sweep out to about five blocks.
        for (int wave = 0; wave < 3; wave++) {
            double p = (age - wave * 2) / 18;
            if (p < 0 || p >= 1) continue;
            double radius = .2 + (4.6 - wave * .45) * (1 - StrictMath.pow(1 - p, 3));
            float alpha = (float) (.62 * StrictMath.pow(1 - p, 1.5));
            shockRing(glow, wave, 0, radius, .11 + .18 * (1 - p),
                    detail == 0 ? 36 : detail == 1 ? 24 : 16,
                    wave == 0 ? 0xE9FBFF : 0x85CEFF, alpha, wave * .37);
        }

        double t = age / 36;
        float opacity = (float) StrictMath.pow(1 - t, 1.2);
        int pieces = detail == 0 ? 48 : detail == 1 ? 32 : 18;
        for (int i = 0; i < pieces; i++) {
            double angle = i * 2.399963229728653;
            double vertical = 1 - 2 * (i + .5) / pieces;
            double horizontal = StrictMath.sqrt(1 - vertical * vertical);
            double dx = StrictMath.cos(angle) * horizontal, dz = StrictMath.sin(angle) * horizontal;
            // Fast outward burst eases to a drift: most travel happens in the first half second.
            double reach = (.8 + (i % 7) * .57) * (1 - StrictMath.exp(-age * .17));
            double x = dx * reach + .03;
            double y = -.3 + vertical * reach - t * t * .85;
            double z = dz * reach;
            double size = (.045 + (i % 4) * .017) * (1 - .65 * t);
            box(solid, x - size, y - size, z - size, x + size, y + size, z + size,
                    i % 3 == 0 ? 0xDBF5FF : 0x3D536A, opacity);
            double tail = (.35 + (i % 5) * .13) * (1 - t);
            line(glow, x, y, z, x - dx * tail, y - vertical * tail + .05,
                    z - dz * tail, size * .45, 0xCFF8FF, opacity * .9F);
        }

        // Curled wisps break up the silhouette after the white flash has already dissipated.
        int wisps = detail == 0 ? 14 : detail == 1 ? 10 : 6;
        for (int i = 0; i < wisps; i++) {
            double angle = i * 2.399963229728653;
            double radial = (.6 + i % 4 * .38) * (1 - StrictMath.exp(-age * .14));
            double oldX = StrictMath.cos(angle) * radial;
            double oldY = -.35 + StrictMath.sin(i * 1.9) * radial;
            double oldZ = StrictMath.sin(angle) * radial;
            for (int segment = 1; segment <= 5; segment++) {
                double u = segment / 5D, curl = angle + u * 2.6 + age * .09;
                double x = StrictMath.cos(angle) * radial + StrictMath.cos(curl) * u * .7;
                double y = -.35 + StrictMath.sin(i * 1.9) * radial + u * .45 + t * .8;
                double z = StrictMath.sin(angle) * radial + StrictMath.sin(curl) * u * .7;
                float a = (float) (.22 * opacity * clamp(age / 3));
                line(solid, oldX, oldY, oldZ, x, y, z, .065 * (1 - u * .6), 0x91AFC0, a);
                oldX = x; oldY = y; oldZ = z;
            }
        }
    }

    /** Slightly buckled annular fronts in three planes, with explicit quad winding. */
    private static void shockRing(Sink out, int plane, double center, double radius, double width,
                                   int segments, int color, float alpha, double turn) {
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments + turn;
            double b = (i + 1) * Math.PI * 2 / segments + turn;
            double ra = radius * (1 + .045 * StrictMath.sin(i * 2.1));
            double rb = radius * (1 + .045 * StrictMath.sin((i + 1) * 2.1));
            ringVertex(out, plane, center, a, Math.max(0, ra - width), color, alpha);
            ringVertex(out, plane, center, a, ra + width, color, alpha);
            ringVertex(out, plane, center, b, rb + width, color, alpha);
            ringVertex(out, plane, center, b, Math.max(0, rb - width), color, alpha);
        }
    }

    private static void ringVertex(Sink out, int plane, double center, double angle, double radius,
                                   int color, float alpha) {
        double a = StrictMath.cos(angle) * radius, b = StrictMath.sin(angle) * radius;
        if (plane == 0) out.vertex(a, -.3 + b, center + b * .18, color, alpha);
        else if (plane == 1) out.vertex(center + a * .22, -.3 + a, b, color, alpha);
        else out.vertex(a, -.3 + center + b * .25, b, color, alpha);
    }

    private static double clamp(double n) { return Math.max(0, Math.min(1, n)); }
    private static void line(Sink out, double x, double y, double z, double tx, double ty, double tz,
                             double width, int color, float alpha) {
        // Two crossed ribbons remain visible from the whole camera orbit.
        for (int plane = 0; plane < 2; plane++) {
            double dx = plane == 0 ? width : 0, dy = plane == 1 ? width : 0, dz = plane == 0 ? 0 : width * .35;
            out.vertex(x - dx, y - dy, z - dz, color, alpha);
            out.vertex(x + dx, y + dy, z + dz, color, alpha);
            out.vertex(tx + dx, ty + dy, tz + dz, color, alpha);
            out.vertex(tx - dx, ty - dy, tz - dz, color, alpha);
        }
    }
    private static void box(Sink out, double x, double y, double z, double X, double Y, double Z, int color, float alpha) {
        quad(out,x,y,z, X,y,z, X,Y,z, x,Y,z,color,alpha);
        quad(out,x,y,Z, x,Y,Z, X,Y,Z, X,y,Z,color,alpha);
        quad(out,x,y,z, x,Y,z, x,Y,Z, x,y,Z,color,alpha);
        quad(out,X,y,z, X,y,Z, X,Y,Z, X,Y,z,color,alpha);
        quad(out,x,y,z, x,y,Z, X,y,Z, X,y,z,color,alpha);
        quad(out,x,Y,z, X,Y,z, X,Y,Z, x,Y,Z,color,alpha);
    }
    private static void quad(Sink o,double x,double y,double z,double X,double Y,double Z,
                              double a,double b,double c,double A,double B,double C,int color,float alpha) {
        o.vertex(x,y,z,color,alpha);o.vertex(X,Y,Z,color,alpha);o.vertex(a,b,c,color,alpha);o.vertex(A,B,C,color,alpha);
    }
}
