package net.kazi.kazimod.client.nagi;

/** Arm-local, texture-free light geometry. +Y runs from the shoulder towards the hand. */
public final class ImagineBreakerAuraMesh {
    private static final double TAU = Math.PI * 2;
    private static final int AMBER = 0xFF791C, GOLD = 0xFFCF59, WHITE = 0xFFFFD8;

    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z, int rgb, float alpha);
    }

    private ImagineBreakerAuraMesh() {}

    public static void render(Sink sink, float age, int detail, float opacity) {
        if (opacity <= 0 || !Float.isFinite(age)) return;
        opacity = Math.min(1, opacity);
        int sides = detail == 0 ? 12 : 8;
        int strands = detail == 0 ? 6 : detail == 1 ? 4 : 3;
        int steps = detail == 0 ? 7 : 5;
        double time = age * .09;
        float pulse = (float) (.86 + .14 * Math.sin(time * 2.7));

        // A faint sleeve of light, with transparent ends and no opaque shell.
        for (int row = 0; row < 4; row++) {
            double y0 = .08 + row * .16, y1 = y0 + .16;
            float a0 = row == 0 ? 0 : .09F * opacity;
            float a1 = row == 3 ? 0 : .09F * opacity;
            double r0 = .18 + .025 * Math.sin(row + time);
            double r1 = .18 + .025 * Math.sin(row + 1 + time);
            for (int i = 0; i < sides; i++) {
                double a = TAU * i / sides, b = TAU * (i + 1) / sides;
                polar(sink, a, r0, y0, GOLD, a0); polar(sink, b, r0, y0, GOLD, a0);
                polar(sink, b, r1, y1, GOLD, a1); polar(sink, a, r1, y1, GOLD, a1);
            }
        }

        // Broad amber edges and fine pale cores curl along the forearm, not the torso.
        for (int strand = 0; strand < strands; strand++) {
            for (int pass = 0; pass < 2; pass++) {
                for (int segment = 0; segment < steps; segment++) {
                    double u = segment / (double) steps, v = (segment + 1.0) / steps;
                    ribbonPoint(sink, strand, strands, time, u, -1, pass, opacity * pulse);
                    ribbonPoint(sink, strand, strands, time, u, 1, pass, opacity * pulse);
                    ribbonPoint(sink, strand, strands, time, v, 1, pass, opacity * pulse);
                    ribbonPoint(sink, strand, strands, time, v, -1, pass, opacity * pulse);
                }
            }
        }

        // Crossed, smoothly fading discs give the palm volume from every viewing angle.
        for (int plane = 0; plane < 3; plane++) {
            for (int i = 0; i < sides; i++) {
                double a = TAU * i / sides, b = TAU * (i + 1) / sides;
                disc(sink, plane, a, 0, WHITE, opacity * pulse * .68F);
                disc(sink, plane, a, .12, GOLD, opacity * pulse * .32F);
                disc(sink, plane, b, .12, GOLD, opacity * pulse * .32F);
                disc(sink, plane, b, 0, WHITE, opacity * pulse * .68F);
                disc(sink, plane, a, .12, GOLD, opacity * pulse * .32F);
                disc(sink, plane, a, .32, AMBER, 0);
                disc(sink, plane, b, .32, AMBER, 0);
                disc(sink, plane, b, .12, GOLD, opacity * pulse * .32F);
            }
        }

        int rays = detail == 0 ? 10 : detail == 1 ? 7 : 5;
        for (int ray = 0; ray < rays; ray++) {
            for (int pass = 0; pass < 2; pass++) {
                for (int step = 0; step < 3; step++) {
                    rayPoint(sink, ray, rays, time, step, -1, pass, opacity);
                    rayPoint(sink, ray, rays, time, step, 1, pass, opacity);
                    rayPoint(sink, ray, rays, time, step + 1, 1, pass, opacity);
                    rayPoint(sink, ray, rays, time, step + 1, -1, pass, opacity);
                }
            }
        }

        // Small expanding ripples and embers are mesh details, not spawned particles/packets.
        for (int ring = 0; ring < 2; ring++) {
            double life = fraction(age / 24.0 + ring * .5);
            double radius = .18 + life * .27;
            float alpha = (float) (Math.sin(life * Math.PI) * .24 * opacity);
            for (int i = 0; i < sides; i++) {
                double a = TAU * i / sides, b = TAU * (i + 1) / sides;
                polar(sink, a, radius - .013, .65 + life * .12, GOLD, 0);
                polar(sink, b, radius - .013, .65 + life * .12, GOLD, 0);
                polar(sink, b, radius, .65 + life * .12, GOLD, alpha);
                polar(sink, a, radius, .65 + life * .12, GOLD, alpha);
                polar(sink, a, radius, .65 + life * .12, GOLD, alpha);
                polar(sink, b, radius, .65 + life * .12, GOLD, alpha);
                polar(sink, b, radius + .025, .65 + life * .12, AMBER, 0);
                polar(sink, a, radius + .025, .65 + life * .12, AMBER, 0);
            }
        }
        int embers = detail == 0 ? 8 : detail == 1 ? 5 : 3;
        for (int i = 0; i < embers; i++) {
            double life = fraction(age / (17.0 + i) + i * .618);
            double angle = i * 2.4 + time * .3;
            double radius = .2 + life * .3;
            double x = Math.cos(angle) * radius, y = .22 + .42 * fraction(i * .37) - life * .14;
            double z = Math.sin(angle) * radius;
            float alpha = (float) (Math.sin(life * Math.PI) * .7 * opacity);
            double size = .009 + .008 * (1 - life);
            for (int plane = 0; plane < 2; plane++) {
                vertex(sink, x, y - size * 2, z, WHITE, alpha);
                vertex(sink, x + (plane == 0 ? size : 0), y, z + (plane == 1 ? size : 0), GOLD, 0);
                vertex(sink, x, y + size * 2, z, GOLD, alpha);
                vertex(sink, x - (plane == 0 ? size : 0), y, z - (plane == 1 ? size : 0), GOLD, 0);
            }
        }
    }

    private static void ribbonPoint(Sink sink, int strand, int count, double time,
                                    double u, int edge, int pass, float opacity) {
        double angle = TAU * strand / count + time * .45 + u * 2.1
                + Math.sin(u * 24 + strand * 2 + time * 2) * .2;
        double radius = .18 + .025 * Math.sin(u * 17 - time * 2 + strand);
        double width = (pass == 0 ? .17 : .045) * Math.sin(Math.PI * u);
        polar(sink, angle + edge * width, radius + pass * .002, .09 + u * .64,
                pass == 0 ? AMBER : WHITE,
                (float) (Math.sin(Math.PI * u) * opacity * (pass == 0 ? .28 : .65)));
    }

    private static void rayPoint(Sink sink, int ray, int count, double time,
                                 int step, int edge, int pass, float opacity) {
        double u = step / 3.0;
        double angle = ray * TAU / count + time * .17
                + Math.sin(ray * 3.1 + step * 2.5 + time * 2) * .15 * u;
        double length = .34 + .1 * Math.sin(ray * 2.7 + time * 3);
        double radius = .12 + u * length;
        double width = (pass == 0 ? .14 : .035) * (1 - u);
        polar(sink, angle + edge * width, radius,
                .65 + Math.sin(ray * 1.7 + time) * .16 * u,
                pass == 0 ? AMBER : GOLD, (float) ((1 - u) * opacity * (pass == 0 ? .35 : .8)));
    }

    private static void disc(Sink sink, int plane, double angle, double radius, int color, float alpha) {
        double u = Math.cos(angle) * radius, v = Math.sin(angle) * radius;
        vertex(sink, plane == 2 ? 0 : u, .65 + (plane == 0 ? 0 : v),
                plane == 0 ? v : plane == 1 ? 0 : u, color, alpha);
    }

    private static void polar(Sink sink, double angle, double radius, double y, int color, float alpha) {
        vertex(sink, Math.cos(angle) * radius, y, Math.sin(angle) * radius, color, alpha);
    }

    private static void vertex(Sink sink, double x, double y, double z, int color, float alpha) {
        sink.vertex((float) x, (float) y, (float) z, color, Math.max(0, Math.min(1, alpha)));
    }

    private static double fraction(double value) { return value - Math.floor(value); }
}
