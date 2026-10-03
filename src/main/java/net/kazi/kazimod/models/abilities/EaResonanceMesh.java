package net.kazi.kazimod.models.abilities;

/** Additional Enuma Elish layers: starfall, torn space, blade ignition and blast pressure.
 * Charge coordinates are world-up; blade/release coordinates use local +Z forward.
 */
public final class EaResonanceMesh {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2;
    private static final int RED = 0xF32449;
    private static final int WHITE = 0xFFF1E9;
    private static final int BLUE = 0xA8CFFF;
    private static final int GOLD = 0xFFE7AB;
    private static final int DARK = 0x160E1B;

    private EaResonanceMesh() { }

    public static void charge(EaVfxMesh.Sink body, EaVfxMesh.Sink glow, int detail,
                              float age, float progress, float opacity, int seed) {
        if (!finite(age, progress, opacity)) return;
        float p = clamp(progress), alpha = clamp(opacity) * smooth(p / 0.12F);
        if (alpha <= 0.001F) return;
        float power = smooth((p - 0.15F) / 0.85F);
        // Small stars occupy the space between the existing galaxy planes.
        int stars = count(detail, 64, 38, 20);
        for (int i = 0; i < stars; i++) {
            float key = hash(seed + i * 503);
            float t = fract(key + age * (0.003F + hash(seed + i * 509) * 0.003F));
            float a = hash(seed + i * 521) * TAU + age * 0.012F + t * 1.5F;
            float r = 4.7F + hash(seed + i * 523) * 8.0F - power * t;
            float x = cos(a) * r, y = -5.8F + t * 20.0F, z = sin(a) * r;
            float visible = alpha * sin(PI * t) * (0.45F + 0.55F * power);
            float size = (0.035F + hash(seed + i * 541) * 0.055F) * (0.7F + power * 0.6F);
            int color = i % 5 == 0 ? GOLD : i % 3 == 0 ? BLUE : WHITE;
            star(glow, x, y, z, size, size * 3.4F, color, visible * 0.8F);
            if (i % 7 == 0) {
                stroke(glow, x, Math.max(-7.8F, y - (0.6F + power * 2.0F)), z, x, y, z,
                        0.015F, color, visible * 0.4F);
            }
        }

        // The reference's black scythe-shaped streaks cut through the red storm.
        // Open strips and low opacity preserve the player and the underlying winds.
        int strands = count(detail, 6, 4, 3), steps = count(detail, 22, 15, 10);
        for (int i = 0; i < strands; i++) {
            float phase = hash(seed + i * 547) * TAU - age * (0.055F + i * 0.006F);
            float base = 4.2F + hash(seed + i * 557) * 2.4F;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * 5.0F, b = phase + v * 5.0F;
                float r = base + sin(u * PI) * 1.2F, rr = base + sin(v * PI) * 1.2F;
                float y = -5.7F + u * 13.5F, yy = -5.7F + v * 13.5F;
                float w = sin(PI * u) * (0.13F + power * 0.14F);
                float ww = sin(PI * v) * (0.13F + power * 0.14F);
                body.vertex(cos(a - w) * r, y - w, sin(a - w) * r, DARK, 0);
                body.vertex(cos(b - ww) * rr, yy - ww, sin(b - ww) * rr, DARK, 0);
                body.vertex(cos(b) * rr, yy, sin(b) * rr, DARK, alpha * power * 0.56F);
                body.vertex(cos(a) * r, y, sin(a) * r, DARK, alpha * power * 0.56F);
                stroke(glow, cos(a) * r, y, sin(a) * r, cos(b) * rr, yy, sin(b) * rr,
                        0.025F * sin(PI * u), RED, alpha * power * 0.42F);
            }
        }

        int fragments = count(detail, 24, 15, 8);
        for (int i = 0; i < fragments; i++) {
            float t = fract(hash(seed + i * 563) + age * 0.005F);
            float a = hash(seed + i * 569) * TAU - age * 0.035F - t * 2.0F;
            float r = 5.3F + hash(seed + i * 571) * 4.0F;
            fragment(body, glow, cos(a) * r, -5.2F + t * 14.0F, sin(a) * r,
                    0.09F + hash(seed + i * 577) * 0.21F, a + age * 0.07F,
                    alpha * power * sin(PI * t));
        }
    }

    /** Follows the sword as it rises and aims; it never changes the sword mesh. */
    public static void blade(EaVfxMesh.Sink glow, int detail, float age,
                             float progress, float opacity) {
        if (!finite(age, progress, opacity)) return;
        float p = clamp(progress), power = smooth((p - 0.18F) / 0.82F);
        float alpha = clamp(opacity) * power;
        if (alpha <= 0.001F) return;
        int steps = count(detail, 28, 18, 12);
        for (int strand = 0; strand < 3; strand++) {
            float direction = strand == 1 ? -1 : 1;
            float phase = strand * TAU / 3 + direction * (age * 0.10F + p * p * 7.0F);
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * 8.8F, b = phase + v * 8.8F;
                float r = 0.26F + sin(PI * u) * (0.30F + power * 0.22F);
                float rr = 0.26F + sin(PI * v) * (0.30F + power * 0.22F);
                float visible = alpha * sin(PI * u);
                stroke(glow, cos(a) * r, sin(a) * r, 0.15F + u * 2.9F,
                        cos(b) * rr, sin(b) * rr, 0.15F + v * 2.9F,
                        0.07F, RED, visible * 0.22F);
                stroke(glow, cos(a) * r, sin(a) * r, 0.15F + u * 2.9F,
                        cos(b) * rr, sin(b) * rr, 0.15F + v * 2.9F,
                        0.018F, strand == 1 ? WHITE : RED, visible * 0.65F);
            }
        }
        float ignition = smooth((p - 0.67F) / 0.33F);
        float pulse = 0.88F + 0.12F * sin(age * 0.16F);
        star(glow, 0, 0, 3.12F, 0.12F + ignition * 0.24F,
                0.42F + ignition * 1.05F, RED, alpha * pulse * 0.8F);
        star(glow, 0, 0, 3.13F, 0.045F + ignition * 0.09F,
                0.2F + ignition * 0.60F, WHITE, alpha * pulse * 0.9F);
        for (int i = 0; i < count(detail, 8, 6, 4); i++) {
            float a = i * TAU / count(detail, 8, 6, 4) + age * 0.018F;
            float r = 0.5F + ignition * 1.1F;
            stroke(glow, cos(a) * r, sin(a) * r, 2.75F,
                    cos(a) * r * 0.2F, sin(a) * r * 0.2F, 3.18F,
                    0.02F, i % 3 == 0 ? WHITE : RED, alpha * ignition * 0.48F);
        }
        // Small broken arcs articulate the three independently rotating drums.
        int collarSteps = count(detail, 14, 10, 6);
        for (int drum = 0; drum < 3; drum++) {
            float direction = drum == 1 ? -1 : 1;
            float phase = age * 0.17F * direction + drum * 1.7F;
            for (int arc = 0; arc < 2; arc++) for (int j = 0; j < collarSteps; j++) {
                float u = (float) j / collarSteps, v = (float) (j + 1) / collarSteps;
                float a = phase + arc * PI + u * 2.1F, b = phase + arc * PI + v * 2.1F;
                float r = EaSwordMesh.DRUM_RADIUS + 0.05F + power * 0.05F, z = 0.15F + drum * 0.89F;
                stroke(glow, cos(a) * r, sin(a) * r, z + sin(a) * 0.055F,
                        cos(b) * r, sin(b) * r, z + sin(b) * 0.055F,
                        0.012F, drum == 1 ? GOLD : RED, alpha * sin(PI * u) * 0.64F);
            }
        }
    }

    public static void release(EaVfxMesh.Sink body, EaVfxMesh.Sink glow, int detail,
                               float age, float length, float radius,
                               float collapseAge, float opacity, int seed) {
        if (!finite(age, length, radius) || !Float.isFinite(collapseAge) || !Float.isFinite(opacity)
                || length <= 0 || radius <= 0) return;
        float alpha = clamp(opacity) * smooth(age / 6.0F);
        if (alpha <= 0.001F) return;
        // Branching bolts skim the red rupture instead of covering its dark center.
        int bolts = count(detail, 5, 3, 2), steps = count(detail, 24, 16, 10);
        for (int i = 0; i < bolts; i++) {
            float phase = hash(seed + i * 593) * TAU + age * 0.045F;
            float pulse = 0.62F + 0.38F * sin(age * 0.16F + i * 2.1F);
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * 7.0F + (hash(seed + i * 599 + j * 601) - 0.5F) * 0.55F;
                float b = phase + v * 7.0F + (hash(seed + i * 599 + (j + 1) * 601) - 0.5F) * 0.55F;
                float r = envelope(u, radius) * 0.9F, rr = envelope(v, radius) * 0.9F;
                float visible = alpha * pulse * sin(PI * u);
                float x = cos(a) * r, y = sin(a) * r, xx = cos(b) * rr, yy = sin(b) * rr;
                stroke(glow, x, y, u * length, xx, yy, v * length, 0.11F, RED, visible * 0.24F);
                stroke(glow, x, y, u * length, xx, yy, v * length, 0.025F,
                        i % 2 == 0 ? WHITE : BLUE, visible * 0.67F);
                if (j % 5 == 2) stroke(glow, x, y, u * length,
                        cos(a + 0.20F) * r * 0.73F, sin(a + 0.20F) * r * 0.73F,
                        Math.min(1, u + 0.06F) * length, 0.018F, RED, visible * 0.68F);
            }
        }

        // Sweeping pressure crescents race down the beam with broad feathered tails.
        int waves = count(detail, 4, 3, 2), arcSteps = count(detail, 28, 18, 12);
        for (int i = 0; i < waves; i++) {
            float t = fract(age * 0.021F + (float) i / waves);
            float z = length * (0.08F + t * 0.83F);
            float r = envelope(z / length, radius) * (0.68F + 0.25F * t);
            float tail = Math.min(length * 0.07F, 5.0F);
            float phase = age * 0.06F + i * 2.4F;
            for (int j = 0; j < arcSteps; j++) {
                float u = (float) j / arcSteps, v = (float) (j + 1) / arcSteps;
                float a = phase + u * 4.0F, b = phase + v * 4.0F;
                float visible = alpha * sin(PI * t) * sin(PI * u);
                glow.vertex(cos(a) * r * 0.68F, sin(a) * r * 0.68F, z - tail, RED, 0);
                glow.vertex(cos(b) * r * 0.68F, sin(b) * r * 0.68F, z - tail, RED, 0);
                glow.vertex(cos(b) * r, sin(b) * r, z, RED, visible * 0.20F);
                glow.vertex(cos(a) * r, sin(a) * r, z, RED, visible * 0.20F);
                stroke(glow, cos(a) * r, sin(a) * r, z, cos(b) * r, sin(b) * r, z,
                        0.055F, WHITE, visible * 0.46F);
            }
        }

        int fragments = count(detail, 34, 20, 10);
        for (int i = 0; i < fragments; i++) {
            float t = fract(hash(seed + i * 607) + age * 0.018F);
            float a = hash(seed + i * 613) * TAU + age * 0.024F;
            float r = envelope(t, radius) * 0.86F;
            float size = Math.min(radius * 0.045F, length * 0.03F)
                    * (0.5F + hash(seed + i * 617)) * sin(PI * t);
            fragment(body, glow, cos(a) * r, sin(a) * r, t * length, size,
                    a + age * 0.10F, alpha * sin(PI * t));
        }
        releaseMotes(glow, detail, age, length, radius, alpha, seed);
        if (age < 18) shockFront(glow, detail, age / 18, Math.min(length * 0.08F, 2.2F),
                radius, alpha, seed, false);
        if (collapseAge >= 0) {
            nova(body, glow, detail, collapseAge, length, radius, alpha, seed);
            shockFront(glow, detail, collapseAge / 30, length, radius, alpha, seed, true);
        }
    }

    /** Fast sparks peel off the hot center; the outer red silhouette stays readable. */
    private static void releaseMotes(EaVfxMesh.Sink out, int detail, float age,
                                      float length, float radius, float alpha, int seed) {
        int motes = count(detail, 28, 18, 10);
        for (int i = 0; i < motes; i++) {
            float life = fract(hash(seed + i * 751) + age * (0.025F + (i % 3) * 0.006F));
            float u = 0.06F + life * 0.88F;
            float a = hash(seed + i * 757) * TAU + age * 0.04F + life * 1.3F;
            float r = envelope(u, radius) * (0.58F + life * 0.32F);
            float x = cos(a) * r, y = sin(a) * r, z = u * length;
            float back = Math.max(0, z - Math.min(length * 0.045F, 4.2F));
            float visible = alpha * sin(PI * life);
            int color = i % 4 == 0 ? GOLD : RED;
            stroke(out, cos(a - 0.08F) * r * 0.90F, sin(a - 0.08F) * r * 0.90F, back,
                    x, y, z, radius * 0.008F, RED, visible * 0.13F);
            stroke(out, cos(a - 0.04F) * r * 0.96F, sin(a - 0.04F) * r * 0.96F, (back + z) * 0.5F,
                    x, y, z, radius * 0.002F, color, visible * 0.7F);
            float size = Math.min(radius * 0.009F, length * 0.005F);
            star(out, x, y, z, size, size * 2.6F, GOLD, visible * 0.65F);
        }
    }

    /** Feathered, broken shock fronts give firing and collapse distinct expanding silhouettes. */
    private static void shockFront(EaVfxMesh.Sink out, int detail, float time, float z,
                                     float radius, float alpha, int seed, boolean finishing) {
        if (time <= 0 || time >= 1) return;
        float expand = 1 - (1 - time) * (1 - time) * (1 - time);
        float visible = alpha * smooth(time / 0.10F) * (1 - smooth(time));
        float reach = radius * (finishing ? 0.45F + expand * 1.95F : 0.30F + expand * 1.2F);
        int steps = count(detail, 28, 18, 12);
        for (int arc = 0; arc < 3; arc++) {
            float phase = arc * TAU / 3 + hash(seed + arc * 761) * 0.3F + time * 0.6F;
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * 1.72F, b = phase + v * 1.72F;
                float jag = 1 + sin(a * 9 + time * 7) * 0.025F;
                float jag2 = 1 + sin(b * 9 + time * 7) * 0.025F;
                float r = reach * jag, rr = reach * jag2;
                float zz = z + sin(a * 3) * radius * 0.09F, zzz = z + sin(b * 3) * radius * 0.09F;
                float strength = visible * sin(PI * u);
                float ca = cos(a), sa = sin(a), cb = cos(b), sb = sin(b);
                out.vertex(ca * r * 0.78F, sa * r * 0.78F, zz, RED, 0);
                out.vertex(cb * rr * 0.78F, sb * rr * 0.78F, zzz, RED, 0);
                out.vertex(cb * rr, sb * rr, zzz, RED, strength * 0.34F);
                out.vertex(ca * r, sa * r, zz, RED, strength * 0.34F);
                out.vertex(ca * r, sa * r, zz, RED, strength * 0.34F);
                out.vertex(cb * rr, sb * rr, zzz, RED, strength * 0.34F);
                out.vertex(cb * rr * 1.08F, sb * rr * 1.08F, zzz, RED, 0);
                out.vertex(ca * r * 1.08F, sa * r * 1.08F, zz, RED, 0);
                stroke(out, ca * r, sa * r, zz, cb * rr, sb * rr, zzz,
                        radius * 0.009F, finishing ? GOLD : WHITE, strength * 0.66F);
            }
        }
    }

    private static void nova(EaVfxMesh.Sink body, EaVfxMesh.Sink glow, int detail,
                             float age, float length, float radius, float alpha, int seed) {
        float t = clamp(age / 30), expand = smooth(t / 0.8F);
        float visible = alpha * smooth(age / 2) * (1 - smooth(t));
        int petals = count(detail, 16, 11, 7);
        for (int i = 0; i < petals; i++) {
            float a = TAU * i / petals + hash(seed + i * 631) * 0.22F + age * 0.006F;
            float reach = radius * (0.4F + expand * (1.1F + hash(seed + i * 641) * 0.55F));
            float base = radius * 0.18F, width = reach * 0.11F;
            float x = cos(a), y = sin(a), tipZ = length + sin(i * 2.1F) * reach * 0.22F;
            glow.vertex(x * base, y * base, length, WHITE, visible * 0.58F);
            glow.vertex(x * reach * 0.45F - y * width, y * reach * 0.45F + x * width,
                    length, RED, visible * 0.29F);
            glow.vertex(x * reach, y * reach, tipZ, RED, 0);
            glow.vertex(x * reach * 0.45F + y * width, y * reach * 0.45F - x * width,
                    length, RED, visible * 0.29F);
            stroke(glow, x * base, y * base, length, x * reach, y * reach, tipZ,
                    0.035F, i % 3 == 0 ? WHITE : RED, visible * 0.7F);
            fragment(body, glow, x * reach, y * reach, tipZ,
                    0.13F + hash(seed + i * 647) * 0.19F, a + age * 0.08F, visible * 0.8F);
        }
        star(glow, 0, 0, length, radius * 0.09F, radius * (0.3F + expand * 0.45F),
                WHITE, visible * 0.7F);
    }

    private static void fragment(EaVfxMesh.Sink body, EaVfxMesh.Sink glow,
                                 float x, float y, float z, float size, float spin, float alpha) {
        if (size <= 0.00001F || alpha <= 0.001F) return;
        float dx = cos(spin) * size, dy = sin(spin) * size;
        for (int side = -1; side <= 1; side += 2) {
            body.vertex(x - dy, y + dx, z, 0x33212B, alpha * 0.72F);
            body.vertex(x + dx, y + dy, z + size * side, DARK, alpha * 0.8F);
            body.vertex(x + dy, y - dx, z, DARK, alpha * 0.8F);
            body.vertex(x - dx, y - dy, z - size * side, 0x482832, alpha * 0.64F);
        }
        stroke(glow, x - dy, y + dx, z, x + dx, y + dy, z + size,
                size * 0.10F, RED, alpha * 0.55F);
    }

    /** Three crossed pointed quads remain visible from all camera directions. */
    private static void star(EaVfxMesh.Sink out, float x, float y, float z,
                             float size, float reach, int color, float alpha) {
        for (int plane = 0; plane < 3; plane++) {
            float dx = plane == 0 ? size : 0, dy = plane == 1 ? size : 0, dz = plane == 2 ? size : 0;
            float tx = plane == 2 ? reach : 0, ty = plane == 0 ? reach : 0, tz = plane == 1 ? reach : 0;
            out.vertex(x - dx, y - dy, z - dz, color, alpha);
            out.vertex(x + tx, y + ty, z + tz, color, 0);
            out.vertex(x + dx, y + dy, z + dz, color, alpha);
            out.vertex(x - tx, y - ty, z - tz, color, 0);
        }
    }

    /** Two perpendicular thin quads, avoiding camera-dependent line orientation. */
    private static void stroke(EaVfxMesh.Sink out, float x, float y, float z,
                               float xx, float yy, float zz, float width, int color, float alpha) {
        if (width <= 0.00001F || alpha <= 0.001F) return;
        float dx = xx - x, dy = yy - y, dz = zz - z;
        float nx = -dy, ny = dx, n = (float) Math.sqrt(dx * dx + dy * dy);
        if (n < 0.00001F) { nx = 1; ny = 0; n = 1; }
        nx *= width / n; ny *= width / n;
        quad(out, x, y, z, xx, yy, zz, nx, ny, 0, color, alpha);
        float bx = -dz * ny, by = dz * nx, bz = dx * ny - dy * nx;
        float d = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        if (d > 0.00001F) quad(out, x, y, z, xx, yy, zz,
                bx * width / d, by * width / d, bz * width / d, color, alpha);
    }

    private static void quad(EaVfxMesh.Sink out, float x, float y, float z,
                             float xx, float yy, float zz, float dx, float dy, float dz, int color, float alpha) {
        out.vertex(x - dx, y - dy, z - dz, color, alpha);
        out.vertex(xx - dx, yy - dy, zz - dz, color, alpha);
        out.vertex(xx + dx, yy + dy, zz + dz, color, alpha);
        out.vertex(x + dx, y + dy, z + dz, color, alpha);
    }

    private static float envelope(float u, float radius) {
        return radius * (0.18F + 0.82F * (float) Math.sqrt(Math.max(0, sin(PI * u * 0.87F))));
    }
    private static int count(int detail, int high, int medium, int low) {
        return detail == 0 ? high : detail == 1 ? medium : low;
    }
    private static float hash(int value) {
        value ^= value >>> 16; value *= 0x7feb352d; value ^= value >>> 15;
        value *= 0x846ca68b; value ^= value >>> 16;
        return (value & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }
    private static boolean finite(float a, float b, float c) { return Float.isFinite(a) && Float.isFinite(b) && Float.isFinite(c); }
    private static float fract(float v) { return v - (float) Math.floor(v); }
    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private static float smooth(float v) { v = clamp(v); return v * v * (3 - 2 * v); }
    private static float sin(float v) { return (float) Math.sin(v); }
    private static float cos(float v) { return (float) Math.cos(v); }
}
