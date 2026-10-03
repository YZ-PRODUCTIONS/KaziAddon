package net.kazi.kazimod.models.abilities;

/**
 * Ea's charge: a scarlet cyclone between three flat, rotating galaxy planes.
 * All coordinates are world-up and relative to the caster's feet, independent
 * of the direction of the attack. Streams contain quads and use straight alpha.
 */
public final class EaChargeMesh {
    public static final float MAX_RADIUS = 42.0F;
    public static final float MAX_HEIGHT = 25.0F;
    public static final float MIN_Y = EaCasterStormMesh.MIN_Y;
    public static final int GALAXY_LINGER_TICKS = 40;
    private static final int GALAXY_FADE_TICKS = 10;
    private static final float[] GALAXY_HEIGHTS = {4.5F, -0.35F, -1.10F};
    private static final float GALAXY_OPACITY = 0.80F;

    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2;
    private static final int RED = 0xED113F;
    private static final int SCARLET = 0xFF3753;
    private static final int IVORY = 0xE9E5E4;
    private static final int SILVER = 0xA9ABB5;
    private static final int ICE = 0xD7DCE7;
    private static final ThreadLocal<GalaxyScratch> GALAXY = ThreadLocal.withInitial(GalaxyScratch::new);

    private EaChargeMesh() { }

    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z, int color, float alpha);
    }

    @FunctionalInterface
    public interface TexturedSink {
        void vertex(float x, float y, float z, float u, float v, int color, float alpha);
    }

    /** Detail 0 is full, 1 reduced, 2+ distant. Age is in ticks; progress spans 0..1. */
    public static void charge(Sink body, Sink glow, TexturedSink galaxies, int detail,
                              float age, float progress, float opacity, int seed) {
        if (!Float.isFinite(age) || !Float.isFinite(progress) || !Float.isFinite(opacity)) return;
        float p = clamp(progress);
        float alpha = smooth(p / 0.035F) * clamp(opacity);
        if (alpha <= 0.001F) return;

        float awaken = smooth(p / 0.34F);
        float celestial = smooth((p - 0.29F) / 0.40F);
        float climax = smooth((p - 0.84F) / 0.16F);
        // Integrating acceleration into phase avoids a sudden speed jump late in the charge.
        float spin = age * 0.055F + 5.3F * climax * climax;
        aura(body, glow, detail, age, awaken, celestial, climax, alpha);
        winds(glow, detail, age, spin, awaken, celestial, climax, alpha, seed);
        fragments(body, glow, detail, age, awaken, celestial, climax, alpha, seed);
        EaCasterStormMesh.charge(body, glow, detail, age, p, opacity, seed);

        galaxyLayers(glow, galaxies, detail, age, p, climax, spin, alpha, seed);

        // Light runs upward between the galaxies only at the final gathering.
        if (climax > 0) {
            int steps = count(detail, 44, 30, 18);
            for (int strand = 0; strand < 4; strand++) {
                float phase = strand * TAU / 4 + spin * 1.7F;
                for (int j = 0; j < steps; j++) {
                    float u = (float) j / steps, v = (float) (j + 1) / steps;
                    float a = phase + u * TAU * 1.2F, b = phase + v * TAU * 1.2F;
                    float r = 0.42F + sin(PI * u) * 0.64F;
                    float rr = 0.42F + sin(PI * v) * 0.64F;
                    taperRibbon(glow, cos(a) * r, GALAXY_HEIGHTS[2] + u * (GALAXY_HEIGHTS[0] - GALAXY_HEIGHTS[2]), sin(a) * r,
                            cos(b) * rr, GALAXY_HEIGHTS[2] + v * (GALAXY_HEIGHTS[0] - GALAXY_HEIGHTS[2]), sin(b) * rr,
                            a, b, 0.11F * sin(PI * u), 0.11F * sin(PI * v),
                            ICE, alpha * climax * sin(PI * u) * 0.62F);
                }
            }
        }
        EaResonanceMesh.charge(body::vertex, glow::vertex, detail, age, p, opacity, seed);
    }

    /** Continue the three flat galaxies after firing, fading over the final half-second. */
    public static void lingeringGalaxies(Sink glow, TexturedSink galaxies, int detail,
                                         float releaseAge, float chargeAge, float opacity, int seed) {
        if (!Float.isFinite(releaseAge) || !Float.isFinite(chargeAge) || !Float.isFinite(opacity)
                || releaseAge < 0 || releaseAge >= GALAXY_LINGER_TICKS) return;
        float alpha = clamp(opacity) * (1 - smooth((releaseAge - (GALAXY_LINGER_TICKS - GALAXY_FADE_TICKS))
                / GALAXY_FADE_TICKS));
        if (alpha <= 0.001F) return;
        float age = chargeAge + releaseAge;
        float spin = age * 0.055F + 5.3F;
        galaxyLayers(glow, galaxies, detail, age, 1, 1, spin, alpha, seed);
    }

    private static void galaxyLayers(Sink glow, TexturedSink galaxies, int detail,
                                      float age, float progress, float climax, float spin, float alpha, int seed) {
        for (int layer = 0; layer < 3; layer++) {
            float reveal = smooth((progress - 0.30F - layer * 0.065F) / 0.15F);
            if (reveal <= 0.001F) continue;
            float direction = layer == 1 ? -1 : 1;
            float diskSpin = spin * direction * (0.75F + layer * 0.19F) + layer * 2.13F;
            float size = layer == 1 ? 4.0F : 3.0F;
            float radius = size * (6.7F + layer * 1.45F) * (0.62F + 0.38F * reveal)
                    * (1 - climax * 0.24F);
            // One overhead plane and two below the feet, centered on the cast location.
            float centerY = GALAXY_HEIGHTS[layer];
            float galaxyAlpha = alpha * reveal * GALAXY_OPACITY;
            galaxy(galaxies, detail, centerY, radius,
                    diskSpin, layer, galaxyAlpha * (0.46F + climax * 0.12F));
            galaxyWinds(glow, detail, centerY, radius,
                    diskSpin, layer, galaxyAlpha, climax);
            sparks(glow, detail, centerY, radius,
                    diskSpin, age, galaxyAlpha, seed + layer * 151);
            galaxyDust(glow, detail, centerY, radius, diskSpin, age,
                    galaxyAlpha, seed + layer * 151);
        }
    }

    private static void aura(Sink body, Sink glow, int detail, float age, float awaken,
                             float celestial, float climax, float alpha) {
        int streams = count(detail, 9, 7, 5);
        int steps = count(detail, 25, 18, 12);
        float height = 4.0F + awaken * 6.0F + celestial * 7.0F;
        for (int strand = 0; strand < streams; strand++) {
            float phase = strand * TAU / streams - age * 0.047F;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * 4.8F, b = phase + v * 4.8F;
                float r = auraRadius(u, awaken, celestial, climax, phase);
                float rr = auraRadius(v, awaken, celestial, climax, phase);
                float weight = sin(PI * u) * (0.50F + 0.50F * sin(u * 8.2F + strand) * sin(u * 8.2F + strand));
                float width = (0.32F + awaken * 0.31F) * sin(PI * u);
                mantle(body, a, b, r, rr, 0.06F + u * height, 0.06F + v * height,
                        width, 0x780E28, alpha * weight * 0.045F);
                mantle(glow, a + 0.045F, b + 0.045F, r + 0.03F, rr + 0.03F,
                        0.06F + u * height, 0.06F + v * height,
                        width * 0.90F, RED, alpha * weight * (0.10F + climax * 0.035F));
            }
        }
        // Three soft broken sweeps at ground level anchor the cyclone to the user.
        int groundSteps = count(detail, 48, 32, 20);
        for (int band = 0; band < 3; band++) {
            float r = 1.2F + band * 1.05F + awaken * 1.1F;
            for (int j = 0; j < groundSteps; j++) {
                float u = (float) j / groundSteps, v = (float) (j + 1) / groundSteps;
                float a = u * TAU * 0.72F + age * 0.035F + band * 2.0F;
                float b = v * TAU * 0.72F + age * 0.035F + band * 2.0F;
                flatBand(glow, a, b, r, 0.50F, 0.10F + band * 0.13F,
                        RED, alpha * sin(PI * u) * 0.16F);
            }
        }
    }

    private static float auraRadius(float u, float awaken, float celestial, float climax, float phase) {
        return (1.35F + awaken * 1.7F + celestial * 1.65F)
                * (0.58F + 0.85F * sin(PI * u * 0.82F))
                * (1 - climax * 0.12F) + 0.23F * sin(u * 11 + phase);
    }

    private static void winds(Sink glow, int detail, float age, float spin, float awaken,
                              float celestial, float climax, float alpha, int seed) {
        int strands = count(detail, 15, 11, 7);
        int steps = count(detail, 38, 28, 18);
        float height = 5.0F + awaken * 5.0F + celestial * 8.0F;
        for (int strand = 0; strand < strands; strand++) {
            float direction = strand % 5 == 0 ? -1 : 1;
            float phase = hash(seed + strand * 101) * TAU + spin * direction;
            float turns = 0.70F + hash(seed + strand * 59) * 0.60F;
            float verticalOffset = hash(seed + strand * 41) * 0.32F;
            boolean bright = strand % 3 != 0;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * TAU * turns, b = phase + v * TAU * turns;
                float r = windRadius(u, awaken, celestial, climax, strand);
                float rr = windRadius(v, awaken, celestial, climax, strand);
                float w = (bright ? 0.28F : 0.75F) * sin(PI * u) * (0.7F + awaken);
                float ww = (bright ? 0.28F : 0.75F) * sin(PI * v) * (0.7F + awaken);
                float pulse = 0.60F + 0.40F * sin(u * 8 + age * 0.043F + strand);
                float visibility = alpha * sin(PI * u) * (0.23F + awaken * 0.45F) * pulse;
                taperRibbon(glow, cos(a) * r, 0.22F + u * height + verticalOffset, sin(a) * r,
                        cos(b) * rr, 0.22F + v * height + verticalOffset, sin(b) * rr,
                        a, b, w, ww, bright ? IVORY : 0xA98A94, visibility * (bright ? 0.42F : 0.24F));
                // A second transparent edge gives the wind a soft flowing profile.
                if (detail < 2 && bright) {
                    taperRibbon(glow, cos(a) * r, 0.22F + u * height + verticalOffset, sin(a) * r,
                            cos(b) * rr, 0.22F + v * height + verticalOffset, sin(b) * rr,
                            a, b, w * 2.5F, ww * 2.5F, SILVER, visibility * 0.065F);
                }
            }
        }
    }

    private static float windRadius(float u, float awaken, float celestial, float climax, int strand) {
        float shape = 0.58F + 0.71F * sin(PI * u * 0.92F);
        return (2.3F + awaken * 2.2F + celestial * 1.7F + (strand % 4) * 0.29F)
                * shape * (1 - climax * 0.15F) + 0.24F * sin(u * 13 + strand);
    }

    private static void galaxy(TexturedSink out, int detail, float y,
                               float radius, float spin, int layer, float alpha) {
        int sectors = count(detail, 48, 32, 20);
        int rows = count(detail, 7, 5, 3);
        GalaxyScratch scratch = GALAXY.get();
        scratch.count = 0;
        for (int row = 0; row <= rows; row++) {
            float r = (float) row / rows;
            for (int sector = 0; sector <= sectors; sector++) {
                galaxyVertex(scratch, r, TAU * sector / sectors, y, radius, spin, layer, alpha);
            }
        }
        // Every vertex shares exactly the same Y, including the core and outer arms.
        for (int row = 0; row < rows; row++) {
            for (int j = 0; j < sectors; j++) {
                int top = row * (sectors + 1) + j, bottom = top + sectors + 1;
                scratch.emit(out, top);
                scratch.emit(out, top + 1);
                scratch.emit(out, bottom + 1);
                scratch.emit(out, bottom);
            }
        }
    }

    private static final class GalaxyScratch implements TexturedSink {
        private final float[] points = new float[8 * 49 * 6];
        private final int[] colors = new int[8 * 49];
        private int count;
        @Override public void vertex(float x, float y, float z, float u, float v, int color, float alpha) {
            int offset = count * 6;
            points[offset] = x; points[offset + 1] = y; points[offset + 2] = z;
            points[offset + 3] = u; points[offset + 4] = v; points[offset + 5] = alpha;
            colors[count++] = color;
        }
        private void emit(TexturedSink out, int index) {
            int offset = index * 6;
            out.vertex(points[offset], points[offset + 1], points[offset + 2], points[offset + 3],
                    points[offset + 4], colors[index], points[offset + 5]);
        }
    }

    private static void galaxyVertex(TexturedSink out, float r, float a, float y,
                                     float radius, float spin, int layer, float alpha) {
        float angle = a + spin + (1 - r) * 0.10F * sin(spin * 0.35F);
        float irregular = 0.91F + 0.06F * sin(a * 3 + layer * 2.7F) + 0.035F * sin(a * 5 - spin * 0.23F);
        float x = cos(angle) * r * radius * irregular, z = sin(angle) * r * radius * irregular * 0.86F;
        float edge = 1 - smooth((r - 0.83F) / 0.17F);
        int tint = layer == 1 ? 0xD9DCE2 : layer == 2 ? 0xE8E4E4 : 0xCAD0D9;
        out.vertex(x, y, z, 0.5F + cos(a) * r * 0.5F,
                0.5F + sin(a) * r * 0.5F, tint, alpha * edge);
    }

    private static void galaxyWinds(Sink out, int detail, float y, float radius,
                                    float spin, int layer,
                                    float alpha, float climax) {
        int steps = count(detail, 46, 32, 20);
        // Broken silver gusts stay in the same horizontal plane as each galaxy.
        for (int band = 0; band < 3; band++) {
            float base = spin * 1.23F + band * TAU / 3 + layer * 0.75F;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float sweep = 1.23F + band * 0.25F;
                float a = base + u * sweep, b = base + v * sweep;
                float r = radius * (0.62F + u * 0.43F + 0.065F * sin(u * 7 + band));
                float rr = radius * (0.62F + v * 0.43F + 0.065F * sin(v * 7 + band));
                float width = radius * (0.018F + 0.015F * climax) * sin(PI * u);
                float width2 = radius * (0.018F + 0.015F * climax) * sin(PI * v);
                flatRibbon(out, a, b, r, rr, width * 2.8F, width2 * 2.8F, y,
                        SILVER, alpha * sin(PI * u) * 0.11F);
                flatRibbon(out, a, b, r, rr, width, width2, y,
                        IVORY, alpha * sin(PI * u) * 0.34F);
                // A scarlet fringe swirls outside the warm rim in the original.
                if (band % 2 == 0) {
                    float edgeA = a + 0.3F, edgeB = b + 0.3F;
                    flatRibbon(out, edgeA, edgeB, r * 1.08F, rr * 1.08F,
                            radius * 0.008F * sin(PI * u), radius * 0.008F * sin(PI * v), y,
                            RED, alpha * sin(PI * u) * 0.17F);
                }
            }
        }
    }

    private static void sparks(Sink out, int detail, float y, float radius,
                               float spin, float age, float alpha, int seed) {
        int stars = count(detail, 16, 10, 5);
        for (int i = 0; i < stars; i++) {
            float phase = hash(seed + i * 179) * TAU + spin;
            float r = radius * (0.27F + hash(seed + i * 193) * 0.60F);
            float twinkle = 0.22F + 0.78F * Math.max(0, sin(age * 0.13F + i * 2.3F));
            float x = cos(phase) * r, z = sin(phase) * r;
            float size = 0.025F + twinkle * 0.055F;
            out.vertex(x - size, y, z, ICE, 0);
            out.vertex(x, y, z + size * 2.5F, ICE, alpha * twinkle * 0.72F);
            out.vertex(x + size, y, z, ICE, 0);
            out.vertex(x, y, z - size * 2.5F, ICE, alpha * twinkle * 0.72F);
        }
    }

    /** Fine comet trails drift along the arms, entirely within each flat galaxy plane. */
    private static void galaxyDust(Sink out, int detail, float y, float radius,
                                    float spin, float age, float alpha, int seed) {
        int motes = count(detail, 24, 14, 8), steps = count(detail, 4, 3, 2);
        for (int i = 0; i < motes; i++) {
            float key = hash(seed + i * 733);
            float life = fract(key + age * (0.004F + hash(seed + i * 739) * 0.003F));
            float phase = spin + (i % 3) * TAU / 3 + key * 0.7F;
            float radial = 0.22F + life * 0.65F;
            float angle = phase + (1 - radial) * 4.4F;
            float visible = alpha * sin(PI * life) * 0.48F;
            int color = i % 5 == 0 ? 0xFA5874 : i % 3 == 0 ? 0x709FE3 : ICE;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = angle - (1 - u) * 0.18F, b = angle - (1 - v) * 0.18F;
                float r = radius * (radial - (1 - u) * 0.045F);
                float rr = radius * (radial - (1 - v) * 0.045F);
                float width = radius * 0.0018F;
                out.vertex(cos(a) * (r - width * u), y, sin(a) * (r - width * u), color, visible * u * u);
                out.vertex(cos(b) * (rr - width * v), y, sin(b) * (rr - width * v), color, visible * v * v);
                out.vertex(cos(b) * (rr + width * v), y, sin(b) * (rr + width * v), color, visible * v * v);
                out.vertex(cos(a) * (r + width * u), y, sin(a) * (r + width * u), color, visible * u * u);
            }
        }
    }

    private static void flatRibbon(Sink out, float a, float b, float r, float rr,
                                   float width, float width2, float y, int color, float alpha) {
        float ca = cos(a), sa = sin(a), cb = cos(b), sb = sin(b);
        out.vertex(ca * (r - width), y, sa * (r - width), color, 0);
        out.vertex(cb * (rr - width2), y, sb * (rr - width2), color, 0);
        out.vertex(cb * rr, y, sb * rr, color, alpha);
        out.vertex(ca * r, y, sa * r, color, alpha);
        out.vertex(ca * r, y, sa * r, color, alpha);
        out.vertex(cb * rr, y, sb * rr, color, alpha);
        out.vertex(cb * (rr + width2), y, sb * (rr + width2), color, 0);
        out.vertex(ca * (r + width), y, sa * (r + width), color, 0);
    }

    private static void fragments(Sink body, Sink glow, int detail, float age, float awaken,
                                  float celestial, float climax, float alpha, int seed) {
        int count = count(detail, 29, 18, 10);
        float strength = awaken * (1 - celestial * 0.37F);
        for (int i = 0; i < count; i++) {
            float u = fract(hash(seed + i * 419) + age * (0.008F + i % 3 * 0.002F));
            float a = hash(seed + i * 431) * TAU - age * 0.085F + u * 3.2F;
            float r = (2.6F + u * 3.2F + celestial * 1.1F) * (1 - climax * 0.17F);
            float y = 0.8F + u * (6.5F + celestial * 9.0F);
            float size = (0.11F + hash(seed + i * 461) * 0.27F) * sin(PI * u);
            float x = cos(a) * r, z = sin(a) * r;
            float dx = cos(a + 0.8F) * size, dz = sin(a + 0.8F) * size;
            float visibility = alpha * strength * sin(PI * u);
            body.vertex(x - dx, y + size, z - dz, 0x1E1020, visibility * 0.67F);
            body.vertex(x + dx, y + size * 0.45F, z + dz, 0x7E2435, visibility * 0.50F);
            body.vertex(x + dx * 0.65F, y - size, z + dz * 0.65F, 0x32101E, visibility * 0.70F);
            body.vertex(x - dx * 0.65F, y - size * 0.4F, z - dz * 0.65F, 0x150C16, visibility * 0.75F);
            star(glow, x, y, z, size * 0.17F, SCARLET, visibility * 0.6F);
        }
    }

    private static void mantle(Sink out, float a, float b, float r, float rr,
                               float y, float yy, float width, int color, float alpha) {
        out.vertex(cos(a - width) * r, y, sin(a - width) * r, color, 0);
        out.vertex(cos(b - width) * rr, yy, sin(b - width) * rr, color, 0);
        out.vertex(cos(b) * rr, yy, sin(b) * rr, color, alpha);
        out.vertex(cos(a) * r, y, sin(a) * r, color, alpha);
        out.vertex(cos(a) * r, y, sin(a) * r, color, alpha);
        out.vertex(cos(b) * rr, yy, sin(b) * rr, color, alpha);
        out.vertex(cos(b + width) * rr, yy, sin(b + width) * rr, color, 0);
        out.vertex(cos(a + width) * r, y, sin(a + width) * r, color, 0);
    }

    private static void taperRibbon(Sink out, float x, float y, float z, float xx, float yy, float zz,
                                    float angle, float angle2, float width, float width2,
                                    int color, float alpha) {
        // Both vertical and radial extent make broad curved sheets, visible edge-on.
        float dx = cos(angle) * width * 0.45F, dz = sin(angle) * width * 0.45F;
        float dxx = cos(angle2) * width2 * 0.45F, dzz = sin(angle2) * width2 * 0.45F;
        out.vertex(x - dx, y - width, z - dz, color, 0);
        out.vertex(xx - dxx, yy - width2, zz - dzz, color, 0);
        out.vertex(xx, yy, zz, color, alpha);
        out.vertex(x, y, z, color, alpha);
        out.vertex(x, y, z, color, alpha);
        out.vertex(xx, yy, zz, color, alpha);
        out.vertex(xx + dxx, yy + width2, zz + dzz, color, 0);
        out.vertex(x + dx, y + width, z + dz, color, 0);
    }

    private static void flatBand(Sink out, float a, float b, float radius, float width, float y,
                                 int color, float alpha) {
        float ca = cos(a), sa = sin(a), cb = cos(b), sb = sin(b);
        out.vertex(ca * (radius - width), y, sa * (radius - width), color, 0);
        out.vertex(cb * (radius - width), y, sb * (radius - width), color, 0);
        out.vertex(cb * radius, y, sb * radius, color, alpha);
        out.vertex(ca * radius, y, sa * radius, color, alpha);
        out.vertex(ca * radius, y, sa * radius, color, alpha);
        out.vertex(cb * radius, y, sb * radius, color, alpha);
        out.vertex(cb * (radius + width), y, sb * (radius + width), color, 0);
        out.vertex(ca * (radius + width), y, sa * (radius + width), color, 0);
    }

    private static void star(Sink out, float x, float y, float z, float size, int color, float alpha) {
        out.vertex(x - size, y, z, color, 0);
        out.vertex(x, y + size * 2.5F, z, color, alpha);
        out.vertex(x + size, y, z, color, 0);
        out.vertex(x, y - size * 2.5F, z, color, alpha);
        out.vertex(x, y, z - size, color, 0);
        out.vertex(x, y + size * 2.5F, z, color, alpha);
        out.vertex(x, y, z + size, color, 0);
        out.vertex(x, y - size * 2.5F, z, color, alpha);
    }

    private static int count(int detail, int high, int medium, int low) {
        return detail == 0 ? high : detail == 1 ? medium : low;
    }

    private static float hash(int value) {
        value ^= value >>> 16; value *= 0x7feb352d; value ^= value >>> 15;
        value *= 0x846ca68b; value ^= value >>> 16;
        return (value & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private static float fract(float value) { return value - (float) Math.floor(value); }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float smooth(float value) { value = clamp(value); return value * value * (3 - 2 * value); }
    private static float sin(float value) { return (float) Math.sin(value); }
    private static float cos(float value) { return (float) Math.cos(value); }
}
