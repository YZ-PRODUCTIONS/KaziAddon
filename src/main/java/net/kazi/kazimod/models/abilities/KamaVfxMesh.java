package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.models.abilities.KokuVfxMesh;

public final class KamaVfxMesh {
    private static final float PI = (float)Math.PI;
    public static final int DOMAIN_SLASH_COUNT = 1152;
    private static final int[] DOMAIN_OUTLINES = new int[]{4252927, 16732863, 16769899, 9568140, 11700991};

    private KamaVfxMesh() {
    }

    public static void dismantle(KokuVfxMesh.Sink out, float age, float duration, float size, int seed) {
        float fade = 1.0f - KamaVfxMesh.clamp(age / duration);
        float angle = (float)seed * 1.77f;
        float x = KamaVfxMesh.cos(angle) * size * 0.5f;
        float y = KamaVfxMesh.sin(angle) * size * 0.5f;
        float width = size * 0.018f;
        KamaVfxMesh.flatSlash(out, -x, -y, 0.0f, 0.0f, 0.0f, 0.0f, width, fade);
        KamaVfxMesh.flatSlash(out, 0.0f, 0.0f, x, y, 0.0f, width, 0.0f, fade);
    }

    public static void slash(KokuVfxMesh.Sink out, float age, float duration, float size, int seed) {
        float p = KamaVfxMesh.clamp(age / duration);
        float fade = 1.0f - p;
        for (int cut = 0; cut < 3; ++cut) {
            float angle = (float)seed * 1.77f + (float)cut * 1.05f;
            for (int i = 0; i < 20; ++i) {
                float u = (float)i / 20.0f;
                float v = (float)(i + 1) / 20.0f;
                float x = (u - 0.5f) * size;
                float xx = (v - 0.5f) * size;
                float y = KamaVfxMesh.sin(u * (float)Math.PI) * size * 0.12f;
                float yy = KamaVfxMesh.sin(v * (float)Math.PI) * size * 0.12f;
                float width = size * 0.018f;
                KamaVfxMesh.flatSlash(out, x * KamaVfxMesh.cos(angle) - y * KamaVfxMesh.sin(angle), x * KamaVfxMesh.sin(angle) + y * KamaVfxMesh.cos(angle), xx * KamaVfxMesh.cos(angle) - yy * KamaVfxMesh.sin(angle), xx * KamaVfxMesh.sin(angle) + yy * KamaVfxMesh.cos(angle), 0.0f, width * Math.max(0.0f, KamaVfxMesh.sin(u * (float)Math.PI)), width * Math.max(0.0f, KamaVfxMesh.sin(v * (float)Math.PI)), fade);
            }
        }
    }

    public static void web(KokuVfxMesh.Sink out, float age, float duration, float radius) {
        float fade = 1.0f - KamaVfxMesh.clamp(age / duration);
        KokuVfxMesh.Sink ground = (x, y, z, color, alpha) -> out.vertex(x, z, y, color, alpha);
        for (int i = 0; i < 18; ++i) {
            float a = (float)i * (float)Math.PI / 9.0f;
            KamaVfxMesh.flatSlash(ground, 0.0f, 0.0f, KamaVfxMesh.cos(a) * radius, KamaVfxMesh.sin(a) * radius, 0.08f, 0.06f, 0.0f, fade);
            for (int ring = 1; ring <= 4; ++ring) {
                float r = radius * (float)ring / 4.0f;
                KamaVfxMesh.flatSlash(ground, KamaVfxMesh.cos(a) * r, KamaVfxMesh.sin(a) * r, KamaVfxMesh.cos(a + 0.34906587f) * r, KamaVfxMesh.sin(a + 0.34906587f) * r, 0.08f, 0.035f, 0.035f, fade);
            }
        }
    }

    public static void charge(KokuVfxMesh.Sink out, float age, float progress) {
        float p = KamaVfxMesh.clamp(progress);
        float spread = 0.15f + 0.65f * (1.0f - p);
        for (int strand = 0; strand < 14; ++strand) {
            for (int i = 0; i < 16; ++i) {
                float u = (float)i / 16.0f;
                float v = (float)(i + 1) / 16.0f;
                float a = (float)strand * 2.399f + age * 0.15f + u * 6.0f;
                float b = (float)strand * 2.399f + age * 0.15f + v * 6.0f;
                float r = spread * KamaVfxMesh.sin(u * (float)Math.PI);
                float rr = spread * KamaVfxMesh.sin(v * (float)Math.PI);
                float width = (0.04f + 0.06f * KamaVfxMesh.sin(u * (float)Math.PI)) * (0.3f + p);
                KamaVfxMesh.line(out, KamaVfxMesh.cos(a) * r, (u - 0.5f) * (1.0f - p) * 1.3f + KamaVfxMesh.sin(a) * r, (u - 0.5f) * (0.5f + p * 2.0f), KamaVfxMesh.cos(b) * rr, (v - 0.5f) * (1.0f - p) * 1.3f + KamaVfxMesh.sin(b) * rr, (v - 0.5f) * (0.5f + p * 2.0f), width, strand % 3 == 0 ? 16724232 : (strand % 3 == 1 ? 16756496 : 16774563), 0.8f);
            }
        }
        if (p > 0.55f) {
            KamaVfxMesh.arrow(out, age, (p - 0.55f) / 0.45f);
        }
    }

    public static void arrow(KokuVfxMesh.Sink out, float age, float scale) {
        if (scale <= 0.0f) {
            return;
        }
        for (int strand = 0; strand < 12; ++strand) {
            for (int i = 0; i < 16; ++i) {
                float u = (float)i / 16.0f;
                float v = (float)(i + 1) / 16.0f;
                float a = (float)strand * (float)Math.PI / 6.0f + age * 0.18f + u * 5.0f;
                float b = (float)strand * (float)Math.PI / 6.0f + age * 0.18f + v * 5.0f;
                float r = (0.05f + KamaVfxMesh.sin(u * (float)Math.PI) * 0.28f) * scale;
                float rr = (0.05f + KamaVfxMesh.sin(v * (float)Math.PI) * 0.28f) * scale;
                KamaVfxMesh.line(out, KamaVfxMesh.cos(a) * r, KamaVfxMesh.sin(a) * r, (u - 0.5f) * 4.0f * scale, KamaVfxMesh.cos(b) * rr, KamaVfxMesh.sin(b) * rr, (v - 0.5f) * 4.0f * scale, 0.06f * scale * KamaVfxMesh.sin(u * (float)Math.PI), strand % 3 == 0 ? 16727304 : 16771468, 0.85f);
            }
        }
        KamaVfxMesh.line(out, 0.0f, 0.0f, -2.0f * scale, 0.0f, 0.0f, 2.0f * scale, 0.09f * scale, 0xFFFFDC, 1.0f);
        KamaVfxMesh.line(out, -0.45f * scale, 0.0f, 1.3f * scale, 0.0f, 0.0f, 2.0f * scale, 0.07f * scale, 16774320, 1.0f);
        KamaVfxMesh.line(out, 0.45f * scale, 0.0f, 1.3f * scale, 0.0f, 0.0f, 2.0f * scale, 0.07f * scale, 16774320, 1.0f);
    }

    public static void pillar(KokuVfxMesh.Sink out, float age) {
        float grow = KamaVfxMesh.clamp(age / 18.0f);
        float fade = 1.0f - KamaVfxMesh.clamp((age - 75.0f) / 25.0f);
        if (fade <= 0.0f) {
            return;
        }
        float height = 6.0f + 44.0f * grow;
        float radius = 3.0f + 16.0f * grow;
        for (int strand = 0; strand < 28; ++strand) {
            for (int i = 0; i < 20; ++i) {
                float u = (float)i / 20.0f;
                float v = (float)(i + 1) / 20.0f;
                float a = (float)strand * 2.399f + u * 1.5f;
                float b = (float)strand * 2.399f + v * 1.5f;
                float wave = KamaVfxMesh.sin(u * 15.0f - age * 0.3f + (float)strand) * 0.15f;
                float wave2 = KamaVfxMesh.sin(v * 15.0f - age * 0.3f + (float)strand) * 0.15f;
                float r = radius * (0.2f + KamaVfxMesh.fract((float)strand * 0.618f) * 0.8f) * (1.0f - u * 0.55f + wave);
                float rr = radius * (0.2f + KamaVfxMesh.fract((float)strand * 0.618f) * 0.8f) * (1.0f - v * 0.55f + wave2);
                KamaVfxMesh.line(out, KamaVfxMesh.cos(a) * r, u * height, KamaVfxMesh.sin(a) * r, KamaVfxMesh.cos(b) * rr, v * height, KamaVfxMesh.sin(b) * rr, radius * (0.11f + KamaVfxMesh.sin(u * (float)Math.PI) * 0.1f) * (1.0f - u * 0.85f), strand % 3 == 0 ? 16727814 : (strand % 3 == 1 ? 16757268 : 16774317), fade * 0.65f);
            }
        }
        for (int i = 0; i < 72; ++i) {
            float t = KamaVfxMesh.fract((float)i * 0.618f + age * 0.035f);
            float a = (float)i * 2.399f;
            float r = radius * (0.7f + t * 0.6f);
            KamaVfxMesh.line(out, KamaVfxMesh.cos(a) * r, t * height, KamaVfxMesh.sin(a) * r, KamaVfxMesh.cos(a) * r, t * height + 1.3f, KamaVfxMesh.sin(a) * r, 0.08f, 16770470, fade * (1.0f - t));
        }
    }

    public static void domain(KokuVfxMesh.Sink out, float age, float radius) {
        KamaVfxMesh.domain(out, age, radius, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    public static void domain(KokuVfxMesh.Sink out, float age, float radius, float rightX, float rightY, float rightZ, float upX, float upY, float upZ) {
        if (radius <= 0.0f) {
            return;
        }
        for (int i = 0; i < 1152; ++i) {
            float cycle = (float)Math.floor(age / 3.0f + (float)i * 0.17f);
            int variant = KamaVfxMesh.slashHash(i * 7349 + (int)cycle * 19391);
            float a = (float)i * 2.399f + cycle;
            float length = Math.min(radius * 0.4f, 2.0f + (float)(variant & 0x3FF) / 1023.0f * 22.0f);
            float fade = 1.0f - KamaVfxMesh.fract(age / 3.0f + (float)i * 0.17f);
            float width = Math.min(length * 0.025f, 0.07f + (float)(variant >>> 10 & 0xFF) / 255.0f * 0.24f);
            float directionY = KamaVfxMesh.sin((float)i * 19.17f + cycle * 3.13f);
            if (i % 3 != 0) {
                directionY *= 0.08f;
            }
            float directionXZ = (float)Math.sqrt(1.0f - directionY * directionY);
            float azimuth = (float)i * 2.399f + cycle * 1.93f;
            float distance = (radius - length * 0.5f - width) * (float)Math.cbrt(KamaVfxMesh.fract((float)i * 0.618034f + cycle * 0.373f));
            float x = KamaVfxMesh.cos(azimuth) * directionXZ * distance;
            float y = directionY * distance;
            float z = KamaVfxMesh.sin(azimuth) * directionXZ * distance;
            KokuVfxMesh.Sink plane = (u, v, w, color, alpha) -> out.vertex(x + rightX * u + upX * v, y + rightY * u + upY * v, z + rightZ * u + upZ * v, color, alpha);
            float dx = KamaVfxMesh.cos(a) * length * 0.5f;
            float dy = KamaVfxMesh.sin(a) * length * 0.5f;
            int style = (variant >>> 18) % 3;
            int fill = style == 1 ? 0 : 0xFFFFFF;
            int edge = style == 1 ? 0xFFFFFF : 0;
            int palette = (variant >>> 22) % DOMAIN_OUTLINES.length;
            int start = style == 2 ? DOMAIN_OUTLINES[palette] : edge;
            int middle = style == 2 ? DOMAIN_OUTLINES[(palette + 1) % DOMAIN_OUTLINES.length] : edge;
            int end = style == 2 ? DOMAIN_OUTLINES[(palette + 2) % DOMAIN_OUTLINES.length] : edge;
            KamaVfxMesh.flatSlash(plane, -dx, -dy, 0.0f, 0.0f, 0.0f, 0.0f, width, fade, fill, start, middle);
            KamaVfxMesh.flatSlash(plane, 0.0f, 0.0f, dx, dy, 0.0f, width, 0.0f, fade, fill, middle, end);
        }
    }

    private static int slashHash(int value) {
        value ^= value >>> 16;
        value *= 2146121005;
        value ^= value >>> 15;
        return (value *= -2073254261) ^ value >>> 16;
    }

    private static void flatSlash(KokuVfxMesh.Sink out, float x, float y, float xx, float yy, float z, float width, float endWidth, float alpha) {
        KamaVfxMesh.flatSlash(out, x, y, xx, yy, z, width, endWidth, alpha, 0xFFFFFF, 0, 0);
    }

    private static void flatSlash(KokuVfxMesh.Sink out, float x, float y, float xx, float yy, float z, float width, float endWidth, float alpha, int fill, int startOutline, int endOutline) {
        if (alpha <= 0.0f || width <= 0.0f && endWidth <= 0.0f) {
            return;
        }
        float dx = xx - x;
        float dy = yy - y;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4f) {
            return;
        }
        float nx = -dy / length;
        float ny = dx / length;
        for (int strip = 0; strip < 3; ++strip) {
            float low = strip == 0 ? -1.0f : (strip == 1 ? -0.65f : 0.65f);
            float high = strip == 0 ? -0.65f : (strip == 1 ? 0.65f : 1.0f);
            int color = strip == 1 ? fill : startOutline;
            int endColor = strip == 1 ? fill : endOutline;
            out.vertex(x + nx * width * low, y + ny * width * low, z, color, alpha);
            out.vertex(xx + nx * endWidth * low, yy + ny * endWidth * low, z, endColor, alpha);
            out.vertex(xx + nx * endWidth * high, yy + ny * endWidth * high, z, endColor, alpha);
            out.vertex(x + nx * width * high, y + ny * width * high, z, color, alpha);
        }
    }

    private static void line(KokuVfxMesh.Sink out, float x, float y, float z, float xx, float yy, float zz, float width, int color, float alpha) {
        if (width <= 0.0f || alpha <= 0.0f) {
            return;
        }
        float dx = xx - x;
        float dy = yy - y;
        float dz = zz - z;
        float nx = -dy;
        float ny = dx;
        float nz = 0.0f;
        float length = (float)Math.sqrt(nx * nx + ny * ny);
        if (length < 1.0E-4f) {
            nx = 1.0f;
            ny = 0.0f;
            length = 1.0f;
        }
        KamaVfxMesh.quad(out, x, y, z, xx, yy, zz, nx *= width / length, ny *= width / length, nz, color, alpha);
        float bx = dy * nz - dz * ny;
        float by = dz * nx - dx * nz;
        float bz = dx * ny - dy * nx;
        float l = (float)Math.sqrt(bx * bx + by * by + bz * bz);
        if (l > 1.0E-4f) {
            KamaVfxMesh.quad(out, x, y, z, xx, yy, zz, bx * width / l, by * width / l, bz * width / l, color, alpha * 0.7f);
        }
    }

    private static void quad(KokuVfxMesh.Sink out, float x, float y, float z, float xx, float yy, float zz, float nx, float ny, float nz, int color, float alpha) {
        out.vertex(x + nx, y + ny, z + nz, color, alpha);
        out.vertex(xx + nx, yy + ny, zz + nz, color, alpha);
        out.vertex(xx - nx, yy - ny, zz - nz, color, alpha);
        out.vertex(x - nx, y - ny, z - nz, color, alpha);
    }

    private static float sin(float x) {
        return (float)Math.sin(x);
    }

    private static float cos(float x) {
        return (float)Math.cos(x);
    }

    private static float fract(float x) {
        return x - (float)Math.floor(x);
    }

    private static float clamp(float x) {
        return Math.max(0.0f, Math.min(1.0f, x));
    }
}
