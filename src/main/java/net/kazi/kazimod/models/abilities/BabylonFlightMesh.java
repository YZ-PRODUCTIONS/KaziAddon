package net.kazi.kazimod.models.abilities;

/**
 * Pure, bounded geometry for treasure flight and contact. Local +Z is travel for
 * trails and the outward surface normal for impacts. Every primitive is a quad;
 * soft edges and tips fade to zero rather than exposing a rectangular billboard.
 */
public final class BabylonFlightMesh {
    @FunctionalInterface
    public interface Sink {
        void vertex(double x, double y, double z, float r, float g, float b, float a);
    }

    public static final int IMPACT_TICKS = 12;
    public static final double TRAIL_RADIUS = 8;
    public static final double IMPACT_RADIUS = 4;
    private static final double TAU = Math.PI * 2;
    private static final int AMBER = 0xF5A92E, GOLD = 0xFFD365, PALE = 0xFFF0AD, WHITE = 0xFFFFE7;

    private BabylonFlightMesh() { }

    /** A short, layered comet wake; no history or particles accumulate over flight. */
    public static void trail(Sink surface, Sink glow, int detail, float age, float opacity, int seed, double speed) {
        detail = Math.max(0, Math.min(2, detail));
        opacity = clamp(opacity);
        if (!Float.isFinite(opacity) || !Float.isFinite(age) || opacity <= .001F || age <= 0
                || !Double.isFinite(speed) || speed <= .001) return;
        // This projectile flies straight. Limit the wake to two ticks of travel,
        // growing out of the gate rather than appearing through the wall behind it.
        double length = Math.min(6.8, speed * 1.95) * smooth(age / 2.2F);
        if (length < .02) return;
        double phase = random(seed, 17) * TAU;
        int segments = detail == 0 ? 14 : detail == 1 ? 10 : 6;
        int veils = detail == 2 ? 2 : 3;
        for (int plane = 0; plane < veils; plane++) {
            double angle = phase + Math.PI * plane / veils;
            double wx = Math.cos(angle), wy = Math.sin(angle);
            for (int i = 0; i < segments; i++) {
                double t = i / (double) segments, u = (i + 1) / (double) segments;
                double wa = wakeWidth(t), wb = wakeWidth(u);
                double swayA = Math.sin(t * 5.4 + age * .25 + phase) * .045 * t;
                double swayB = Math.sin(u * 5.4 + age * .25 + phase) * .045 * u;
                float aa = opacity * .105F * wakeAlpha(t), ab = opacity * .105F * wakeAlpha(u);
                ribbon(surface, -wy * swayA, wx * swayA, .22 - length * t,
                        -wy * swayB, wx * swayB, .22 - length * u,
                        wx, wy, 0, wa, wb, AMBER, aa, ab);
                ribbon(glow, -wy * swayA * .4, wx * swayA * .4, .22 - length * t,
                        -wy * swayB * .4, wx * swayB * .4, .22 - length * u,
                        wx, wy, 0, wa * .34, wb * .34, GOLD, aa * 1.75F, ab * 1.75F);
            }
        }

        // The luminous thread is thin enough that the actual item stays readable.
        for (int plane = 0; plane < 2; plane++) {
            double angle = phase + plane * Math.PI / 2;
            for (int i = 0; i < segments; i++) {
                double t = i / (double) segments, u = (i + 1) / (double) segments;
                ribbon(glow, 0, 0, .36 - length * .93 * t, 0, 0, .36 - length * .93 * u,
                        Math.cos(angle), Math.sin(angle), 0,
                        .031 * Math.pow(1 - t, .7), .031 * Math.pow(1 - u, .7), WHITE,
                        opacity * .76F * wakeAlpha(t), opacity * .76F * wakeAlpha(u));
            }
        }

        // Detached parallel filaments break up the silhouette into fine streaks.
        int strands = detail == 0 ? 4 : detail == 1 ? 3 : 1;
        for (int strand = 0; strand < strands; strand++) {
            double angle = phase + strand * 2.39996323;
            double start = .12 + random(seed, strand + 31) * .2;
            double end = .66 + random(seed, strand + 47) * .31;
            double radial = .065 + random(seed, strand + 59) * .075;
            int steps = detail == 0 ? 8 : 5;
            for (int i = 0; i < steps; i++) {
                double t = i / (double) steps, u = (i + 1) / (double) steps;
                double ra = radial * (1 + t) + .018 * Math.sin(t * 4 + age * .27 + strand);
                double rb = radial * (1 + u) + .018 * Math.sin(u * 4 + age * .27 + strand);
                float aa = opacity * .48F * (float) Math.sin(t * Math.PI);
                float ab = opacity * .48F * (float) Math.sin(u * Math.PI);
                ribbon(glow, Math.cos(angle) * ra, Math.sin(angle) * ra, -length * mix(start, end, t),
                        Math.cos(angle) * rb, Math.sin(angle) * rb, -length * mix(start, end, u),
                        -Math.sin(angle), Math.cos(angle), 0, .012 * (1 - t), .012 * (1 - u), PALE, aa, ab);
            }
        }

        int flecks = detail == 0 ? 9 : detail == 1 ? 5 : 2;
        for (int i = 0; i < flecks; i++) {
            double life = fract(age * (.065 + random(seed, i + 101) * .016) + random(seed, i + 127));
            double angle = phase + i * 2.39996323;
            double radius = .09 + .30 * life * life;
            double x = Math.cos(angle) * radius, y = Math.sin(angle) * radius;
            double z = -life * length * .92;
            double size = .009 + random(seed, i + 137) * .01;
            double streak = Math.min(.12 + random(seed, i + 149) * .22, length * .08);
            float alpha = opacity * smooth(age / .8F) * .65F * (float) Math.pow(Math.sin(life * Math.PI), 1.3);
            ribbon(glow, x, y, z, x, y, z - streak, Math.cos(angle), Math.sin(angle), 0,
                    size, 0, PALE, alpha, 0);
            ribbon(glow, x, y, z, x, y, z - streak, -Math.sin(angle), Math.cos(angle), 0,
                    size * .7, 0, WHITE, alpha * .6F, 0);
        }
    }

    /** The same contact sparkle and expanding ripple used by impacts and treasury portals. */
    public static void ripple(Sink surface, Sink glow, int detail, float age,
                              double scale, float opacity, int seed) {
        detail = Math.max(0, Math.min(2, detail));
        if (!Float.isFinite(age) || age < 0 || age >= IMPACT_TICKS
                || !Double.isFinite(scale) || scale <= 0
                || !Float.isFinite(opacity) || opacity <= 0) return;
        opacity = clamp(opacity);
        double t = age / IMPACT_TICKS;
        float fade = (float) Math.pow(1 - t, 1.65) * opacity;
        float flash = (1 - smooth(age / 3.6F)) * opacity;
        double phase = random(seed, 3) * TAU;
        int segments = detail == 0 ? 48 : detail == 1 ? 32 : 20;

        if (flash > .001F) {
            double radius = scale * (.24 + .43 * smooth(age / 2));
            disc(surface, segments, radius * 1.9, .04, AMBER, flash * .19F);
            disc(glow, segments, radius, .055, GOLD, flash * .48F);
            disc(glow, segments, radius * .45, .065, WHITE, flash * .93F);
            // Short directional needles make the initial flash feel like contact.
            int rays = detail == 2 ? 6 : 10;
            for (int i = 0; i < rays; i++) {
                double a = phase + i * TAU / rays;
                double reach = scale * (.58 + random(seed, i + 17) * .62) * (1 + age * .12);
                double ca = Math.cos(a), sa = Math.sin(a);
                ribbon(glow, ca * .09, sa * .09, .07, ca * reach, sa * reach, .07,
                        -sa, ca, 0, scale * .034, 0, PALE, flash * .8F, 0);
            }
            for (int plane = 0; plane < 2; plane++) {
                double a = phase + plane * Math.PI / 2;
                ribbon(glow, 0, 0, .03, 0, 0, scale * (1.4 + age * .10),
                        Math.cos(a), Math.sin(a), 0, scale * .055, 0, WHITE, flash * .7F, 0);
            }
        }

        // Expanding ring is split into crescents and feathered on both edges.
        double wave = scale * (.17 + 2.15 * (1 - Math.pow(1 - t, 2.2)));
        for (int i = 0; i < segments; i++) {
            double a = i * TAU / segments, b = (i + 1) * TAU / segments;
            float aa = fade * .66F * arcEnvelope(a, phase);
            float ab = fade * .66F * arcEnvelope(b, phase);
            ringSegment(surface, a, b, wave, scale * .16, .05, AMBER, aa * .23F, ab * .23F);
            ringSegment(glow, a, b, wave, scale * .028 * (1 - t * .6), .055, PALE, aa, ab);
            if (detail == 0) ringSegment(glow, a, b, wave * .87, scale * .009, .06,
                    GOLD, aa * .25F, ab * .25F);
        }
    }

    /** A contact flash, open crown, broken wave and ballistic sparks; purely visual. */
    public static void impact(Sink surface, Sink glow, int detail, float age, boolean block, int seed) {
        detail = Math.max(0, Math.min(2, detail));
        if (!Float.isFinite(age) || age < 0 || age >= IMPACT_TICKS) return;
        double t = age / IMPACT_TICKS;
        double scale = block ? 1 : .58;
        float fade = (float) Math.pow(1 - t, 1.65);
        double phase = random(seed, 3) * TAU;

        ripple(surface, glow, detail, age, scale, 1F, seed);

        // Open petals rise off the struck surface and separate as the crown fades.
        int petals = detail == 0 ? 12 : detail == 1 ? 9 : 6;
        double crown = scale * (.3 + 1.4 * (1 - Math.pow(1 - t, 2)));
        for (int i = 0; i < petals; i++) {
            double angle = phase + i * TAU / petals;
            double reach = crown * (.76 + random(seed, i + 53) * .36);
            double lift = scale * (.18 + .64 * Math.sin(t * Math.PI)) * (.6 + random(seed, i + 71) * .6);
            double ca = Math.cos(angle), sa = Math.sin(angle);
            double start = reach * .55;
            float alpha = fade * (.64F + .2F * (float) random(seed, i + 83));
            ribbon(surface, ca * start, sa * start, .06, ca * reach, sa * reach, lift,
                    -sa, ca, 0, scale * .10 * (1 - t), 0, AMBER, alpha * .19F, 0);
            ribbon(glow, ca * start, sa * start, .065, ca * reach, sa * reach, lift,
                    -sa, ca, 0, scale * .027 * (1 - t), 0, PALE, alpha, 0);
        }

        int sparks = detail == 0 ? 18 : detail == 1 ? 11 : 5;
        for (int i = 0; i < sparks; i++) {
            double life = Math.min(1, t * (1.05 + random(seed, i + 107) * .55));
            if (life >= 1) continue;
            double angle = phase + i * 2.39996323;
            double speed = scale * (1.05 + random(seed, i + 131) * 1.9);
            double ca = Math.cos(angle), sa = Math.sin(angle);
            double radial = .06 + speed * life;
            double height = scale * (.10 + (.5 + random(seed, i + 157) * 1.25) * Math.sin(life * Math.PI * .75));
            double tail = scale * (.09 + .22 * (1 - life));
            double width = scale * (.014 + .013 * random(seed, i + 181)) * (1 - life * .75);
            float alpha = (float) Math.pow(1 - life, 1.35) * .88F;
            ribbon(glow, ca * radial, sa * radial, height,
                    ca * (radial - tail), sa * (radial - tail), height - tail * .3,
                    -sa, ca, 0, 0, width, WHITE, alpha, alpha * .1F);
            ribbon(glow, ca * radial, sa * radial, height,
                    ca * (radial - tail), sa * (radial - tail), height - tail * .3,
                    0, 0, 1, 0, width * .65, GOLD, alpha * .65F, 0);
        }
    }

    private static double wakeWidth(double t) {
        return (.058 + .22 * Math.sin(t * Math.PI * .85)) * Math.pow(1 - t, .75);
    }

    private static float wakeAlpha(double t) {
        return (float) (Math.pow(1 - t, 1.3) * (.64 + .36 * Math.sin(t * Math.PI)));
    }

    private static float arcEnvelope(double angle, double phase) {
        return (float) (.18 + .82 * Math.pow(Math.max(0, Math.sin(angle * 3 + phase)), .65));
    }

    /** Two gradient quads meet at a luminous centerline. */
    private static void ribbon(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                               double wx, double wy, double wz, double wa, double wb, int color, float aa, float ab) {
        if (aa <= .001F && ab <= .001F) return;
        for (int side = -1; side <= 1; side += 2) {
            vertex(out, ax, ay, az, color, aa);
            vertex(out, bx, by, bz, color, ab);
            vertex(out, bx + wx * wb * side, by + wy * wb * side, bz + wz * wb * side, color, 0);
            vertex(out, ax + wx * wa * side, ay + wy * wa * side, az + wz * wa * side, color, 0);
        }
    }

    private static void ringSegment(Sink out, double a, double b, double radius, double width,
                                    double z, int color, float aa, float ab) {
        if (aa <= .001F && ab <= .001F) return;
        double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
        for (int side = -1; side <= 1; side += 2) {
            vertex(out, ca * radius, sa * radius, z, color, aa);
            vertex(out, cb * radius, sb * radius, z, color, ab);
            vertex(out, cb * (radius + width * side), sb * (radius + width * side), z, color, 0);
            vertex(out, ca * (radius + width * side), sa * (radius + width * side), z, color, 0);
        }
    }

    private static void disc(Sink out, int segments, double radius, double z, int color, float alpha) {
        if (alpha <= .001F) return;
        for (int i = 0; i < segments; i++) {
            double a = i * TAU / segments, b = (i + 1) * TAU / segments;
            vertex(out, 0, 0, z, color, alpha);
            vertex(out, Math.cos(a) * radius, Math.sin(a) * radius, z, color, 0);
            vertex(out, Math.cos(b) * radius, Math.sin(b) * radius, z, color, 0);
            vertex(out, 0, 0, z, color, alpha);
        }
    }

    private static void vertex(Sink out, double x, double y, double z, int color, float alpha) {
        out.vertex(x, y, z, (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, clamp(alpha));
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float smooth(float value) { value = clamp(value); return value * value * (3 - 2 * value); }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
    private static double fract(double value) { return value - Math.floor(value); }
    private static double random(int seed, int index) {
        int value = seed * 0x45d9f3b + index * 0x27d4eb2d;
        value = (value ^ (value >>> 16)) * 0x45d9f3b;
        value ^= value >>> 16;
        return (value & 0x7fffffff) / 2147483647.0;
    }
}
