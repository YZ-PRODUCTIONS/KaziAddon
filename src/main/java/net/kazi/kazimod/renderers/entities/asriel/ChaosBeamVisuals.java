package net.kazi.kazimod.renderers.entities.asriel;

import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Violent spectral plasma, traveling eruptions and electrical fractures around the combat volume. */
public final class ChaosBeamVisuals {
    public static final float VISUAL_RADIUS_SCALE = 2F;
    private static final double TAU = Math.PI * 2;
    private static final float[] WHITE = {1, .99F, .97F};
    private static final float[][] SPECTRUM = {
            {1, .12F, .27F}, {1, .49F, .08F}, {1, .92F, .18F},
            {.19F, 1, .53F}, {.12F, .79F, 1}, {.37F, .20F, 1}, {.96F, .14F, 1}};

    private ChaosBeamVisuals() { }

    /** Includes both endpoint detonations and the cannon behind the origin. */
    public static double cullingRadius(double baseRadius) {
        return Math.max(5.0D, Math.max(0, baseRadius) * VISUAL_RADIUS_SCALE * 2.9D + .3D);
    }

    public static void render(Sink out, float age, float duration, float length, float radius,
                              boolean enhanced, int detail) {
        if (!Float.isFinite(age) || !Float.isFinite(duration) || !Float.isFinite(length)
                || !Float.isFinite(radius) || age < 0 || age >= duration || duration <= 0
                || length <= .001F || radius <= .001F) return;
        // Keep the cannon's muzzle blast unchanged; enlarge only the beam and its endpoint effects.
        float muzzleRadius = Math.min(radius, 1.15F);
        radius *= VISUAL_RADIUS_SCALE;
        detail = Math.max(0, Math.min(2, detail));
        float fade = clamp((age + .65F) / 1.25F) * clamp((duration - age) / 5F);
        float muzzlePulse = (float) Math.exp(-fract(age / (enhanced ? 2.4 : 3.4)) * 3.8);
        float pulse = (float) Math.max(Math.exp(-fract(age / (enhanced ? 1.7 : 2.2)) * 5.4),
                .62 * Math.exp(-fract((age + .7) / 3.3) * 7));
        core(out, age, length, radius, pulse, fade, enhanced, detail);
        sheaths(out, age, length, radius, pulse, fade, enhanced, detail);
        fractures(out, age, length, radius, fade, enhanced, detail);
        fronts(out, age, length, radius, fade, enhanced, detail);
        jets(out, age, length, radius, fade, enhanced, detail);
        eruptions(out, age, length, radius, fade, enhanced, detail);
        detonation(out, age, 0, muzzleRadius, muzzlePulse, fade, false, enhanced, detail);
        detonation(out, age, length, radius, pulse, fade, true, enhanced, detail);
    }

    private static void core(Sink out, float age, double length, double radius, float pulse,
                             float fade, boolean enhanced, int detail) {
        int steps = count(detail, 16, 10, 6), sides = count(detail, 8, 6, 6);
        double rotation = age * -.19;
        for (int step = 0; step < steps; step++) {
            double za = length * step / steps, zb = length * (step + 1) / steps;
            double ta = step / (double) steps, tb = (step + 1) / (double) steps;
            double ax = radius * .040 * Math.sin(ta * 31 - age * 2.8), ay = radius * .035 * Math.cos(ta * 27 - age * 2.2);
            double bx = radius * .040 * Math.sin(tb * 31 - age * 2.8), by = radius * .035 * Math.cos(tb * 27 - age * 2.2);
            double ra = radius * neck(za, radius) * (.36 + .16 * pulse + .10 * Math.sin(ta * 43 - age * 3.1));
            double rb = radius * neck(zb, radius) * (.36 + .16 * pulse + .10 * Math.sin(tb * 43 - age * 3.1));
            for (int layer = 0; layer < 2; layer++) {
                double scale = layer == 0 ? 1 : 1.65;
                float[] tint = vivid(age * .29 + ta * 7.2 + layer * .35);
                float alpha = fade * (layer == 0 ? .25F + .10F * pulse : .075F);
                for (int side = 0; side < sides; side++) {
                    double a = TAU * side / sides + rotation, b = TAU * (side + 1) / sides + rotation;
                    float facet = layer == 0 ? (float) (.65 + .35 * Math.abs(Math.cos(a + .3))) : 1;
                    quad(out, ax + Math.cos(a) * ra * scale, ay + Math.sin(a) * ra * scale, za,
                            ax + Math.cos(b) * ra * scale, ay + Math.sin(b) * ra * scale, za,
                            bx + Math.cos(b) * rb * scale, by + Math.sin(b) * rb * scale, zb,
                            bx + Math.cos(a) * rb * scale, by + Math.sin(a) * rb * scale, zb, tint, alpha * facet, alpha * facet);
                }
            }
            // This continuous inner lance gives every camera angle a solid white center, even at low detail.
            line(out, ax, ay, za, bx, by, zb, radius * (.055 + .025 * pulse), WHITE, fade * .68F, fade * .68F);
        }
        // Small axial caps keep the narrow white spine anchored to both exact clipping planes.
        double tip = radius * (.055 + .025 * pulse);
        for (int end = 0; end < 2; end++) {
            double z = end == 0 ? 0 : length;
            quad(out, -tip, 0, z, 0, tip, z, tip, 0, z, 0, -tip, z, WHITE, fade * .68F, fade * .68F);
        }
    }

    private static void sheaths(Sink out, float age, double length, double radius, float pulse,
                                float fade, boolean enhanced, int detail) {
        int strips = count(detail, enhanced ? 8 : 7, enhanced ? 6 : 5, 3);
        int steps = count(detail, 16, 10, 6);
        double snap = Math.floor(age * 1.4);
        for (int strip = 0; strip < strips; strip++) {
            double start = TAU * strip / strips - age * (enhanced ? .39 : .30);
            double twist = (strip % 2 == 0 ? 1 : -1) * (enhanced ? 6.5 : 4.8);
            for (int step = 0; step < steps; step++) {
                if (hash(strip * 37 + step * 17 + snap) > .88) continue;
                double ta = step / (double) steps, tb = (step + 1) / (double) steps;
                double a = start + ta * twist + .23 * Math.sin(step * 7.1 + snap * 2.3 + strip);
                double b = start + tb * twist + .23 * Math.sin((step + 1) * 7.1 + snap * 2.3 + strip);
                double ra = radius * neck(ta * length, radius) * (.91 + .31 * Math.sin(ta * 37 - age * 3.5 + strip) + .13 * pulse);
                double rb = radius * neck(tb * length, radius) * (.91 + .31 * Math.sin(tb * 37 - age * 3.5 + strip) + .13 * pulse);
                double wa = .11 + .12 * hash(step * 31 + strip + snap), wb = .07 + .09 * hash(step * 11 + strip + snap);
                float[] color = vivid(strip * 7.0 / strips + age * .19 + ta * 1.3);
                float alpha = fade * (.66F + .20F * pulse);
                quad(out, Math.cos(a - wa) * ra, Math.sin(a - wa) * ra, ta * length,
                        Math.cos(a + wa) * ra, Math.sin(a + wa) * ra, ta * length,
                        Math.cos(b + wb) * rb, Math.sin(b + wb) * rb, tb * length,
                        Math.cos(b - wb) * rb, Math.sin(b - wb) * rb, tb * length, color, alpha, alpha * .30F);
                if (detail < 2 && step % 4 == strip % 4) {
                    line(out, Math.cos(a - wa) * ra, Math.sin(a - wa) * ra, ta * length,
                            Math.cos(b - wb) * rb, Math.sin(b - wb) * rb, tb * length,
                            radius * .024, color, fade * .90F, fade * .05F);
                }
            }
        }
    }

    private static void fractures(Sink out, float age, double length, double radius, float fade,
                                  boolean enhanced, int detail) {
        int forks = count(detail, enhanced ? 7 : 6, enhanced ? 5 : 4, 2), steps = count(detail, 6, 4, 3);
        double snap = Math.floor(age * 2.2);
        for (int fork = 0; fork < forks; fork++) {
            double phase = fract(age * .13 + hash(fork * 13));
            double z0 = phase * length, reach = Math.min(length * .34, radius * 5);
            double theta = fork * 2.39996 + snap * .12;
            float[] color = vivid(age * .32 + fork * .85 + (enhanced ? 2 : 0));
            double ax = 0, ay = 0, az = 0;
            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps;
                double z = Math.min(length, z0 + t * reach);
                double jitter = hash(fork * 71 + step * 17 + snap * 3);
                double a = theta + (jitter - .5) * 1.35 + t * 1.2;
                double r = radius * neck(z, radius) * (1.15 + .72 * jitter) * Math.sin(.52 + t * 2.1);
                double bx = Math.cos(a) * r, by = Math.sin(a) * r;
                if (step > 0) {
                    float alpha = fade * (float) (.42 + .45 * jitter);
                    line(out, ax, ay, az, bx, by, z, radius * .044, color, alpha, alpha * .30F);
                    if (detail < 2 && step == steps / 2) {
                        double branch = a + .38, br = r + radius * neck(z, radius) * .46;
                        line(out, bx, by, z, Math.cos(branch) * br, Math.sin(branch) * br,
                                Math.min(length, z + radius * .54), radius * .014, WHITE, alpha * .72F, 0);
                    }
                }
                ax = bx; ay = by; az = z;
            }
        }
    }

    private static void fronts(Sink out, float age, double length, double radius, float fade,
                               boolean enhanced, int detail) {
        int waves = count(detail, enhanced ? 4 : 3, 2, 1), sides = count(detail, 16, 12, 8);
        for (int wave = 0; wave < waves; wave++) {
            double progress = fract(age * (enhanced ? .19 : .16) + wave / (double) waves);
            double z = progress * length;
            double r = radius * neck(z, radius) * (1.10 + .71 * Math.sin(progress * Math.PI));
            float envelope = fade * (float) Math.sin(progress * Math.PI);
            float[] color = vivid(age * .32 + progress * 5.2 + wave);
            for (int side = 0; side < sides; side++) {
                if ((side + wave) % 4 == 0) continue;
                double a = TAU * side / sides + wave * .4 + age * .11;
                double b = a + TAU / sides * .88;
                double rear = Math.max(0, z - radius * (.48 + .38 * hash(side * 17 + wave)));
                double outer = r * (1 + .18 * Math.sin(side * 2.7 + age * 1.8));
                quad(out, Math.cos(a) * outer, Math.sin(a) * outer, z,
                        Math.cos(b) * outer, Math.sin(b) * outer, z,
                        Math.cos(b) * r * .77, Math.sin(b) * r * .77, rear,
                        Math.cos(a) * r * .77, Math.sin(a) * r * .77, rear,
                        color, envelope * .62F, 0);
                if (detail < 2 && side % 3 == 0) {
                    line(out, Math.cos(a) * outer, Math.sin(a) * outer, z,
                            Math.cos(b) * outer, Math.sin(b) * outer, z,
                            radius * .022, WHITE, envelope * .70F, envelope * .12F);
                }
            }
        }
    }

    private static void jets(Sink out, float age, double length, double radius, float fade,
                             boolean enhanced, int detail) {
        int jets = count(detail, enhanced ? 21 : 18, 11, 5);
        for (int jet = 0; jet < jets; jet++) {
            double progress = fract(age * (.21 + .07 * hash(jet)) + hash(jet * 13));
            double z = progress * length, tail = Math.max(0, z - Math.min(length * .3, radius * (2.1 + 2.4 * hash(jet * 7))));
            double angle = jet * 2.39996 + age * .09;
            double r = radius * (1.0 + .93 * hash(jet * 19)) * (.6 + .4 * Math.sin(progress * Math.PI));
            double x = Math.cos(angle) * r * neck(z, radius), y = Math.sin(angle) * r * neck(z, radius);
            double tailX = Math.cos(angle) * r * neck(tail, radius) * .8;
            double tailY = Math.sin(angle) * r * neck(tail, radius) * .8;
            float[] color = vivid(age * .31 + progress * 5 + jet * .3);
            float alpha = fade * (float) Math.sin(progress * Math.PI);
            line(out, tailX, tailY, tail, x, y, z, radius * .075, color, 0, alpha * .90F);
            line(out, x * .9, y * .9, Math.max(tail, z - radius * .7), x, y, z,
                    radius * .012, WHITE, 0, alpha * .72F);
        }
    }

    /** Local explosions interrupt the ribbons with expanding colored splinters along the whole beam. */
    private static void eruptions(Sink out, float age, double length, double radius, float fade,
                                  boolean enhanced, int detail) {
        int nodes = count(detail, enhanced ? 5 : 4, 3, 2), spokes = count(detail, 7, 5, 3);
        for (int node = 0; node < nodes; node++) {
            double phase = fract(age / (enhanced ? 2.3 : 3.1) + node * .618);
            double progress = (node + .5) / nodes + .027 * Math.sin(age * 1.3 + node * 2.4);
            double z = Math.max(0, Math.min(length, progress * length));
            double size = radius * neck(z, radius), expansion = Math.sqrt(phase);
            float flash = fade * (float) Math.exp(-phase * 3.4);
            for (int spoke = 0; spoke < spokes; spoke++) {
                double seed = hash(node * 53 + spoke * 19 + Math.floor(age * .9));
                double angle = spoke * TAU / spokes + node * 1.3 + age * .16;
                double x = Math.cos(angle), y = Math.sin(angle);
                double inner = size * (.32 + .24 * seed);
                double reach = size * (.82 + 1.35 * expansion) * (.85 + .15 * seed);
                double width = size * (.065 + .11 * seed) * (1 - .55 * phase);
                double tip = Math.max(0, Math.min(length, z + size * (seed - .5) * 1.4));
                float[] color = vivid(node * 1.7 + spoke * 7.0 / spokes + age * .22);
                quad(out, x * inner - y * width, y * inner + x * width, z,
                        x * inner + y * width, y * inner - x * width, z,
                        x * reach, y * reach, tip, x * reach, y * reach, tip,
                        color, flash * .88F, 0);
                line(out, x * inner, y * inner, z, x * reach * .88, y * reach * .88, tip,
                        size * .023, color, flash, flash * .06F);
            }
        }
    }

    /** The endpoint ruptures outward in a full rainbow, with tumbling fragments instead of a white disk. */
    private static void rupture(Sink out, float age, double z, double radius, float pulse,
                                float fade, boolean enhanced, int detail) {
        int spokes = count(detail, enhanced ? 20 : 17, 11, 6), sides = count(detail, 18, 12, 8);
        double cycle = age / (enhanced ? 2.1 : 2.7);
        double phase = fract(cycle), expansion = Math.sqrt(phase);
        float flash = fade * (.18F + .82F * (float) Math.exp(-phase * 3.7));
        for (int shell = 0; shell < 2; shell++) {
            double reach = radius * (.72 + 1.65 * expansion) * (shell == 0 ? 1 : .70);
            for (int side = 0; side < sides; side++) {
                if ((side + shell) % 4 == 0) continue;
                double a = side * TAU / sides + age * (shell == 0 ? .24 : -.27);
                double b = a + TAU / sides * .72;
                double outer = reach * (.86 + .13 * hash(side * 13 + shell + Math.floor(cycle)));
                double depth = radius * (shell == 0 ? -.48 : .39);
                float[] color = vivid(side * 7.0 / sides + age * .23 + shell * .6);
                quad(out, Math.cos(a) * outer, Math.sin(a) * outer, z + depth,
                        Math.cos(b) * outer, Math.sin(b) * outer, z + depth,
                        Math.cos(b) * outer * .67, Math.sin(b) * outer * .67, z,
                        Math.cos(a) * outer * .67, Math.sin(a) * outer * .67, z,
                        color, flash * .83F, 0);
            }
        }
        for (int spoke = 0; spoke < spokes; spoke++) {
            double seed = hash(spoke * 31 + Math.floor(cycle) * 17);
            double angle = spoke * 2.399963 + age * .18;
            double x = Math.cos(angle), y = Math.sin(angle);
            double reach = radius * (1.0 + 1.55 * seed) * (.68 + .32 * expansion);
            double width = radius * (.08 + .15 * seed);
            double depth = z + radius * (seed - .5) * 1.45;
            float[] color = vivid(spoke * 7.0 / spokes + age * .23);
            float alpha = fade * (.42F + .38F * pulse);
            quad(out, x * radius * .34 - y * width, y * radius * .34 + x * width, z,
                    x * radius * .34 + y * width, y * radius * .34 - x * width, z,
                    x * reach, y * reach, depth, x * reach, y * reach, depth,
                    color, alpha, alpha * .04F);
            line(out, x * radius * .35, y * radius * .35, z,
                    x * reach * .90, y * reach * .90, depth, radius * .022,
                    color, alpha, 0);
            double shard = radius * (.045 + .065 * seed), spin = age * 1.4 + spoke;
            double px = x * reach, py = y * reach;
            double sx = Math.cos(spin) * shard, sy = Math.sin(spin) * shard;
            quad(out, px + sx, py + sy, depth, px - sy, py + sx, depth - shard,
                    px - sx, py - sy, depth, px + sy, py - sx, depth + shard,
                    color, flash * .9F, flash * .25F);
        }
    }

    private static void detonation(Sink out, float age, double z, double radius, float pulse,
                                   float fade, boolean end, boolean enhanced, int detail) {
        if (end) { rupture(out, age, z, radius, pulse, fade, enhanced, detail); return; }
        int spokes = count(detail, enhanced ? 16 : 14, 9, 6), steps = count(detail, 16, 12, 8);
        double expansion = fract(age / (end ? 4.2 : 3.4));
        double blast = radius * ((end ? 1.15 : .9) + (end ? 1.3 : 1.05) * Math.sqrt(expansion));
        float shock = fade * (.16F + .84F * (float) Math.pow(1 - expansion, 1.2));
        double tilt = age * (end ? -.19 : .15);
        // A forward compression crown at the muzzle, and a broader flared shock at the clipped endpoint.
        for (int side = 0; side < steps; side++) {
            if (side % 5 == 0) continue;
            double a = TAU * side / steps + tilt, b = a + TAU / steps * .89;
            double ra = blast * (.91 + .07 * Math.sin(side * 2.9 + age * .5));
            double rb = blast * (.91 + .07 * Math.sin((side + 1) * 2.9 + age * .5));
            double depth = radius * (.25 + .31 * hash(side * 13)) * (end ? -1 : 1);
            float[] color = spectral(age * .24 + side * .22 + (end ? 1 : 0));
            quad(out, Math.cos(a) * ra, Math.sin(a) * ra, z,
                    Math.cos(b) * rb, Math.sin(b) * rb, z,
                    Math.cos(b) * rb * .76, Math.sin(b) * rb * .76, z + depth,
                    Math.cos(a) * ra * .76, Math.sin(a) * ra * .76, z + depth,
                    color, shock * .72F, 0);
        }
        double snap = Math.floor(age * .8);
        for (int spoke = 0; spoke < spokes; spoke++) {
            double seed = hash(spoke * 31 + snap), angle = spoke * TAU / spokes + age * (end ? -.045 : .065);
            double reach = radius * ((end ? 1.1 : .80) + 1.35 * seed) * (.82 + .18 * pulse);
            double inner = radius * .28, width = radius * (.17 + .15 * seed);
            double x = Math.cos(angle), y = Math.sin(angle);
            double tipZ = z + radius * ((end ? -.25 : .28) + (seed - .5) * .7);
            float[] color = spectral(age * .24 + spoke * .48 + (enhanced ? 2 : 0));
            float alpha = fade * (.56F + .30F * pulse);
            quad(out, x * inner - y * width, y * inner + x * width, z,
                    x * inner + y * width, y * inner - x * width, z,
                    x * reach, y * reach, tipZ, x * reach, y * reach, tipZ,
                    color, alpha, .025F * alpha);
            line(out, x * inner, y * inner, z, x * reach * .83, y * reach * .83, tipZ,
                    radius * .032, WHITE, alpha * (end ? .8F : .35F), 0);
            // White-hot inner petals are broad, uneven and three dimensional, unlike thin decorative rays.
            double hotReach = radius * (.72 + .62 * seed) * (.86 + .14 * pulse);
            double hotWidth = radius * (.20 + .15 * seed);
            double hotZ = z + radius * (.24 + seed * .32) * (end ? -1 : 1);
            quad(out, -y * hotWidth, x * hotWidth, hotZ,
                    y * hotWidth, -x * hotWidth, hotZ,
                    x * hotReach, y * hotReach, tipZ,
                    x * hotReach, y * hotReach, tipZ, WHITE, fade * (.32F + .20F * pulse) * (end ? 1 : .32F), 0);
        }
        // A short, hard flash at ignition, followed by the readable spectral fractures.
        float ignition = clamp(1 - age / 4) * fade;
        if (ignition > 0) {
            float alpha = ignition * (end ? .5F : .14F);
            line(out, -radius * .85, 0, z, radius * .85, 0, z, radius * .13, WHITE, alpha, alpha);
            line(out, 0, -radius * .85, z, 0, radius * .85, z, radius * .13, WHITE, alpha, alpha);
        }
    }

    /** A narrow muzzle grows smoothly into the combat-sized beam over the first few blocks. */
    private static double neck(double z, double radius) {
        double opening = Math.min(1, .9 / radius);
        double t = Math.max(0, Math.min(1, z / Math.max(.001, radius * 2.4)));
        return opening + (1 - opening) * t * t * (3 - 2 * t);
    }

    private static int count(int detail, int high, int middle, int low) { return detail == 0 ? high : detail == 1 ? middle : low; }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static double fract(double value) { return value - Math.floor(value); }
    private static double hash(double value) { return fract(Math.sin(value * 12.9898 + 78.233) * 43758.5453); }
    /** Saturation boost is local to this beam; the palette shared with Shocker and the cannon is unchanged. */
    private static float[] vivid(double phase) {
        float[] color = spectral(phase);
        float low = Math.min(color[0], Math.min(color[1], color[2]));
        float high = Math.max(color[0], Math.max(color[1], color[2]));
        float range = Math.max(.001F, high - low);
        for (int i = 0; i < 3; i++) color[i] = (float) Math.pow((color[i] - low) / range, 1.28);
        return color;
    }
    /** Shared by Shocker Breaker so both attacks use the exact same rainbow stops and blends. */
    static float[] spectral(double phase) {
        int index = (int) Math.floor(phase);
        float f = (float) fract(phase);
        float[] a = SPECTRUM[Math.floorMod(index, SPECTRUM.length)], b = SPECTRUM[Math.floorMod(index + 1, SPECTRUM.length)];
        return new float[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
    }
    private static void vertex(Sink out, double x, double y, double z, float[] color, float alpha) {
        out.vertex(x, y, z, color[0], color[1], color[2], alpha);
    }
    private static void quad(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz,
                             float[] color, float firstAlpha, float lastAlpha) {
        vertex(out, ax, ay, az, color, firstAlpha); vertex(out, bx, by, bz, color, firstAlpha);
        vertex(out, cx, cy, cz, color, lastAlpha); vertex(out, dx, dy, dz, color, lastAlpha);
    }
    /** Crossed tapered strips remain visible from any camera angle without allocating vertices. */
    private static void line(Sink out, double ax, double ay, double az, double bx, double by, double bz,
                             double width, float[] color, float firstAlpha, float lastAlpha) {
        double dx = bx - ax, dy = by - ay, dz = bz - az, length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < .00001 || width <= 0 || firstAlpha <= 0 && lastAlpha <= 0) return;
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
}
