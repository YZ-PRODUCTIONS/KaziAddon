package net.kazi.kazimod.renderers.entities.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Violent spectral lightning, a single ground detonation and airborne discharge fragments. */
public final class ShockerBreakerVisuals {
    private static final double TAU = Math.PI * 2;
    private static final float[] WHITE = {1, .99F, .97F};
    private static final double[][] CIRCLES = {circle(8), circle(6), circle(5)};
    private ShockerBreakerVisuals() { }

    /** The fixed target stays free of invented warning rings; the caster carries the pre-strike electricity. */
    public static void render(Sink out, float age, float radius, boolean enhanced, int detail) {
        if (!Float.isFinite(age) || !Float.isFinite(radius) || radius <= 0
                || age < ShockerBreakerVisualTiming.CHARGE_TICKS || age >= ShockerBreakerVisualTiming.lifetime(enhanced)) return;
        detail = Math.max(0, Math.min(2, detail));
        float time = age - ShockerBreakerVisualTiming.CHARGE_TICKS;
        float fade = ShockerBreakerVisualTiming.fade(time, enhanced);
        if (fade <= 0) return;
        float pulse = ShockerBreakerVisualTiming.pulse(time, enhanced);
        float[] color = vivid(time * .24);
        double unit = radius / 3.0;
        // Each warning produces one detonation; later impacts belong to fresh markers.
        float column = ShockerBreakerVisualTiming.columnStrength(time, enhanced)
                * ShockerBreakerVisualTiming.sustainedFlicker(time);
        column(out, time, unit, color, column * fade, pulse, enhanced, detail);
        plumes(out, time, radius, column * fade, pulse, enhanced, detail);
        filaments(out, time, unit, color, fade * column, enhanced, detail);
        branches(out, time, radius, fade * column, enhanced, detail);
        airborneEruptions(out, time, radius, fade * column, enhanced, detail);
        impact(out, time, radius, color, pulse, fade, enhanced, detail);
        shockFronts(out, time, radius, color, pulse, fade, enhanced, detail);
        arcs(out, time, radius, color, fade, detail);
        groundDischarges(out, time, radius, fade * column, enhanced, detail);
        shards(out, time, radius, color, fade, enhanced, detail);
    }

    /** Brief rainbow forks around the caster. */
    public static void chargeAura(Sink out, float age, int detail) {
        if (!Float.isFinite(age) || age < 0 || age >= ShockerBreakerVisualTiming.CHARGE_TICKS) return;
        detail = Math.max(0, Math.min(2, detail));
        float envelope = clamp(age / 4) * clamp((34 - age) / 10);
        if (envelope <= 0) return;
        int forks = count(detail, 6, 5, 4), segments = count(detail, 9, 7, 5);
        double snap = Math.floor(age / 2);
        for (int fork = 0; fork < forks; fork++) {
            double yaw = fork * 2.399963 + .35;
            double sweep = (hash(fork * 17 + 3) - .5) * 2.15;
            double startHeight = .12 + .32 * hash(fork + 9);
            double height = 1.35 + 1.45 * hash(fork * 5 + 7);
            float[] color = vivid(age * .24 + fork * .85);
            float flicker = (float) (.35 + .65 * Math.pow(Math.sin(age * .58 + fork * 2.2), 2));
            float alpha = envelope * flicker;
            double ax = 0, ay = 0, az = 0;
            for (int step = 0; step <= segments; step++) {
                double t = step / (double) segments;
                double a = yaw + sweep * t + Math.sin(step * 8.1 + snap * 2.1 + fork) * .14;
                double r = .95 + .25 * t + .30 * Math.sin(step * 13.7 + fork * 6.1 + snap);
                double bx = Math.cos(a) * r, bz = Math.sin(a) * r;
                double by = startHeight + t * height;
                if (step > 0) {
                    beam(out, ax, ay, az, bx, by, bz, .065, color, alpha * .14F);
                    beam(out, ax, ay, az, bx, by, bz, .015, color, alpha * .78F);
                    if (detail == 0) beam(out, ax, ay, az, bx, by, bz, .004, WHITE, alpha * .52F);
                }
                if (step == segments / 2 && detail < 2) {
                    double dx = Math.cos(a + .9), dz = Math.sin(a + .9);
                    beam(out, bx, by, bz, bx + dx * .30, by + .20, bz + dz * .30,
                            .014, color, alpha * .65F);
                    beam(out, bx + dx * .30, by + .20, bz + dz * .30,
                            bx + dx * .59, by + .27, bz + dz * .44, .009, color, alpha * .45F);
                    beam(out, bx + dx * .59, by + .27, bz + dz * .44,
                            bx + dx * .65, by + .54, bz + dz * .56, .004, color, alpha * .22F);
                }
                ax = bx; ay = by; az = bz;
            }
        }
    }

    private static void column(Sink out, float time, double unit, float[] color, float alpha,
                               float pulse, boolean enhanced, int detail) {
        if (alpha <= .001F) return;
        int steps = count(detail, 14, 10, 8);
        double[] circle = CIRCLES[detail];
        int sides = circle.length / 2 - 1;
        double width = unit * (.48 + .38 * pulse) * (enhanced ? 1.12 : 1);
        double phase = Math.floor(time * 2);
        for (int step = 0; step < steps; step++) {
            color = vivid(time * .28 + step * 6.2 / steps);
            double ya = 24.0 * step / steps, yb = 24.0 * (step + 1) / steps;
            double ax = unit * .025 * Math.sin(ya * 2.8 + phase * 1.31);
            double az = unit * .022 * Math.cos(ya * 3.4 + phase * .94);
            double bx = unit * .025 * Math.sin(yb * 2.8 + phase * 1.31);
            double bz = unit * .022 * Math.cos(yb * 3.4 + phase * .94);
            double ra = width * (.82 + .24 * Math.sin(ya * 4.7 - time * 2.4) + .20 * hash(step * 13 + (int) phase));
            double rb = width * (.82 + .24 * Math.sin(yb * 4.7 - time * 2.4) + .20 * hash((step + 1) * 13 + (int) phase));
            for (int layer = 0; layer < 2; layer++) {
                double scale = layer == 0 ? 1.35 : .16;
                float[] tint = layer == 0 ? color : WHITE;
                float opacity = alpha * (layer == 0 ? .29F : .99F);
                for (int side = 0; side < sides; side++) {
                    double ca = circle[side * 2], sa = circle[side * 2 + 1];
                    double cb = circle[(side + 1) * 2], sb = circle[(side + 1) * 2 + 1];
                    quad(out, ax + ca * ra * scale, ya, az + sa * ra * scale,
                            bx + ca * rb * scale, yb, bz + sa * rb * scale,
                            bx + cb * rb * scale, yb, bz + sb * rb * scale,
                            ax + cb * ra * scale, ya, az + sb * ra * scale, tint, opacity);
                }
            }
        }
    }

    /** Torn counter-flowing sheets carry several vivid hues at once around the thin white spine. */
    private static void plumes(Sink out, float time, double radius, float strength, float pulse,
                               boolean enhanced, int detail) {
        if (strength <= .001F) return;
        int strips = count(detail, enhanced ? 7 : 6, enhanced ? 5 : 4, 3);
        int steps = count(detail, 14, 10, 7);
        double snap = Math.floor(time * 1.6);
        for (int strip = 0; strip < strips; strip++) {
            double start = TAU * strip / strips - time * (enhanced ? .46 : .36);
            double twist = (strip % 2 == 0 ? 1 : -1) * 4.8;
            for (int step = 0; step < steps; step++) {
                if (hash(strip * 37 + step * 17 + (int) snap) > .87) continue;
                double ta = step / (double) steps, tb = (step + 1) / (double) steps;
                double a = start + ta * twist + .31 * Math.sin(step * 6.7 + snap * 2.3 + strip);
                double b = start + tb * twist + .31 * Math.sin((step + 1) * 6.7 + snap * 2.3 + strip);
                double ra = radius * (.32 + .13 * Math.sin(ta * 37 - time * 3.6 + strip) + .06 * pulse);
                double rb = radius * (.32 + .13 * Math.sin(tb * 37 - time * 3.6 + strip) + .06 * pulse);
                double wa = .16 + .15 * hash(step * 31 + strip + (int) snap);
                double wb = .025 + .07 * hash(step * 11 + strip + (int) snap);
                float[] tint = vivid(time * .27 + strip * 7.0 / strips + ta * 2.1);
                float alpha = strength * (enhanced ? .92F : .84F);
                gradient(out, Math.cos(a - wa) * ra, ta * 24, Math.sin(a - wa) * ra,
                        Math.cos(a + wa) * ra, ta * 24, Math.sin(a + wa) * ra,
                        Math.cos(b + wb) * rb, tb * 24, Math.sin(b + wb) * rb,
                        Math.cos(b - wb) * rb, tb * 24, Math.sin(b - wb) * rb,
                        tint, alpha * .20F, alpha, alpha * .66F, 0);
            }
        }
    }

    private static void filaments(Sink out, float time, double unit, float[] color, float alpha, boolean enhanced, int detail) {
        if (alpha <= .001F) return;
        int strands = count(detail, enhanced ? 6 : 5, 4, 3), steps = count(detail, 12, 9, 6);
        double snap = Math.floor(time * 2.6);
        for (int strand = 0; strand < strands; strand++) {
            double rotation = strand * TAU / strands;
            double ax = 0, ay = 0, az = 0;
            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps;
                color = vivid(time * .31 + t * 6.2 + strand * .9);
                double angle = rotation + Math.sin(t * 25 + snap * .91 + strand) * 1.13;
                double spread = unit * (1.28 + .72 * Math.sin(step * 4.8 + snap * 2.37 + strand));
                double bx = Math.cos(angle) * spread, bz = Math.sin(angle) * spread, by = t * 24;
                float opacity = alpha * (.68F + .25F * (float) Math.sin(t * Math.PI));
                if (step > 0) {
                    beam(out, ax, ay, az, bx, by, bz, unit * .065, color, opacity);
                    if (detail == 0 && strand < 2)
                        beam(out, ax, ay, az, bx, by, bz, unit * .008, WHITE, opacity * .55F);
                }
                ax = bx; ay = by; az = bz;
            }
        }
    }

    /** Wide side forks flicker independently while the original impact remains a single detonation. */
    private static void branches(Sink out, float time, double radius, float strength, boolean enhanced, int detail) {
        if (strength <= .001F) return;
        int forks = count(detail, enhanced ? 8 : 6, enhanced ? 5 : 4, enhanced ? 3 : 2);
        int steps = count(detail, 4, 3, 3);
        double phase = Math.floor(time * 2), unit = radius / 3;
        for (int fork = 0; fork < forks; fork++) {
            double angle = fork * 2.399963 + .16 * Math.sin(phase + fork * 3.1);
            double dx = Math.cos(angle), dz = Math.sin(angle);
            double start = radius * .25, end = radius * (1.02 + .26 * hash(fork * 11 + 7));
            double height = 4.4 + 16.2 * hash(fork * 19 + 2), drop = 2.0 + 1.9 * hash(fork * 5 + 3);
            float alpha = strength * (float) (.50 + .48 * Math.pow(Math.sin(time * 2.1 + fork * 1.7), 2));
            float[] tint = vivid(time * .29 + fork * .92);
            double ax = dx * start, ay = height, az = dz * start;
            for (int step = 1; step <= steps; step++) {
                double t = step / (double) steps;
                double offset = unit * .36 * Math.sin(step * 7.1 + phase * 2.7 + fork) * Math.sin(t * Math.PI);
                double reach = start + (end - start) * t;
                double bx = dx * reach - dz * offset, bz = dz * reach + dx * offset;
                double by = height - drop * t + unit * .38 * Math.sin(step * 8.3 + phase) * Math.sin(t * Math.PI);
                double taper = 1 - .65 * t;
                beam(out, ax, ay, az, bx, by, bz, unit * .085 * taper, tint, alpha * .90F);
                if (detail == 0)
                    beam(out, ax, ay, az, bx, by, bz, unit * .008 * taper, WHITE, alpha * .42F);
                ax = bx; ay = by; az = bz;
            }
        }
    }

    /** Fast local eruptions travel through the air; none produce another ground-impact flash. */
    private static void airborneEruptions(Sink out, float time, double radius, float strength,
                                         boolean enhanced, int detail) {
        if (strength <= .001F) return;
        int nodes = count(detail, enhanced ? 6 : 5, enhanced ? 4 : 3, 2);
        int spokes = count(detail, 6, 5, 3);
        for (int node = 0; node < nodes; node++) {
            double phase = positiveMod(time * (enhanced ? .084 : .071) + node / (double) nodes, 1);
            double centerY = 2 + (1 - phase) * 20;
            double flash = Math.pow(Math.sin(phase * Math.PI), .55);
            double rotation = node * 2.399963 + time * (node % 2 == 0 ? .55 : -.43);
            for (int spoke = 0; spoke < spokes; spoke++) {
                double jitter = hash(node * 31 + spoke * 13 + (int) Math.floor(time * 1.2));
                double angle = rotation + TAU * spoke / spokes + (jitter - .5) * .65;
                double dx = Math.cos(angle), dz = Math.sin(angle);
                double start = radius * .14;
                double reach = radius * (.48 + .73 * jitter) * (.40 + .60 * flash);
                double width = radius * (.035 + .030 * jitter);
                double ox = -dz * width, oz = dx * width;
                double tipY = Math.max(.35, Math.min(23.65, centerY + (jitter - .5) * 3.0));
                double bendY = centerY + (tipY - centerY) * .48;
                float[] tint = vivid(time * .28 + node * 1.1 + spoke * 7.0 / spokes);
                float alpha = strength * (float) flash * (enhanced ? .92F : .82F);
                gradient(out, dx * start - ox, centerY, dz * start - oz,
                        dx * reach * .52 - ox * .4, bendY, dz * reach * .52 - oz * .4,
                        dx * reach, tipY, dz * reach,
                        dx * start + ox, centerY, dz * start + oz,
                        tint, alpha * .78F, alpha, 0, alpha * .26F);
                if (detail < 2 && spoke % 2 == node % 2)
                    beam(out, dx * start, centerY, dz * start, dx * reach, tipY, dz * reach,
                            radius * .009, tint, alpha * .80F);
            }
        }
    }

    private static void impact(Sink out, float time, double radius, float[] color, float pulse,
                               float fade, boolean enhanced, int detail) {
        float flash = (float) Math.sqrt(pulse) * fade;
        int sides = count(detail, 20, 14, 10);
        double ground = radius * (.35 + .88 * pulse);
        for (int side = 0; side < sides; side++) {
            double a = side * TAU / sides, b = (side + 1) * TAU / sides;
            gradient(out, 0, .025, 0, Math.cos(a) * ground, .025, Math.sin(a) * ground,
                    Math.cos(b) * ground, .025, Math.sin(b) * ground, 0, .025, 0,
                    vivid(time * .24 + side * 7.0 / sides), fade * (.10F + .32F * pulse), 0, 0, fade * (.10F + .32F * pulse));
        }
        if (flash <= .008F) return;
        int spikes = count(detail, enhanced ? 24 : 18, enhanced ? 16 : 12, enhanced ? 10 : 8);
        double snap = Math.floor(time * .5);
        // Unequal eruption blades tear out of the strike. Their open gaps keep the
        // radial blast readable instead of merging into an opaque spherical flash.
        for (int i = 0; i < spikes; i++) {
            color = vivid(time * .24 + i * .48);
            double a = i * 2.399963 + .10 * Math.sin(snap + i * 7.1);
            double reach = radius * (.65 + .72 * hash(i * 13 + 1)) * (.35 + .65 * Math.sqrt(pulse));
            double height = radius * (.22 + 1.08 * hash(i * 7 + 4)) * (.22 + .78 * Math.sqrt(pulse));
            double dx = Math.cos(a), dz = Math.sin(a);
            double sx = dx * radius * .13, sz = dz * radius * .13;
            double x = dx * reach, z = dz * reach;
            double width = radius * (.075 + .05 * hash(i + 12));
            double ox = -dz * width, oz = dx * width;
            double bend = reach * .55;
            gradient(out, sx - ox, .06, sz - oz, dx * bend - ox * .45, height * .75, dz * bend - oz * .45,
                    x, height, z, sx + ox, .06, sz + oz, color, flash * .72F, flash * .95F, 0, flash * .42F);
            gradient(out, sx - ox * .18, .07, sz - oz * .18, dx * bend, height * .70, dz * bend,
                    x, height, z, sx + ox * .18, .07, sz + oz * .18,
                    color, flash * .96F, flash * .80F, 0, flash * .96F);
            if (i % 2 == 0) {
                // Low horizontal shock needles punch beyond the damaging core.
                double needle = radius * (1.15 + .27 * hash(i * 9 + 8));
                gradient(out, dx * radius * .2 - ox * .25, .12, dz * radius * .2 - oz * .25,
                        dx * needle, .14 + radius * .08 * hash(i), dz * needle,
                        dx * radius * .2 + ox * .25, .12, dz * radius * .2 + oz * .25,
                        dx * radius * .2, .12, dz * radius * .2, vivid(i * .73 + 3.1), flash * .88F, 0, flash * .88F, flash * .88F);
            }
        }
    }

    private static void shockFronts(Sink out, float time, double radius, float[] color, float pulse,
                                    float fade, boolean enhanced, int detail) {
        if (pulse <= 0) return;
        double sinceFlash = -Math.log(pulse) / .67;
        int segments = count(detail, 28, 20, 12);
        int waves = enhanced && detail < 2 ? 3 : 2;
        for (int wave = 0; wave < waves; wave++) {
            double t = (sinceFlash - wave * 1.05) / 7;
            if (t < 0 || t >= 1) continue;
            double travel = 1 - Math.pow(1 - t, 2);
            double extent = radius * (.18 + 1.18 * travel);
            float alpha = fade * (float) Math.pow(1 - t, 1.4) * (wave == 0 ? .44F : .25F);
            for (int segment = 0; segment < segments; segment++) {
                if ((segment + wave) % 5 == 0) continue;
                color = vivid(time * .24 + wave + segment * 7.0 / segments);
                double aa = segment * TAU / segments + wave * .47, ab = (segment + .82) * TAU / segments + wave * .47;
                double rough = .95 + .06 * Math.sin(segment * 4.7 + wave);
                double ra = extent * rough, rb = extent * (.95 + .06 * Math.sin((segment + 1) * 4.7 + wave));
                double thickness = radius * (.035 + .085 * (1 - t));
                double ya = .08 + radius * .13 * (1 - t) * hash(segment * 3 + wave);
                double yb = .08 + radius * .13 * (1 - t) * hash((segment + 1) * 3 + wave);
                gradient(out, Math.cos(aa) * (ra - thickness), ya, Math.sin(aa) * (ra - thickness),
                        Math.cos(ab) * (rb - thickness), yb, Math.sin(ab) * (rb - thickness),
                        Math.cos(ab) * rb, .04, Math.sin(ab) * rb,
                        Math.cos(aa) * ra, .04, Math.sin(aa) * ra, color, alpha, alpha, 0, 0);
                if (detail < 2 && segment % 2 == 0) {
                    // A sloping, fragmented pressure skirt gives the wave vertical volume.
                    gradient(out, Math.cos(aa) * (ra - thickness), ya, Math.sin(aa) * (ra - thickness),
                            Math.cos(ab) * (rb - thickness), yb, Math.sin(ab) * (rb - thickness),
                            Math.cos(ab) * rb * .76, yb + radius * .20 * (1 - t), Math.sin(ab) * rb * .76,
                            Math.cos(aa) * ra * .76, ya + radius * .20 * (1 - t), Math.sin(aa) * ra * .76,
                            vivid(time * .24 + wave + segment * 7.0 / segments + .45), alpha * .70F, alpha * .70F, 0, 0);
                }
            }
        }
    }

    private static void arcs(Sink out, float time, double radius, float[] color, float fade, int detail) {
        int arcs = count(detail, 6, 4, 3), steps = count(detail, 7, 6, 5);
        for (int arc = 0; arc < arcs; arc++) {
            color = vivid(time * .29 + arc * .85);
            double rotation = arc * 2.399963 + time * .028;
            double extent = radius * (.84 + .18 * hash(arc + 71));
            double height = radius * (.63 + .22 * hash(arc * 9 + 2));
            double ax = 0, ay = 0, az = 0;
            float alpha = fade * (.26F + .14F * (float) Math.sin(time * 1.73 + arc));
            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps, a = t * Math.PI;
                double jagged = Math.sin(step * 9.1 + Math.floor(time * 1.5) + arc * 3.7);
                double x = Math.cos(a) * extent, sideways = Math.sin(a * 2 + arc) * radius * .10 + jagged * radius * .06;
                double bx = Math.cos(rotation) * x - Math.sin(rotation) * sideways;
                double bz = Math.sin(rotation) * x + Math.cos(rotation) * sideways;
                double by = .08 + Math.sin(a) * height * (1 + jagged * .12);
                if (step > 0) {
                    beam(out, ax, ay, az, bx, by, bz, radius * .017, color, alpha);
                    if (detail == 0 && arc % 2 == 0)
                        beam(out, ax, ay, az, bx, by, bz, radius * .004, WHITE, alpha * .85F);
                }
                ax = bx; ay = by; az = bz;
            }
        }
    }

    /** Short groundward ribbons sustain the electrical spray without repeating the blast wave. */
    private static void groundDischarges(Sink out, float time, double radius, float strength, boolean enhanced, int detail) {
        if (strength <= .001F) return;
        int rays = count(detail, enhanced ? 10 : 8, enhanced ? 7 : 6, enhanced ? 5 : 4);
        for (int ray = 0; ray < rays; ray++) {
            double phase = positiveMod(time * .24 + hash(ray * 13 + 5), 1);
            double angle = ray * 2.399963 + .15 * Math.sin(Math.floor(time * 2) + ray);
            double dx = Math.cos(angle), dz = Math.sin(angle);
            double inner = radius * (.12 + .50 * phase), outer = radius * (.65 + .70 * phase);
            double height = radius * (.06 + .36 * Math.sin(phase * Math.PI));
            double width = radius * .028 * (1 - phase), ox = -dz * width, oz = dx * width;
            float alpha = strength * (float) Math.pow(1 - phase, 1.3);
            float[] tint = vivid(time * .28 + ray * .67);
            gradient(out, dx * inner - ox, .09, dz * inner - oz,
                    dx * outer, height, dz * outer, dx * inner + ox, .09, dz * inner + oz,
                    dx * inner, .09, dz * inner, tint, alpha * .63F, 0, alpha * .63F, alpha * .78F);
            if (detail < 2)
                beam(out, dx * inner, .10, dz * inner, dx * outer, height, dz * outer,
                        radius * .005 * (1 - phase), tint, alpha * .88F);
        }
    }

    private static void shards(Sink out, float time, double radius, float[] color, float fade, boolean enhanced, int detail) {
        int sparks = count(detail, enhanced ? 30 : 24, enhanced ? 20 : 16, 10);
        for (int spark = 0; spark < sparks; spark++) {
            double t = positiveMod(time * .11 + hash(spark * 7 + 8), 1);
            color = vivid(time * .31 + t * 4 + spark * .3);
            double angle = spark * 2.399963 + .3 * Math.sin(spark * 5.1);
            double outward = radius * (.10 + (1 - Math.pow(1 - t, 1.7)) * 1.20);
            double x = Math.cos(angle) * outward, z = Math.sin(angle) * outward;
            double y = .12 + radius * (Math.sin(t * Math.PI) * (.56 + .56 * hash(spark * 19 + 2)) + .06 * t);
            float opacity = fade * (float) (Math.sin(t * Math.PI) * .90);
            double size = radius * (.015 + .015 * hash(spark + 113));
            diamond(out, x, y, z, size, Math.min(y - .025, size * (1.8 + hash(spark))), color, opacity);
            if (detail < 2) {
                double tail = radius * (.13 + .10 * hash(spark * 7 + 4));
                beam(out, x, y, z, x - Math.cos(angle) * tail, Math.max(.03, y - tail * .6), z - Math.sin(angle) * tail,
                        size * .35, color, opacity * .62F);
            }
        }
    }

    private static void diamond(Sink out, double x, double y, double z, double width, double height, float[] color, float alpha) {
        quad(out, x - width, y, z, x, y + height, z, x + width, y, z, x, y - height, z, color, alpha);
        quad(out, x, y, z - width, x, y + height, z, x, y, z + width, x, y - height, z, color, alpha);
    }

    /** Two crossed ribbons keep the small strands visible from every viewing direction. */
    private static void beam(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                             double width, float[] color, float alpha) {
        if (alpha <= .001F || width <= 0) return;
        double dx = bx - ax, dy = by - ay, dz = bz - az, length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < .000001) return;
        dx /= length; dy /= length; dz /= length;
        double sx, sy, sz;
        if (Math.abs(dy) > .9) { sx = 0; sy = dz; sz = -dy; }
        else { sx = -dz; sy = 0; sz = dx; }
        double scale = width / Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= scale; sy *= scale; sz *= scale;
        quad(out, ax - sx, ay - sy, az - sz, bx - sx, by - sy, bz - sz,
                bx + sx, by + sy, bz + sz, ax + sx, ay + sy, az + sz, color, alpha);
        double tx = dy * sz - dz * sy, ty = dz * sx - dx * sz, tz = dx * sy - dy * sx;
        quad(out, ax - tx, ay - ty, az - tz, bx - tx, by - ty, bz - tz,
                bx + tx, by + ty, bz + tz, ax + tx, ay + ty, az + tz, color, alpha);
    }

    private static void quad(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz, float[] color, float alpha) {
        gradient(out, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, color, alpha, alpha, alpha, alpha);
    }

    private static void gradient(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                                 double cx, double cy, double cz, double dx, double dy, double dz,
                                 float[] color, float aa, float ab, float ac, float ad) {
        out.vertex(ax, ay, az, color[0], color[1], color[2], aa);
        out.vertex(bx, by, bz, color[0], color[1], color[2], ab);
        out.vertex(cx, cy, cz, color[0], color[1], color[2], ac);
        out.vertex(dx, dy, dz, color[0], color[1], color[2], ad);
    }

    private static int count(int detail, int high, int middle, int low) { return detail == 0 ? high : detail == 1 ? middle : low; }
    /** Keep the shared seven-color hue sequence, with pure chroma instead of additive pastel overlap. */
    private static float[] vivid(double phase) {
        float[] color = ChaosBeamVisuals.spectral(phase);
        float low = Math.min(color[0], Math.min(color[1], color[2]));
        float high = Math.max(color[0], Math.max(color[1], color[2]));
        float range = Math.max(.001F, high - low);
        for (int i = 0; i < 3; i++)
            color[i] = (float) Math.pow((color[i] - low) / range, 1.28);
        return color;
    }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static double positiveMod(double value, double modulus) { return value - Math.floor(value / modulus) * modulus; }
    private static double hash(int seed) { return positiveMod(Math.sin(seed * 127.1 + 311.7) * 43758.5453, 1); }
    private static double[] circle(int sides) {
        double[] circle = new double[(sides + 1) * 2];
        for (int i = 0; i <= sides; i++) { circle[i * 2] = Math.cos(i * TAU / sides); circle[i * 2 + 1] = Math.sin(i * TAU / sides); }
        return circle;
    }
}
