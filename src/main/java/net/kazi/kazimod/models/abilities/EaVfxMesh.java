package net.kazi.kazimod.models.abilities;

/**
 * Ea's untextured, world-space geometry. Local +Z is the attack direction.
 * Keeping this independent of the client makes its bounds and vertex budgets testable.
 */
public final class EaVfxMesh {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2.0F;
    private static final int RED = 0xEB1741;
    private static final int SCARLET = 0xFF3851;
    private static final int IVORY = 0xFFF0D5;
    private static final int GOLD = 0xC89B49;
    private static final int GOLD_LIGHT = 0xFFE5A0;
    private static final int DARK = 0x140B19;

    private EaVfxMesh() { }

    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z, int color, float alpha);
    }

    public static void charge(Sink body, Sink glow, int detail, float age,
                              float progress, float opacity, int seed) {
        if (!Float.isFinite(age) || !Float.isFinite(progress) || !Float.isFinite(opacity)) return;
        float p = clamp(progress);
        float appear = smooth(p / 0.07F) * clamp(opacity);
        if (appear <= 0.001F) return;
        float power = smooth((p - 0.12F) / 0.82F);
        weapon(body, glow, detail, age, p, appear);

        int ringSteps = count(detail, 64, 40, 24);
        // Three celestial planes turn independently around the three blade segments.
        // Their centers stay empty, so the caster and weapon remain readable.
        for (int plane = 0; plane < 3; plane++) {
            float activation = smooth((p - 0.12F - plane * 0.13F) / 0.18F);
            if (activation <= 0.001F) continue;
            float direction = plane == 1 ? -1 : 1;
            float spin = age * (0.017F + plane * 0.006F) * direction;
            float radius = 1.5F + plane * 0.64F + power * 1.0F;
            Sink orbit = rotated(glow, 0.34F + plane * 0.76F, plane * 0.61F,
                    spin * 0.45F, 0, 0, 1.45F);
            ring(orbit, radius, 0.016F + power * 0.015F, 0, spin, ringSteps,
                    IVORY, appear * activation * 0.5F, false);
            ring(orbit, radius + 0.11F, 0.045F, 0, -spin, ringSteps,
                    RED, appear * activation * 0.75F, true);
            ring(orbit, radius - 0.16F, 0.011F, 0, spin, ringSteps,
                    SCARLET, appear * activation * 0.5F, true);
            int glyphs = count(detail, 18, 12, 8);
            for (int g = 0; g < glyphs; g++) {
                float angle = TAU * g / glyphs + spin;
                float ca = cos(angle), sa = sin(angle);
                float size = 0.11F + (g % 3 == 0 ? 0.06F : 0);
                line(orbit, ca * (radius + 0.23F), sa * (radius + 0.23F), 0,
                        ca * (radius + 0.23F + size), sa * (radius + 0.23F + size), 0,
                        0.013F, g % 3 == 0 ? GOLD_LIGHT : RED, appear * activation * 0.7F);
                if (g % 3 == 0) diamond(orbit, ca * radius, sa * radius, 0,
                        0.065F, IVORY, appear * activation * 0.85F);
            }
        }

        int strands = count(detail, 14, 9, 5);
        int steps = count(detail, 22, 16, 10);
        for (int i = 0; i < strands; i++) {
            float base = hash(seed + i * 71) * TAU + age * 0.011F;
            float start = 3.7F + hash(seed + i * 13) * 2.2F;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = base + u * 3.9F - age * (0.025F + 0.05F * power);
                float b = base + v * 3.9F - age * (0.025F + 0.05F * power);
                float r = start * (1 - u) + 0.25F, rr = start * (1 - v) + 0.25F;
                float pulse = 0.25F + 0.75F * smooth((sin(u * 12 - age * 0.14F + i) + 1) * 0.5F);
                line(glow, cos(a) * r, sin(a) * r, -2.1F + u * 4.4F,
                        cos(b) * rr, sin(b) * rr, -2.1F + v * 4.4F,
                        (0.015F + power * 0.026F) * sin(PI * u),
                        i % 4 == 0 ? IVORY : SCARLET, appear * power * pulse * 0.5F);
            }
        }

        cracks(body, glow, detail, age, power, appear, seed);
        int motes = count(detail, 32, 18, 9);
        for (int i = 0; i < motes; i++) {
            float cycle = fract(hash(seed + i * 331) + age * (0.003F + power * 0.008F));
            float a = hash(seed + i * 397) * TAU + cycle * 2.5F;
            float r = (1 - cycle) * (4.2F + power * 1.8F);
            diamond(glow, cos(a) * r, sin(a) * r, -1.8F + cycle * 4.2F,
                    0.025F + cycle * 0.065F, i % 3 == 0 ? GOLD_LIGHT : IVORY,
                    appear * power * sin(PI * cycle) * 0.8F);
        }
        // A restrained final ignition around the tip instead of an opaque sphere.
        float ignition = smooth((p - 0.78F) / 0.22F);
        if (ignition > 0) {
            for (int i = 0; i < 3; i++) {
                Sink flare = rotated(glow, i * 0.83F, i * 0.67F, age * 0.022F, 0, 0, 2.6F);
                ring(flare, 0.37F + ignition * 0.22F, 0.022F, 0, age * 0.09F,
                        ringSteps / 2, IVORY, ignition * appear * 0.65F, false);
            }
        }
    }

    /** collapseAge is negative until the release begins fading. */
    public static void release(Sink body, Sink glow, int detail, float age, float length,
                               float radius, float collapseAge, float opacity, int seed) {
        if (!Float.isFinite(age) || !Float.isFinite(length) || !Float.isFinite(radius)
                || !Float.isFinite(collapseAge) || !Float.isFinite(opacity)) return;
        float alpha = clamp(opacity);
        if (alpha <= 0.001F || length <= 0 || radius <= 0) return;
        float opening = smooth(age / 6.0F);
        float beamLength = Math.max(0.01F, length);
        int steps = count(detail, 72, 48, 28);
        int ribbons = count(detail, 9, 6, 4);


        // Torn, counter-rotating dark sheets are bounded by the actual damage radius.
        // Wide gaps between them keep nearby players and the world visible.
        for (int strand = 0; strand < ribbons; strand++) {
            float base = TAU * strand / ribbons + hash(seed + strand * 43) * 0.24F;
            float direction = strand % 2 == 0 ? 1 : -1;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = base + u * 11.0F + age * 0.08F * direction;
                float b = base + v * 11.0F + age * 0.08F * direction;
                float r = envelope(u, radius), rr = envelope(v, radius);
                float taper = (0.34F + 0.66F * sin(PI * u)) * opening;
                float jag = 0.72F + 0.15F * sin(u * 61 + strand * 2.2F - age * 0.11F);
                float jag2 = 0.72F + 0.15F * sin(v * 61 + strand * 2.2F - age * 0.11F);
                float band = 0.10F + 0.085F * sin(PI * u);
                ribbon(body, a, b, r * jag, rr * jag2, u * beamLength, v * beamLength,
                        band * direction, DARK, alpha * taper * 0.75F);
                ribbon(glow, a, b, r * (jag + 0.045F), rr * (jag2 + 0.045F),
                        u * beamLength, v * beamLength, 0.115F * direction,
                        strand % 3 == 0 ? SCARLET : RED, alpha * taper * 0.62F);
                // A translucent red mantle gives the rupture volume behind its hot seams.
                ribbon(glow, a - 0.06F, b - 0.06F, r * 0.64F, rr * 0.64F,
                        u * beamLength, v * beamLength, 0.24F * direction,
                        RED, alpha * taper * 0.16F);
                // Bright exposed edges of each dimensional tear.
                line(glow, cos(a) * r * jag, sin(a) * r * jag, u * beamLength,
                        cos(b) * rr * jag2, sin(b) * rr * jag2, v * beamLength,
                        (0.035F + radius * 0.011F) * taper,
                        strand % 3 == 0 ? IVORY : SCARLET, alpha * taper * 0.78F);
            }
        }

        // Twisting ivory filaments remain around the new continuous central core.
        for (int strand = 0; strand < 3; strand++) {
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = strand * TAU / 3 + u * 16 - age * 0.19F;
                float b = strand * TAU / 3 + v * 16 - age * 0.19F;
                float r = envelope(u, radius) * 0.16F, rr = envelope(v, radius) * 0.16F;
                ribbon(glow, a, b, r, rr, u * beamLength, v * beamLength,
                        0.3F, strand == 0 ? IVORY : 0xFFADA1, alpha * opening * 0.8F);
            }
        }

        // Traveling segmented shock rings make the enormous length legible from the side.
        int bands = count(detail, 8, 6, 4);
        int ringSteps = count(detail, 56, 36, 24);
        for (int i = 0; i < bands; i++) {
            float u = fract((float) i / bands + age * 0.009F);
            float r = envelope(u, radius) * (0.88F + 0.07F * sin(age * 0.08F + i));
            float bandAlpha = alpha * opening * sin(PI * u);
            ring(glow, r, 0.08F + radius * 0.009F, u * beamLength,
                    age * 0.07F * (i % 2 == 0 ? 1 : -1), ringSteps, RED, bandAlpha * 0.52F, true);
            ring(glow, r * 0.95F, 0.021F + radius * 0.002F, u * beamLength - 0.16F,
                    -age * 0.055F, ringSteps, IVORY, bandAlpha * 0.5F, true);
        }

        // A readable leading crown tracks the same advancing end as the hit volume.
        float crown = radius * 0.62F;
        ring(glow, crown, Math.max(0.06F, radius * 0.028F), beamLength,
                age * 0.05F, ringSteps, SCARLET, alpha * opening * 0.55F, true);
        ring(glow, crown * 0.92F, 0.035F, beamLength - 0.14F,
                -age * 0.07F, ringSteps, IVORY, alpha * opening * 0.6F, false);
        int splinters = count(detail, 34, 20, 10);
        for (int i = 0; i < splinters; i++) {
            float u = fract(hash(seed + i * 613) + age * 0.018F);
            float a = hash(seed + i * 271) * TAU + age * 0.012F;
            float r = envelope(u, radius) * (0.88F + hash(seed + i * 73) * 0.12F);
            float x = cos(a) * r, y = sin(a) * r, z = u * beamLength;
            float size = 0.09F + hash(seed + i * 17) * 0.22F;
            shard(body, glow, x, y, z, size, a + age * 0.027F,
                    alpha * opening * sin(PI * u));
        }

        // The launch shock is a brief annular bloom, never a camera-covering disc.
        float launch = clamp(age / 15.0F);
        if (launch < 1) {
            ring(glow, 0.5F + smooth(launch) * 9.0F, 0.12F * (1 - launch) + 0.025F,
                    0.4F, age * 0.035F, ringSteps, IVORY,
                    alpha * sin(PI * launch) * 0.65F, false);
            ring(glow, 0.5F + smooth(launch) * 7.4F, 0.22F * (1 - launch),
                    0.7F, -age * 0.055F, ringSteps, RED,
                    alpha * sin(PI * launch) * 0.5F, true);
        }
        if (collapseAge >= 0) terminalBloom(glow, detail, collapseAge, beamLength, radius, alpha, seed);
        EaResonanceMesh.release(body, glow, detail, age, beamLength, radius, collapseAge, opacity, seed);
    }

    public static void weapon(Sink body, Sink glow, int detail, float age, float power, float alpha) {
        EaSwordMesh.draw(body, glow, detail, age, power, alpha);
    }

    private static void cracks(Sink body, Sink glow, int detail, float age,
                               float power, float alpha, int seed) {
        float emerge = smooth((power - 0.26F) / 0.7F);
        if (emerge <= 0) return;
        int cracks = count(detail, 13, 9, 5);
        for (int i = 0; i < cracks; i++) {
            float a = hash(seed + i * 193) * TAU + age * 0.003F;
            float radius = 2.9F + hash(seed + i * 197) * 3.3F;
            float z = -1 + hash(seed + i * 199) * 5.0F;
            float max = 0.5F + hash(seed + i * 211) * 2.2F;
            float shown = smooth(clamp(emerge * 1.7F - hash(seed + i * 223) * 0.7F));
            float x = cos(a) * radius, y = sin(a) * radius;
            for (int j = 0; j < 4; j++) {
                float segment = max * shown / 4;
                float angle = a + (hash(seed + i * 227 + j * 239) - 0.5F) * 1.7F;
                float xx = x + cos(angle) * segment, yy = y + sin(angle) * segment;
                line(body, x, y, z, xx, yy, z + 0.08F, 0.044F, DARK, alpha * shown * 0.8F);
                line(glow, x, y, z + 0.022F, xx, yy, z + 0.102F,
                        0.015F, SCARLET, alpha * shown * 0.75F);
                if (j == 2) {
                    line(glow, x, y, z, x + cos(angle + 1.2F) * segment * 0.75F,
                            y + sin(angle + 1.2F) * segment * 0.75F, z - 0.2F,
                            0.01F, IVORY, alpha * shown * 0.65F);
                }
                x = xx; y = yy; z += 0.08F;
            }
        }
    }

    private static void terminalBloom(Sink glow, int detail, float age, float length,
                                      float radius, float alpha, int seed) {
        float t = clamp(age / 30);
        float bloom = smooth(t / 0.65F);
        int steps = count(detail, 80, 48, 28);
        float r = radius * (0.64F + 1.4F * bloom);
        float visible = alpha * smooth(age / 2.0F);
        Sink front = rotated(glow, 0, 0, age * 0.011F, 0, 0, length);
        ring(front, r, 0.08F + (1 - t) * 0.13F, 0, age * 0.025F,
                steps, IVORY, visible * 0.8F, false);
        ring(front, r * 0.86F, 0.16F, 0.18F, -age * 0.045F,
                steps, RED, visible * 0.75F, true);
        // Two tilted rings form a globe of fractured space around the terminal blast.
        for (int i = 0; i < 2; i++) {
            Sink orbit = rotated(front, 0.72F + i * 0.76F, 0.45F + i * 0.68F,
                    -age * 0.025F, 0, 0, 0);
            ring(orbit, r * 0.84F, 0.035F + (1 - t) * 0.025F, 0,
                    i * 1.8F, steps, SCARLET, visible * 0.6F, true);
        }
        int rays = count(detail, 24, 16, 10);
        for (int i = 0; i < rays; i++) {
            float a = TAU * i / rays + hash(seed + i * 17) * 0.13F;
            float inner = r * (0.30F + hash(seed + i * 19) * 0.25F);
            float outer = r * (0.80F + hash(seed + i * 23) * 0.35F);
            line(front, cos(a) * inner, sin(a) * inner, (1 - t) * -1.0F,
                    cos(a) * outer, sin(a) * outer, (1 - t) * 0.8F,
                    0.025F + (1 - t) * 0.075F, i % 3 == 0 ? IVORY : SCARLET,
                    visible * (1 - t) * 0.8F);
            diamond(front, cos(a) * outer, sin(a) * outer, 0,
                    0.055F + (1 - t) * 0.09F, IVORY, visible * 0.7F);
        }
    }

    private static void cylinder(Sink out, float z0, float z1, float r0, float r1,
                                 float rotation, int sides, int color, float alpha, boolean shaded) {
        for (int i = 0; i < sides; i++) {
            float a = TAU * i / sides + rotation, b = TAU * (i + 1) / sides + rotation;
            int tint = shaded ? shade(color, 0.65F + 0.35F * (cos(a - 0.7F) + 1) * 0.5F) : color;
            out.vertex(cos(a) * r0, sin(a) * r0, z0, tint, alpha);
            out.vertex(cos(b) * r0, sin(b) * r0, z0, tint, alpha);
            out.vertex(cos(b) * r1, sin(b) * r1, z1, tint, alpha);
            out.vertex(cos(a) * r1, sin(a) * r1, z1, tint, alpha);
        }
    }

    private static void ring(Sink out, float radius, float width, float z, float rotation,
                             int steps, int color, float alpha, boolean segmented) {
        if (alpha <= 0.001F || radius <= 0 || width <= 0) return;
        for (int i = 0; i < steps; i++) {
            if (segmented && i % 8 >= 5) continue;
            float a = TAU * i / steps + rotation, b = TAU * (i + 1) / steps + rotation;
            out.vertex(cos(a) * (radius - width), sin(a) * (radius - width), z, color, alpha);
            out.vertex(cos(b) * (radius - width), sin(b) * (radius - width), z, color, alpha);
            out.vertex(cos(b) * (radius + width), sin(b) * (radius + width), z, color, alpha);
            out.vertex(cos(a) * (radius + width), sin(a) * (radius + width), z, color, alpha);
        }
    }

    private static void ribbon(Sink out, float a, float b, float r, float rr,
                               float z, float zz, float angularWidth, int color, float alpha) {
        out.vertex(cos(a - angularWidth) * r, sin(a - angularWidth) * r, z, color, alpha);
        out.vertex(cos(b - angularWidth) * rr, sin(b - angularWidth) * rr, zz, color, alpha);
        out.vertex(cos(b + angularWidth) * rr, sin(b + angularWidth) * rr, zz, color, alpha);
        out.vertex(cos(a + angularWidth) * r, sin(a + angularWidth) * r, z, color, alpha);
    }

    private static void line(Sink out, float x, float y, float z, float xx, float yy, float zz,
                             float width, int color, float alpha) {
        if (width <= 0.00001F || alpha <= 0.001F) return;
        float dx = xx - x, dy = yy - y, dz = zz - z;
        float nx = -dy, ny = dx;
        float n = (float) Math.sqrt(nx * nx + ny * ny);
        if (n < 0.00001F) { nx = 1; ny = 0; n = 1; }
        nx *= width / n; ny *= width / n;
        lineQuad(out, x, y, z, xx, yy, zz, nx, ny, 0, color, alpha);
        float bx = -dz * ny, by = dz * nx, bz = dx * ny - dy * nx;
        float d = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        if (d > 0.00001F) lineQuad(out, x, y, z, xx, yy, zz,
                bx * width / d, by * width / d, bz * width / d, color, alpha);
    }

    private static void lineQuad(Sink out, float x, float y, float z, float xx, float yy, float zz,
                                 float nx, float ny, float nz, int color, float alpha) {
        out.vertex(x + nx, y + ny, z + nz, color, alpha);
        out.vertex(xx + nx, yy + ny, zz + nz, color, alpha);
        out.vertex(xx - nx, yy - ny, zz - nz, color, alpha);
        out.vertex(x - nx, y - ny, z - nz, color, alpha);
    }

    private static void diamond(Sink out, float x, float y, float z, float size, int color, float alpha) {
        out.vertex(x - size, y, z, color, alpha);
        out.vertex(x, y + size * 1.9F, z, color, alpha);
        out.vertex(x + size, y, z, color, alpha);
        out.vertex(x, y - size * 1.9F, z, color, alpha);
        out.vertex(x, y, z - size, color, alpha);
        out.vertex(x, y + size * 1.9F, z, color, alpha);
        out.vertex(x, y, z + size, color, alpha);
        out.vertex(x, y - size * 1.9F, z, color, alpha);
    }

    private static void shard(Sink body, Sink glow, float x, float y, float z,
                              float size, float rotation, float alpha) {
        float dx = cos(rotation) * size, dy = sin(rotation) * size;
        body.vertex(x - dy, y + dx, z, 0x1A0C20, alpha * 0.7F);
        body.vertex(x + dx, y + dy, z + size * 3.5F, 0x3D1427, alpha * 0.6F);
        body.vertex(x + dy, y - dx, z, 0x120C1A, alpha * 0.7F);
        body.vertex(x - dx, y - dy, z - size * 1.5F, 0x43152F, alpha * 0.6F);
        line(glow, x - dy, y + dx, z, x + dx, y + dy, z + size * 3.5F,
                0.014F + size * 0.04F, SCARLET, alpha * 0.7F);
    }

    private static Sink rotated(Sink out, float rx, float ry, float rz, float ox, float oy, float oz) {
        float sx = sin(rx), cx = cos(rx), sy = sin(ry), cy = cos(ry), sz = sin(rz), cz = cos(rz);
        return (x, y, z, color, alpha) -> {
            float yy = y * cx - z * sx, zz = y * sx + z * cx;
            float xx = x * cy + zz * sy; zz = -x * sy + zz * cy;
            out.vertex(xx * cz - yy * sz + ox, xx * sz + yy * cz + oy, zz + oz, color, alpha);
        };
    }

    private static float envelope(float u, float radius) {
        return radius * (0.18F + 0.82F * (float) Math.sqrt(Math.max(0, sin(PI * u * 0.87F))));
    }

    private static int count(int detail, int high, int medium, int low) {
        return detail == 0 ? high : detail == 1 ? medium : low;
    }

    private static int shade(int color, float scale) {
        return ((int) ((color >> 16 & 255) * scale) << 16)
                | ((int) ((color >> 8 & 255) * scale) << 8) | (int) ((color & 255) * scale);
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
