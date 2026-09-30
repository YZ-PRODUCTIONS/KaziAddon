package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.models.abilities.KokuVfxMesh;

public final class InfiniteVoidMesh {
    private static final float PI = (float)Math.PI;

    private InfiniteVoidMesh() {
    }

    public static float expansionRadius(float maximum, float age) {
        float progress = Math.max(0.0f, Math.min(1.0f, age / 40.0f));
        return maximum * progress * progress * (3.0f - 2.0f * progress);
    }

    public static void shell(KokuVfxMesh.Sink out, float radius) {
        if (radius <= 0.0f) {
            return;
        }
        for (int y = 0; y < 12; ++y) {
            for (int x = 0; x < 32; ++x) {
                float a = -1.5707964f + (float)Math.PI * (float)y / 12.0f;
                float b = a + 0.2617994f;
                float c = (float)Math.PI * (float)x / 16.0f;
                float d = c + 0.19634955f;
                InfiniteVoidMesh.point(out, radius, a, c);
                InfiniteVoidMesh.point(out, radius, b, c);
                InfiniteVoidMesh.point(out, radius, b, d);
                InfiniteVoidMesh.point(out, radius, a, d);
            }
        }
        for (int i = 0; i < 64; ++i) {
            float a = (float)i * (float)Math.PI / 32.0f;
            float b = (float)(i + 1) * (float)Math.PI / 32.0f;
            out.vertex(0.0f, 0.015f, 0.0f, 197385, 1.0f);
            out.vertex(InfiniteVoidMesh.cos(a) * radius, 0.015f, InfiniteVoidMesh.sin(a) * radius, 197385, 1.0f);
            out.vertex(InfiniteVoidMesh.cos(b) * radius, 0.015f, InfiniteVoidMesh.sin(b) * radius, 197385, 1.0f);
            out.vertex(0.0f, 0.015f, 0.0f, 197385, 1.0f);
        }
    }

    private static void point(KokuVfxMesh.Sink out, float r, float lat, float lon) {
        out.vertex(InfiniteVoidMesh.cos(lat) * InfiniteVoidMesh.cos(lon) * r, InfiniteVoidMesh.sin(lat) * r, InfiniteVoidMesh.cos(lat) * InfiniteVoidMesh.sin(lon) * r, 131592, 1.0f);
    }

    public static void energy(KokuVfxMesh.Sink out, float radius, float age) {
        int i;
        if (radius < 0.1f) {
            return;
        }
        float settle = Math.max(0.0f, Math.min(1.0f, (age - 40.0f) / 25.0f));
        float z = radius * 0.72f;
        float streakBrightness = 0.72f + 0.08f * InfiniteVoidMesh.sin(age * 0.12f);
        for (i = 0; i < 112; ++i) {
            float angle = (float)i * 2.399963f + 0.02f * InfiniteVoidMesh.sin(age * 0.04f);
            float near = 0.04f;
            float far = 1.0f;
            float reach = radius * (0.4f + InfiniteVoidMesh.fract((float)i * 0.317f) * 0.5f);
            float x1 = InfiniteVoidMesh.cos(angle) * reach * near;
            float y1 = InfiniteVoidMesh.sin(angle) * reach * near;
            float x2 = InfiniteVoidMesh.cos(angle) * reach * far;
            float y2 = InfiniteVoidMesh.sin(angle) * reach * far;
            InfiniteVoidMesh.line(out, x1, y1, z - near * radius * 1.4f, x2, y2, z - far * radius * 1.4f, radius * 0.0025f, i % 3 == 0 ? 16247039 : 13986749, streakBrightness);
        }
        for (int band = 0; band < 7; ++band) {
            for (int i2 = 0; i2 < 64; ++i2) {
                float a = (float)i2 * (float)Math.PI / 32.0f;
                float b = (float)(i2 + 1) * (float)Math.PI / 32.0f;
                float phase = age * 0.015f + (float)band * 1.7f;
                float r1 = radius * (0.22f + (float)band * 0.017f + InfiniteVoidMesh.sin(a * 5.0f + phase) * 0.018f);
                float r2 = radius * (0.22f + (float)band * 0.017f + InfiniteVoidMesh.sin(b * 5.0f + phase) * 0.018f);
                float bright = (0.3f + 0.7f * Math.abs(InfiniteVoidMesh.sin(a * 3.0f - phase))) * (0.25f + settle * 0.75f);
                InfiniteVoidMesh.line(out, InfiniteVoidMesh.cos(a) * r1, InfiniteVoidMesh.sin(a) * r1 + radius * 0.1f, z, InfiniteVoidMesh.cos(b) * r2, InfiniteVoidMesh.sin(b) * r2 + radius * 0.1f, z, radius * 0.016f, band == 6 ? 13478568 : 13360376, bright * 0.6f);
            }
        }
        for (i = 0; i < 128; ++i) {
            float a = (float)i * 2.399963f;
            float h = 1.0f - 2.0f * ((float)i + 0.5f) / 128.0f;
            float r = (float)Math.sqrt(1.0f - h * h) * radius * 0.96f;
            float x = InfiniteVoidMesh.cos(a) * r;
            float y = h * radius * 0.96f;
            float starZ = InfiniteVoidMesh.sin(a) * r;
            float size = radius * (0.001f + InfiniteVoidMesh.fract((float)i * 0.73f) * 0.001f);
            InfiniteVoidMesh.line(out, x - size, y, starZ, x + size, y, starZ, size, i % 4 == 0 ? 14001357 : 13231343, 0.35f + settle * 0.45f);
        }
    }

    private static void line(KokuVfxMesh.Sink out, float x, float y, float z, float xx, float yy, float zz, float width, int color, float alpha) {
        float dx = xx - x;
        float dy = yy - y;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-5f || alpha <= 0.0f) {
            return;
        }
        float nx = -dy / length * width;
        float ny = dx / length * width;
        out.vertex(x + nx, y + ny, z, color, alpha);
        out.vertex(xx + nx, yy + ny, zz, color, alpha);
        out.vertex(xx - nx, yy - ny, zz, color, alpha);
        out.vertex(x - nx, y - ny, z, color, alpha);
    }

    private static float sin(float v) {
        return (float)Math.sin(v);
    }

    private static float cos(float v) {
        return (float)Math.cos(v);
    }

    private static float fract(float v) {
        return v - (float)Math.floor(v);
    }
}
