package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.models.abilities.KokuVfxMesh;

public final class ZushiVfxMesh {
    private static final float PI = (float)Math.PI;
    private static final float[][] ROCK = ZushiVfxMesh.buildRock();

    private ZushiVfxMesh() {
    }

    public static void graviZoneInk(KokuVfxMesh.Sink out, float age, float range) {
        if (range <= 0.0f) {
            return;
        }
        ZushiVfxMesh.gravityInkFull(ZushiVfxMesh.zoneBounds(out, range), age, 4.0f);
    }

    public static void graviZoneGlow(KokuVfxMesh.Sink out, float age, float range) {
        if (range <= 0.0f) {
            return;
        }
        ZushiVfxMesh.gravityGlowFull(ZushiVfxMesh.zoneBounds(out, range), age, 4.0f);
    }

    private static KokuVfxMesh.Sink zoneBounds(KokuVfxMesh.Sink out, float range) {
        float halfSize = (range + 0.5f) * 0.5f;
        return (x, y, z, color, alpha) -> {
            float edge = Math.max(Math.abs(x), Math.abs(z));
            float toSquare = edge < 1.0E-4f ? 1.0f : (float)Math.sqrt(x * x + z * z) / edge;
            float xx = Math.max(-1.0f, Math.min(1.0f, x * toSquare / 4.0f));
            float zz = Math.max(-1.0f, Math.min(1.0f, z * toSquare / 4.0f));
            out.vertex(0.5f + xx * halfSize, 0.5f + (ZushiVfxMesh.clamp(y / 11.0f) * 2.0f - 1.0f) * halfSize, 0.5f + zz * halfSize, color, alpha);
        };
    }

    public static void gravity(KokuVfxMesh.Sink ink, KokuVfxMesh.Sink glow, float age, float radius) {
        ZushiVfxMesh.gravityInk(ink, age, radius);
        ZushiVfxMesh.gravityGlow(glow, age, radius);
    }

    public static void gravityInk(KokuVfxMesh.Sink ink, float age, float radius) {
        if (radius <= 0.0f) {
            return;
        }
        ZushiVfxMesh.gravityInkFull(ZushiVfxMesh.halfSize(ink), age, radius);
    }

    private static KokuVfxMesh.Sink halfSize(KokuVfxMesh.Sink out) {
        return (x, y, z, color, alpha) -> out.vertex(x * 0.5f, y * 0.5f, z * 0.5f, color, alpha);
    }

    private static void gravityInkFull(KokuVfxMesh.Sink ink, float age, float radius) {
        float r;
        float a;
        float strength = Math.min(1.0f, age / 5.0f);
        float height = 7.0f + radius;
        for (int i = 0; i < 48; ++i) {
            a = (float)i * 2.399f;
            r = radius * (0.25f + ZushiVfxMesh.fract((float)i * 0.373f) * 0.65f);
            float t = ZushiVfxMesh.fract((float)i * 0.618f + age * 0.098f);
            float y = (1.0f - t) * height;
            float length = 1.5f + ZushiVfxMesh.fract((float)i * 0.713f) * 3.0f;
            float x = ZushiVfxMesh.cos(a) * r;
            float z = ZushiVfxMesh.sin(a) * r;
            float alpha = strength * ZushiVfxMesh.sin(t * (float)Math.PI);
            ZushiVfxMesh.line(ink, x, y, z, x, Math.max(0.1f, y - length), z, 0.09f, 1774637, alpha * 0.8f);
        }
        for (int crack = 0; crack < 20; ++crack) {
            a = (float)crack * 2.399f;
            r = radius * (0.5f + ZushiVfxMesh.fract((float)crack * 0.37f) * 0.5f);
            for (int segment = 0; segment < 3; ++segment) {
                float t = (float)segment / 3.0f;
                float tt = (float)(segment + 1) / 3.0f;
                float bend = ZushiVfxMesh.sin(segment * 4 + crack) * 0.12f;
                ZushiVfxMesh.line(ink, ZushiVfxMesh.cos(a + bend) * r * t, 0.04f, ZushiVfxMesh.sin(a + bend) * r * t, ZushiVfxMesh.cos(a - bend) * r * tt, 0.04f, ZushiVfxMesh.sin(a - bend) * r * tt, 0.035f * (1.0f - t), 2104357, strength * 0.7f);
            }
        }
    }

    public static void gravityGlow(KokuVfxMesh.Sink glow, float age, float radius) {
        if (radius <= 0.0f) {
            return;
        }
        ZushiVfxMesh.gravityGlowFull(ZushiVfxMesh.halfSize(glow), age, radius);
    }

    private static void gravityGlowFull(KokuVfxMesh.Sink glow, float age, float radius) {
        float strength = Math.min(1.0f, age / 5.0f);
        float height = 7.0f + radius;
        for (int i = 0; i < 48; ++i) {
            float a = (float)i * 2.399f;
            float r = radius * (0.25f + ZushiVfxMesh.fract((float)i * 0.373f) * 0.65f);
            float t = ZushiVfxMesh.fract((float)i * 0.618f + age * 0.098f);
            float y = (1.0f - t) * height;
            float length = 1.5f + ZushiVfxMesh.fract((float)i * 0.713f) * 3.0f;
            float x = ZushiVfxMesh.cos(a) * r + 0.03f;
            float z = ZushiVfxMesh.sin(a) * r;
            ZushiVfxMesh.line(glow, x, y, z, x, Math.max(0.1f, y - length), z, 0.025f, i % 3 == 0 ? 11836397 : 15394042, strength * ZushiVfxMesh.sin(t * (float)Math.PI) * 0.6f);
        }
        for (int ring = 0; ring < 3; ++ring) {
            float t = ZushiVfxMesh.fract(age / 13.0f + (float)ring / 3.0f);
            ZushiVfxMesh.ring(glow, radius * (0.85f - t * 0.3f), (1.0f - t) * height, 0.035f, 14141183, strength * 0.35f, 48);
        }
        float pulse = ZushiVfxMesh.fract(age / 14.0f);
        ZushiVfxMesh.ring(glow, radius * (0.55f + pulse * 0.5f), 0.12f, 0.055f, 0xE8E4EE, (1.0f - pulse) * strength * 0.4f, 48);
    }

    public static void meteor(KokuVfxMesh.Sink solid, float radius) {
        if (radius <= 0.0f) {
            return;
        }
        for (float[] vertex : ROCK) {
            solid.vertex(vertex[0] * radius, vertex[1] * radius, vertex[2] * radius, (int)vertex[3], 1.0f);
        }
    }

    public static void meteorFire(KokuVfxMesh.Sink glow, float age, float radius, float speed) {
        if (radius <= 0.0f) {
            return;
        }
        float tail = radius * (4.0f + Math.min(3.0f, speed));
        for (int strand = 0; strand < 24; ++strand) {
            float angle = (float)strand * 2.399f;
            for (int segment = 0; segment < 16; ++segment) {
                float u = (float)segment / 16.0f;
                float v = (float)(segment + 1) / 16.0f;
                float y = -radius * 0.9f + u * (tail + radius);
                float yy = -radius * 0.9f + v * (tail + radius);
                float r = ZushiVfxMesh.flameRadius(u, radius, age, strand);
                float rr = ZushiVfxMesh.flameRadius(v, radius, age, strand);
                float a = angle + ZushiVfxMesh.sin(u * 8.0f - age * 0.3f + (float)strand) * 0.2f;
                float b = angle + ZushiVfxMesh.sin(v * 8.0f - age * 0.3f + (float)strand) * 0.2f;
                float width = radius * (0.065f + 0.1f * ZushiVfxMesh.sin(u * (float)Math.PI)) * (1.0f - u);
                int color = strand % 3 == 0 ? 16728330 : (strand % 3 == 1 ? 16757536 : 16773552);
                ZushiVfxMesh.line(glow, ZushiVfxMesh.cos(a) * r, y, ZushiVfxMesh.sin(a) * r, ZushiVfxMesh.cos(b) * rr, yy, ZushiVfxMesh.sin(b) * rr, width, color, (u < 0.2f ? 0.25f : 0.55f) * (1.0f - u));
            }
        }
        for (int i = 0; i < 32; ++i) {
            float t = ZushiVfxMesh.fract(age * 0.09f + (float)i * 0.618f);
            float a = (float)i * 2.399f;
            float r = radius * (1.3f + ZushiVfxMesh.fract((float)i * 0.31f));
            float y = radius + tail * t;
            ZushiVfxMesh.line(glow, ZushiVfxMesh.cos(a) * r, y, ZushiVfxMesh.sin(a) * r, ZushiVfxMesh.cos(a) * r, y + radius * 0.9f, ZushiVfxMesh.sin(a) * r, radius * 0.012f, i % 2 == 0 ? 16774615 : 16752168, 0.6f * (1.0f - t));
        }
    }

    public static void impact(KokuVfxMesh.Sink glow, float age, float scale) {
        float fade = 1.0f - ZushiVfxMesh.clamp((age - 18.0f) / 32.0f);
        if (fade <= 0.0f || scale <= 0.0f) {
            return;
        }
        float expansion = ZushiVfxMesh.clamp(age / 12.0f);
        float radius = (2.0f + expansion * 16.0f) * scale;
        for (int strand = 0; strand < 28; ++strand) {
            float a = (float)strand * 2.399f;
            for (int segment = 0; segment < 12; ++segment) {
                float u = (float)segment / 12.0f;
                float v = (float)(segment + 1) / 12.0f;
                float r = radius * (0.2f + ZushiVfxMesh.fract((float)strand * 0.618f) * 0.8f);
                float x = ZushiVfxMesh.cos(a) * r * (1.0f - u * 0.6f);
                float z = ZushiVfxMesh.sin(a) * r * (1.0f - u * 0.6f);
                float xx = ZushiVfxMesh.cos(a) * r * (1.0f - v * 0.6f);
                float zz = ZushiVfxMesh.sin(a) * r * (1.0f - v * 0.6f);
                float height = radius * (1.0f + ZushiVfxMesh.fract((float)strand * 0.37f));
                ZushiVfxMesh.line(glow, x, u * height, z, xx, v * height, zz, radius * 0.13f * (1.0f - u), strand % 3 == 0 ? 16732176 : (strand % 3 == 1 ? 16757553 : 0xFFF2BF), fade * 0.6f);
            }
        }
        ZushiVfxMesh.ring(glow, (3.0f + age * 0.8f) * scale, 0.3f, scale * 0.18f, 16771016, fade * 0.7f, 64);
    }

    private static float flameRadius(float t, float radius, float age, int strand) {
        float shell = t < 0.2f ? 0.55f + t * 3.5f : 1.25f * (1.0f - (t - 0.2f) / 0.8f);
        return radius * (shell + ZushiVfxMesh.sin(t * 12.0f - age * 0.35f + (float)strand) * 0.08f);
    }

    private static float[][] buildRock() {
        float[][] vertices = new float[960][4];
        int index = 0;
        for (int latitude = 0; latitude < 12; ++latitude) {
            for (int longitude = 0; longitude < 20; ++longitude) {
                float shade = 0.55f + 0.28f * ZushiVfxMesh.cos((float)longitude * (float)Math.PI / 10.0f) * ZushiVfxMesh.sin(((float)latitude + 0.5f) * (float)Math.PI / 12.0f) + 0.12f * ZushiVfxMesh.sin(latitude * 19 + longitude * 7);
                int gray = (int)(65.0f + shade * 85.0f);
                int color = gray << 16 | gray - 5 << 8 | gray - 11;
                for (int corner = 0; corner < 4; ++corner) {
                    int lat = latitude + (corner >= 2 ? 1 : 0);
                    int lon = longitude + (corner == 1 || corner == 2 ? 1 : 0);
                    float phi = (float)lat * (float)Math.PI / 12.0f;
                    float theta = (float)(lon % 20) * (float)Math.PI / 10.0f;
                    float rough = 1.0f + ZushiVfxMesh.sin(theta * 5.0f + phi * 3.0f) * ZushiVfxMesh.sin(phi) * 0.12f + ZushiVfxMesh.cos(theta * 7.0f - phi * 5.0f) * ZushiVfxMesh.sin(phi) * 0.07f;
                    vertices[index++] = new float[]{ZushiVfxMesh.cos(theta) * ZushiVfxMesh.sin(phi) * rough, ZushiVfxMesh.cos(phi) * rough * 0.92f, ZushiVfxMesh.sin(theta) * ZushiVfxMesh.sin(phi) * rough, color};
                }
            }
        }
        return vertices;
    }

    private static void ring(KokuVfxMesh.Sink out, float radius, float y, float width, int color, float alpha, int steps) {
        for (int i = 0; i < steps; ++i) {
            float a = (float)i * (float)Math.PI * 2.0f / (float)steps;
            float b = (float)(i + 1) * (float)Math.PI * 2.0f / (float)steps;
            out.vertex(ZushiVfxMesh.cos(a) * (radius - width), y, ZushiVfxMesh.sin(a) * (radius - width), color, alpha);
            out.vertex(ZushiVfxMesh.cos(b) * (radius - width), y, ZushiVfxMesh.sin(b) * (radius - width), color, alpha);
            out.vertex(ZushiVfxMesh.cos(b) * (radius + width), y, ZushiVfxMesh.sin(b) * (radius + width), color, alpha);
            out.vertex(ZushiVfxMesh.cos(a) * (radius + width), y, ZushiVfxMesh.sin(a) * (radius + width), color, alpha);
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
        float length = (float)Math.sqrt(nx * nx + ny * ny);
        if (length < 1.0E-4f) {
            nx = 1.0f;
            ny = 0.0f;
            length = 1.0f;
        }
        ZushiVfxMesh.quad(out, x, y, z, xx, yy, zz, nx *= width / length, ny *= width / length, 0.0f, color, alpha);
        float bx = -dz * ny;
        float by = dz * nx;
        float bz = dx * ny - dy * nx;
        float l = (float)Math.sqrt(bx * bx + by * by + bz * bz);
        if (l > 1.0E-4f) {
            ZushiVfxMesh.quad(out, x, y, z, xx, yy, zz, bx * width / l, by * width / l, bz * width / l, color, alpha);
        }
    }

    private static void quad(KokuVfxMesh.Sink out, float x, float y, float z, float xx, float yy, float zz, float nx, float ny, float nz, int color, float alpha) {
        out.vertex(x + nx, y + ny, z + nz, color, alpha);
        out.vertex(xx + nx, yy + ny, zz + nz, color, alpha);
        out.vertex(xx - nx, yy - ny, zz - nz, color, alpha);
        out.vertex(x - nx, y - ny, z - nz, color, alpha);
    }

    private static float sin(float value) {
        return (float)Math.sin(value);
    }

    private static float cos(float value) {
        return (float)Math.cos(value);
    }

    private static float fract(float value) {
        return value - (float)Math.floor(value);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
