package net.kazi.kazimod.renderers.entities.asriel;

import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Faceted bone-light blaster with articulated jaws and a visible spectral reactor. */
public final class ChaosCannonVisuals {
    private static final double TAU = Math.PI * 2;
    private static final float[] IVORY = {1, .96F, .84F};
    private static final float[] WHITE = {1, .99F, .97F};
    private static final float[] COOL = {.55F, .69F, .84F};

    private ChaosCannonVisuals() { }

    /** Includes swept rear horns, moving jaw plates, incoming energy and all recoil poses. */
    public static double cullingRadius() { return 4.6D; }

    public static void cannon(Sink out, float age, float fill, float opacity,
                              boolean enhanced, int detail, boolean firing) {
        if (!Float.isFinite(age) || !Float.isFinite(fill) || !Float.isFinite(opacity)
                || age < 0 || opacity <= 0) return;
        detail = Math.max(0, Math.min(2, detail));
        fill = clamp(fill); opacity = clamp(opacity);
        final float alpha = opacity;
        double pulse = firing ? Math.exp(-fract(age / (enhanced ? 2.7 : 3.8)) * 4) : .5 + .5 * Math.sin(age * .22);
        double recoil = firing ? -.16 * pulse : 0;
        double opening = .10 + .30 * fill + (firing ? .12 * pulse : 0);
        int plates = detail == 2 ? 4 : 6;
        double offset = Math.PI / plates;
        for (int plate = 0; plate < plates; plate++) {
            double angle = TAU * plate / plates + offset;
            double width = Math.PI / plates * .74;
            // Two separate tapering cheek sections leave the rainbow channel visible between them.
            panel(out, angle, width, -2.95 + recoil, -1.77 + recoil, .34, .70, .13, IVORY, alpha * .60F, detail);
            panel(out, angle, width * .89, -1.70 + recoil, -.68 + recoil, .71, .83, .15, IVORY, alpha * .73F, detail);
            // The forward plate swings outward as the chamber fills, exposing the hollow muzzle.
            panel(out, angle, width * .82, -.64 + recoil, .06,
                    .84, .95 + opening, .15, WHITE, alpha * .83F, detail);
            if (detail < 2 || plate % 2 == 0) {
                float[] tint = ChaosBeamVisuals.spectral(plate * 1.1 + age * .11);
                double c = Math.cos(angle), s = Math.sin(angle);
                line(out, c * .41, s * .41, -2.70 + recoil, c * .77, s * .77, -.87 + recoil,
                        .018 + .012 * fill, tint, alpha * (.16F + .58F * fill), alpha * (.24F + .58F * fill));
            }
        }

        // Solid swept horns and a forked lower jaw distinguish the weapon from a stack of rings.
        int hornSides = detail == 2 ? 3 : 4;
        for (int side = -1; side <= 1; side += 2) {
            spike(out, side * .47, .44, -2.30 + recoil,
                    side * .96, 1.08, -3.26 + recoil, .22, .12, IVORY, alpha * .83F, hornSides);
            spike(out, side * .96, 1.08, -3.26 + recoil,
                    side * 1.16, 1.29, -3.82 + recoil, .12, .012, WHITE, alpha * .85F, hornSides);
            spike(out, side * .61, -.57 - opening * .35, -.69,
                    side * (.91 + opening), -.88 - opening, .13, .16, .012,
                    IVORY, alpha * .80F, hornSides);
            // Angled eye slits sit in the cheeks, distinct from the exposed reactor.
            float[] eye = ChaosBeamVisuals.spectral(age * .13 + (side > 0 ? 1.6 : 5.1));
            line(out, side * .65, .42, -1.32 + recoil,
                    side * .89, .21, -.69 + recoil, .065, eye,
                    alpha * (.34F + .58F * fill), alpha * (.54F + .40F * fill));
            if (enhanced && detail < 2) {
                spike(out, side * .62, -.24, -2.43 + recoil,
                        side * 1.60, -.72, -3.04 + recoil, .20, .015, WHITE, alpha * .70F, hornSides);
            }
        }
        if (enhanced) {
            spike(out, 0, .72, -1.77 + recoil, 0, 1.67, -2.33 + recoil,
                    .21, .015, IVORY, alpha * .76F, hornSides);
        }

        // A thin faceted lip outlines an empty bore; no solid cap obscures the beam origin.
        int lipSides = count(detail, 12, 8, 6);
        rim(out, .91 + opening, 0.03, .075, lipSides, WHITE, alpha * .80F, 0);
        if (fill <= 0) return;
        int reactorSides = count(detail, 8, 6, 4);
        for (int band = 0; band < 7; band++) {
            float active = clamp(fill * 7 - band);
            if (active <= 0) continue;
            double z0 = -2.83 + band * .37 + recoil;
            double z1 = z0 + .28 * active;
            double r = (.16 + band * .065) * (1 + .06 * pulse);
            float[] color = ChaosBeamVisuals.spectral(band + age * .07 + (enhanced ? .8 : 0));
            for (int side = 0; side < reactorSides; side++) {
                double a = TAU * side / reactorSides + age * .04;
                double b = a + TAU / reactorSides;
                quad(out, Math.cos(a) * r, Math.sin(a) * r, z0,
                        Math.cos(b) * r, Math.sin(b) * r, z0,
                        Math.cos(b) * r * .88, Math.sin(b) * r * .88, z1,
                        Math.cos(a) * r * .88, Math.sin(a) * r * .88, z1,
                        color, alpha * active * .60F, alpha * active * .86F);
            }
        }
        line(out, 0, 0, -2.80 + recoil, 0, 0, -.12,
                .025 + .045 * fill, WHITE, alpha * fill * .63F, alpha * fill * .84F);
        if (fill > .62F) {
            float energy = (fill - .62F) / .38F;
            rim(out, .28 + .16 * energy, .075, .035 + .035 * energy, lipSides,
                    ChaosBeamVisuals.spectral(age * .18), alpha * energy * .80F, age * .06);
            if (detail < 2) rim(out, .58, -.06, .015, lipSides,
                    WHITE, alpha * energy * .40F, -age * .10);
        }
    }

    public static void charge(Sink out, float age, float fill, float arrival, boolean enhanced, int detail) {
        if (!Float.isFinite(age) || !Float.isFinite(fill) || !Float.isFinite(arrival)
                || age < 0 || arrival <= 0) return;
        detail = Math.max(0, Math.min(2, detail));
        fill = clamp(fill); arrival = clamp(arrival);
        cannon(out, age, fill, arrival, enhanced, detail, false);
        if (fill <= 0) return;
        int streams = count(detail, enhanced ? 11 : 9, 6, 3), steps = count(detail, 5, 4, 3);
        for (int stream = 0; stream < streams; stream++) {
            double phase = fract(age * .055 + stream / (double) streams);
            double yaw = stream * 2.399963 + age * .035;
            float[] color = ChaosBeamVisuals.spectral(stream * .72 + age * .15);
            double ax = 0, ay = 0, az = 0;
            for (int step = 0; step <= steps; step++) {
                double t = phase - step * .055;
                if (t < 0) break;
                double radius = (1 - t) * (2.05 + .55 * fill);
                double angle = yaw + t * (enhanced ? 2.35 : 1.55);
                double x = Math.cos(angle) * radius, y = Math.sin(angle) * radius;
                double z = -.14 - (1 - t) * .75;
                if (step > 0) {
                    float alpha = arrival * fill * (float) Math.sin(phase * Math.PI) * (1 - step / (float) (steps + 1));
                    line(out, ax, ay, az, x, y, z, .022 + .022 * fill, color, alpha * .75F, alpha * .18F);
                }
                ax = x; ay = y; az = z;
            }
        }
        if (fill > .58F) {
            int arcs = count(detail, enhanced ? 6 : 5, 3, 2);
            float energy = (fill - .58F) / .42F;
            for (int arc = 0; arc < arcs; arc++) {
                double a = arc * TAU / arcs + Math.floor(age * .6) * .27;
                double b = a + .18 + .12 * Math.sin(age + arc);
                double x = Math.cos(a), y = Math.sin(a);
                double mx = Math.cos(b) * .62, my = Math.sin(b) * .62;
                float[] color = ChaosBeamVisuals.spectral(arc * 1.2 + age * .2);
                float alpha = arrival * energy * (.40F + .35F * (float) Math.pow(Math.sin(age * .8 + arc), 2));
                line(out, x * 1.17, y * 1.17, -.15, mx, my, .18, .014, color, alpha, alpha);
                line(out, mx, my, .18, .06 * x, .06 * y, .02, .009, WHITE, alpha, alpha * .2F);
            }
        }
    }

    /** A raised outer ridge and angled side faces give each shell plate actual thickness. */
    private static void panel(Sink out, double angle, double width, double z0, double z1,
                              double r0, double r1, double thickness, float[] color, float alpha, int detail) {
        double ca = Math.cos(angle - width), sa = Math.sin(angle - width);
        double cb = Math.cos(angle + width), sb = Math.sin(angle + width);
        double cm = Math.cos(angle), sm = Math.sin(angle);
        double mid0 = r0 + thickness, mid1 = r1 + thickness;
        quad(out, ca * r0, sa * r0, z0, cm * mid0, sm * mid0, z0,
                cm * mid1, sm * mid1, z1, ca * r1, sa * r1, z1, color, alpha * .76F, alpha);
        quad(out, cm * mid0, sm * mid0, z0, cb * r0, sb * r0, z0,
                cb * r1, sb * r1, z1, cm * mid1, sm * mid1, z1, color, alpha * .51F, alpha * .74F);
        quad(out, ca * (r0 - thickness), sa * (r0 - thickness), z0, ca * r0, sa * r0, z0,
                ca * r1, sa * r1, z1, ca * (r1 - thickness), sa * (r1 - thickness), z1,
                COOL, alpha * .28F, alpha * .45F);
        if (detail == 2) return;
        quad(out, cb * r0, sb * r0, z0, cb * (r0 - thickness), sb * (r0 - thickness), z0,
                cb * (r1 - thickness), sb * (r1 - thickness), z1, cb * r1, sb * r1, z1,
                COOL, alpha * .22F, alpha * .35F);
        if (detail == 1) return;
        quad(out, ca * r1, sa * r1, z1, cm * mid1, sm * mid1, z1,
                cb * r1, sb * r1, z1, cm * (r1 - thickness), sm * (r1 - thickness), z1,
                WHITE, alpha * .82F, alpha * .68F);
        quad(out, ca * r0, sa * r0, z0, cb * r0, sb * r0, z0,
                cb * (r0 - thickness), sb * (r0 - thickness), z0,
                ca * (r0 - thickness), sa * (r0 - thickness), z0, COOL, alpha * .27F, alpha * .16F);
    }

    private static void rim(Sink out, double radius, double z, double width, int sides,
                            float[] color, float alpha, double rotation) {
        for (int side = 0; side < sides; side++) {
            double a = TAU * side / sides + rotation, b = a + TAU / sides;
            quad(out, Math.cos(a) * radius, Math.sin(a) * radius, z,
                    Math.cos(b) * radius, Math.sin(b) * radius, z,
                    Math.cos(b) * (radius - width), Math.sin(b) * (radius - width), z,
                    Math.cos(a) * (radius - width), Math.sin(a) * (radius - width), z,
                    color, alpha, alpha * .60F);
        }
    }

    private static void spike(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                              double start, double end, float[] color, float alpha, int sides) {
        double dx = bx - ax, dy = by - ay, dz = bz - az, length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < .00001) return;
        dx /= length; dy /= length; dz /= length;
        double sx = -dz, sy = 0, sz = dx;
        double s = Math.sqrt(sx * sx + sz * sz);
        if (s < .00001) { sx = 1; sz = 0; s = 1; }
        sx /= s; sz /= s;
        double tx = dy * sz - dz * sy, ty = dz * sx - dx * sz, tz = dx * sy - dy * sx;
        for (int side = 0; side < sides; side++) {
            double a = TAU * side / sides, b = a + TAU / sides;
            double ux = sx * Math.cos(a) + tx * Math.sin(a), uy = sy * Math.cos(a) + ty * Math.sin(a), uz = sz * Math.cos(a) + tz * Math.sin(a);
            double vx = sx * Math.cos(b) + tx * Math.sin(b), vy = sy * Math.cos(b) + ty * Math.sin(b), vz = sz * Math.cos(b) + tz * Math.sin(b);
            quad(out, ax + ux * start, ay + uy * start, az + uz * start,
                    ax + vx * start, ay + vy * start, az + vz * start,
                    bx + vx * end, by + vy * end, bz + vz * end,
                    bx + ux * end, by + uy * end, bz + uz * end,
                    color, alpha * (side % 2 == 0 ? .95F : .54F), alpha * .80F);
        }
    }

    private static void line(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                             double width, float[] color, float firstAlpha, float lastAlpha) {
        if (firstAlpha <= 0 && lastAlpha <= 0) return;
        double dx = bx - ax, dy = by - ay, dz = bz - az, length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < .00001) return;
        dx /= length; dy /= length; dz /= length;
        double sx, sy, sz;
        if (Math.abs(dy) > .9) { sx = 0; sy = dz; sz = -dy; }
        else { sx = -dz; sy = 0; sz = dx; }
        double scale = width / Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= scale; sy *= scale; sz *= scale;
        for (int cross = 0; cross < 2; cross++) {
            quad(out, ax + sx, ay + sy, az + sz, ax - sx, ay - sy, az - sz,
                    bx - sx, by - sy, bz - sz, bx + sx, by + sy, bz + sz, color, firstAlpha, lastAlpha);
            double nx = dy * sz - dz * sy, ny = dz * sx - dx * sz, nz = dx * sy - dy * sx;
            sx = nx; sy = ny; sz = nz;
        }
    }

    private static void quad(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz,
                             float[] color, float firstAlpha, float lastAlpha) {
        out.vertex(ax, ay, az, color[0], color[1], color[2], firstAlpha);
        out.vertex(bx, by, bz, color[0], color[1], color[2], firstAlpha);
        out.vertex(cx, cy, cz, color[0], color[1], color[2], lastAlpha);
        out.vertex(dx, dy, dz, color[0], color[1], color[2], lastAlpha);
    }

    private static int count(int detail, int high, int middle, int low) { return detail == 0 ? high : detail == 1 ? middle : low; }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static double fract(double value) { return value - Math.floor(value); }
}
