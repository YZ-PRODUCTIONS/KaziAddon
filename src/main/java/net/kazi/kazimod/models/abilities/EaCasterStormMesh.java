package net.kazi.kazimod.models.abilities;

/**
 * The close, violent storm around Ea's caster, underneath the larger galaxies.
 * Coordinates are relative to the caster's feet and remain world-up. The open
 * center and feathered edges let the player remain visible through the storm.
 */
public final class EaCasterStormMesh {
    public static final float MAX_RADIUS = 7.0F;
    public static final float MAX_HEIGHT = 8.0F;
    public static final float MIN_Y = -8.0F;

    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2.0F;
    private static final int CRIMSON = 0xA00622;
    private static final int RED = 0xFF1033;
    private static final int SCARLET = 0xFF3048;
    private static final int SILVER = 0xDDE3E8;
    private static final int WHITE = 0xFFF3F0;

    private EaCasterStormMesh() { }

    /** The same seeded storm is rebuilt at progressively lower detail with distance. */
    public static void charge(EaChargeMesh.Sink body, EaChargeMesh.Sink glow, int detail,
                              float age, float progress, float opacity, int seed) {
        if (!Float.isFinite(age) || !Float.isFinite(progress) || !Float.isFinite(opacity)) return;
        float p = clamp(progress);
        float alpha = clamp(opacity) * smooth(p / 0.045F);
        if (alpha <= 0.001F) return;
        float strength = 0.52F + 0.48F * smooth(p);
        // Each strand uses a different speed and incline, avoiding stacked rings.
        veils(body, glow, detail, age, strength, alpha, seed);
        gusts(glow, detail, age, strength, alpha, seed);
        embers(glow, detail, age, strength, alpha, seed);

        // Continue the red aura and wind beneath the feet, with a separate
        // pattern so the lower storm doesn't look like a perfect reflection.
        EaChargeMesh.Sink lowerBody = (x, y, z, color, visible) -> body.vertex(-z, -y, x, color, visible);
        EaChargeMesh.Sink lowerGlow = (x, y, z, color, visible) -> glow.vertex(-z, -y, x, color, visible);
        int lowerSeed = seed ^ 0x5EA71;
        veils(lowerBody, lowerGlow, detail, age, strength, alpha, lowerSeed);
        gusts(lowerGlow, detail, age, strength, alpha, lowerSeed);
        embers(lowerGlow, detail, age, strength, alpha, lowerSeed);
    }

    private static void veils(EaChargeMesh.Sink body, EaChargeMesh.Sink glow, int detail,
                               float age, float strength, float alpha, int seed) {
        int strands = count(detail, 7, 5, 4);
        int steps = count(detail, 12, 10, 8);
        float veilStrength = 0.27F + strength * 0.73F;
        for (int i = 0; i < strands; i++) {
            float key = hash(seed + i * 337);
            float phase = key * TAU - age * (0.095F + hash(seed + i * 113) * 0.045F);
            float sweep = 2.5F + hash(seed + i * 173) * 1.6F;
            float baseRadius = 2.7F + hash(seed + i * 199) * 1.75F;
            float height = 4.6F + hash(seed + i * 227) * 1.8F;
            float visibility = alpha * veilStrength * (0.78F + 0.22F * sin(age * 0.11F + i * 2.1F));
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * sweep + sin(u * 12.0F + age * 0.06F + i) * 0.11F;
                float b = phase + v * sweep + sin(v * 12.0F + age * 0.06F + i) * 0.11F;
                float r = veilRadius(baseRadius, u, age, i);
                float rr = veilRadius(baseRadius, v, age, i);
                float y = 0.32F + u * height;
                float yy = 0.32F + v * height;
                // Uneven lobes produce torn tongues of aura, with clear gaps between them.
                float w = veilWidth(u, age, i) * veilStrength * 1.4F;
                float ww = veilWidth(v, age, i) * veilStrength * 1.4F;
                float edgeAngle = a + PI * 0.5F, edgeAngle2 = b + PI * 0.5F;
                // The body and glow use the same directions, so evaluate them once.
                float ca = cos(a), sa = sin(a), cb = cos(b), sb = sin(b);
                float edgeX = cos(edgeAngle), edgeZ = sin(edgeAngle);
                float edgeX2 = cos(edgeAngle2), edgeZ2 = sin(edgeAngle2);
                // Descending feather axes cut across the rising paths to create
                // broad red sheets instead of nearly parallel, hairline strips.
                strip(body, ca, sa, cb, sb, r, rr, y, yy, edgeX, edgeZ, edgeX2, edgeZ2, w, ww, -0.60F,
                        CRIMSON, visibility * 0.30F);
                strip(glow, ca, sa, cb, sb, r + 0.015F, rr + 0.015F, y, yy,
                        edgeX, edgeZ, edgeX2, edgeZ2, w * 0.78F, ww * 0.78F, -0.60F,
                        RED, visibility * 0.42F);
            }
        }
    }

    private static float veilRadius(float base, float u, float age, int strand) {
        return base + sin(u * PI) * 0.78F + sin(u * 10.0F - age * 0.08F + strand) * 0.30F;
    }

    private static float veilWidth(float u, float age, int strand) {
        float lobe = 0.70F + 0.30F * sin(u * 17.0F + age * 0.13F + strand * 2.0F);
        return Math.max(0.0F, sin(PI * u)) * (0.82F + lobe * 0.80F);
    }

    private static void gusts(EaChargeMesh.Sink glow, int detail, float age,
                              float strength, float alpha, int seed) {
        int strands = count(detail, 9, 7, 5);
        int steps = count(detail, 20, 15, 13);
        for (int i = 0; i < strands; i++) {
            float key = hash(seed + i * 383);
            float phase = key * TAU + age * (0.18F + hash(seed + i * 251) * 0.11F)
                    + strength * strength * 4.0F;
            float sweep = 1.55F + hash(seed + i * 269) * 2.2F;
            float radius = 2.65F + hash(seed + i * 283) * 2.9F;
            boolean low = i % 3 == 0;
            float centerY = low ? 1.12F : 3.1F + hash(seed + i * 307) * 1.3F;
            float slope = (i % 2 == 0 ? 1 : -1) * (low ? 1.05F : 2.3F + key);
            float visibility = alpha * strength * (0.78F + 0.22F * sin(age * 0.15F + i * 1.7F));
            for (int j = 0; j < steps; j++) {
                float u = (float) j / steps, v = (float) (j + 1) / steps;
                float a = phase + u * sweep, b = phase + v * sweep;
                float r = radius + sin(u * 7.0F + i) * 0.4F + u * 0.2F;
                float rr = radius + sin(v * 7.0F + i) * 0.4F + v * 0.2F;
                float y = centerY + slope * (u - 0.5F) + sin(a + i) * (low ? 0.18F : 0.42F);
                float yy = centerY + slope * (v - 0.5F) + sin(b + i) * (low ? 0.18F : 0.42F);
                float w = gustWidth(u, strength), ww = gustWidth(v, strength);
                float ca = cos(a), sa = sin(a), cb = cos(b), sb = sin(b);
                // A scarlet wash trails the narrow pale core, instead of an opaque white wall.
                strip(glow, ca, sa, cb, sb, r, rr, y, yy, ca, sa, cb, sb, w * 3.7F, ww * 3.7F, 0.90F,
                        SCARLET, visibility * 0.19F);
                // A narrow pressure crest races through each broad gust.
                float crest = Math.max(0, sin(u * 10.0F - age * 0.32F + i * 2.4F));
                strip(glow, ca, sa, cb, sb, r + 0.018F, rr + 0.018F, y, yy,
                        ca, sa, cb, sb, w, ww, 0.90F, i % 3 == 1 ? SILVER : WHITE,
                        visibility * (0.65F + crest * crest * 0.30F));
            }
        }
    }

    private static float gustWidth(float u, float strength) {
        // Sharp leading end, longer thinning tail; every arc remains visibly broken.
        return Math.max(0.0F, sin(PI * u)) * (0.052F + strength * 0.060F) * (0.45F + u * 0.55F);
    }

    private static void embers(EaChargeMesh.Sink out, int detail, float age,
                               float strength, float alpha, int seed) {
        int count = count(detail, 12, 8, 6);
        for (int i = 0; i < count; i++) {
            float u = fract(hash(seed + i * 409) + age * (0.014F + i % 3 * 0.003F));
            float a = hash(seed + i * 421) * TAU + age * 0.15F + u * 2.0F;
            float r = 2.4F + hash(seed + i * 439) * 3.3F;
            float x = cos(a) * r, y = 0.5F + u * 5.4F, z = sin(a) * r;
            float size = (0.035F + hash(seed + i * 457) * 0.035F) * strength;
            float visible = alpha * strength * Math.max(0.0F, sin(PI * u));
            out.vertex(x - size, y, z, RED, 0);
            out.vertex(x, y + size * 3, z, RED, visible);
            out.vertex(x + size, y, z, RED, 0);
            out.vertex(x, y - size * 3, z, RED, visible);
            // Tapered, curved spark wakes follow the wind instead of hanging in place.
            float tail = 0.07F + strength * 0.10F;
            float backX = cos(a - tail) * r, backZ = sin(a - tail) * r;
            out.vertex(backX, y - 0.28F, backZ, SCARLET, 0);
            out.vertex(x - size, y, z, SCARLET, visible * 0.7F);
            out.vertex(x + size, y, z, WHITE, visible * 0.8F);
            out.vertex(backX, y - 0.28F, backZ, SCARLET, 0);
            out.vertex(x, y, z - size, RED, 0);
            out.vertex(x, y + size * 3, z, RED, visible);
            out.vertex(x, y, z + size, RED, 0);
            out.vertex(x, y - size * 3, z, RED, visible);
        }
    }

    private static void strip(EaChargeMesh.Sink out, float ca, float sa, float cb, float sb,
                               float r, float rr, float y, float yy,
                               float edgeX, float edgeZ, float edgeX2, float edgeZ2,
                               float width, float width2, float vertical, int color, float alpha) {
        float x = ca * r, z = sa * r;
        float xx = cb * rr, zz = sb * rr;
        float dx = edgeX * width * 0.65F, dz = edgeZ * width * 0.65F;
        float dxx = edgeX2 * width2 * 0.65F, dzz = edgeZ2 * width2 * 0.65F;
        float dy = width * vertical, dyy = width2 * vertical;
        out.vertex(x - dx, y - dy, z - dz, color, 0);
        out.vertex(xx - dxx, yy - dyy, zz - dzz, color, 0);
        out.vertex(xx, yy, zz, color, alpha);
        out.vertex(x, y, z, color, alpha);
        out.vertex(x, y, z, color, alpha);
        out.vertex(xx, yy, zz, color, alpha);
        out.vertex(xx + dxx, yy + dyy, zz + dzz, color, 0);
        out.vertex(x + dx, y + dy, z + dz, color, 0);
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
