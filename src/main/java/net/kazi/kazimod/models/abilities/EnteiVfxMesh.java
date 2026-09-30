package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.models.abilities.KokuVfxMesh;

public final class EnteiVfxMesh {
    private static final float PI = (float)Math.PI;
    private static final int LAT = 24;
    private static final int LON = 48;
    private static final float[][] SURFACE = EnteiVfxMesh.buildSurface();

    private EnteiVfxMesh() {
    }

    public static void fireball(KokuVfxMesh.Sink out, float age, float radius, float opacity) {
        if (radius <= 0.0f || opacity <= 0.0f) {
            return;
        }
        float s = EnteiVfxMesh.sin(age * 0.13f);
        float c = EnteiVfxMesh.cos(age * 0.13f);
        for (int lat = 0; lat < 24; ++lat) {
            for (int lon = 0; lon < 48; ++lon) {
                for (int corner = 0; corner < 4; ++corner) {
                    int y = lat + (corner >= 2 ? 1 : 0);
                    int x = lon + (corner == 1 || corner == 2 ? 1 : 0);
                    float[] v = SURFACE[y * 49 + x];
                    float wave = s * v[5] + c * v[4];
                    float r = radius * (1.0f + wave * 0.025f);
                    float heat = EnteiVfxMesh.clamp(1.0f - v[3] * 30.0f + wave * 0.14f);
                    out.vertex(v[0] * r, v[1] * r, v[2] * r, EnteiVfxMesh.mix(16739844, 16775088, heat), opacity);
                }
            }
        }
    }

    public static void corona(KokuVfxMesh.Sink out, float age, float radius, float opacity) {
        if (radius <= 0.0f || opacity <= 0.0f) {
            return;
        }
        for (int strand = 0; strand < 40; ++strand) {
            float y = 1.0f - ((float)strand + 0.5f) / 20.0f;
            float a = (float)strand * 2.399f;
            float planar = (float)Math.sqrt(1.0f - y * y);
            float x = EnteiVfxMesh.cos(a) * planar;
            float z = EnteiVfxMesh.sin(a) * planar;
            for (int step = 0; step < 8; ++step) {
                float t = (float)step / 8.0f;
                float tt = (float)(step + 1) / 8.0f;
                float r = radius * (0.99f + t * 0.3f);
                float rr = radius * (0.99f + tt * 0.3f);
                float curl = EnteiVfxMesh.sin(t * 5.0f - age * 0.25f + (float)strand) * radius * t * 0.17f;
                float curl2 = EnteiVfxMesh.sin(tt * 5.0f - age * 0.25f + (float)strand) * radius * tt * 0.17f;
                EnteiVfxMesh.line(out, x * r - EnteiVfxMesh.sin(a) * curl, y * r, z * r + EnteiVfxMesh.cos(a) * curl, x * rr - EnteiVfxMesh.sin(a) * curl2, y * rr, z * rr + EnteiVfxMesh.cos(a) * curl2, radius * 0.07f * (1.0f - t), strand % 3 == 0 ? 16732678 : (strand % 3 == 1 ? 16758046 : 0xFFF0A0), opacity * (1.0f - t) * 0.5f);
            }
        }
    }

    public static void shockwave(KokuVfxMesh.Sink out, float age, float radius, float opacity) {
        if (radius <= 0.0f || opacity <= 0.0f) {
            return;
        }
        for (int band = 0; band < 3; ++band) {
            float r = radius * (1.03f + (float)band * 0.09f);
            float width = Math.max(0.08f, radius * 0.025f);
            for (int step = 0; step < 80; ++step) {
                float a = (float)step * (float)Math.PI / 40.0f;
                float b = (float)(step + 1) * (float)Math.PI / 40.0f;
                float y = EnteiVfxMesh.sin(a * 9.0f - age * 0.35f) * radius * 0.015f + 0.2f;
                out.vertex(EnteiVfxMesh.cos(a) * (r - width), y, EnteiVfxMesh.sin(a) * (r - width), 16773557, opacity * 0.6f);
                out.vertex(EnteiVfxMesh.cos(b) * (r - width), y, EnteiVfxMesh.sin(b) * (r - width), 16773557, opacity * 0.6f);
                out.vertex(EnteiVfxMesh.cos(b) * (r + width), y, EnteiVfxMesh.sin(b) * (r + width), 16773557, opacity * 0.6f);
                out.vertex(EnteiVfxMesh.cos(a) * (r + width), y, EnteiVfxMesh.sin(a) * (r + width), 16773557, opacity * 0.6f);
            }
        }
    }

    private static float[][] buildSurface() {
        float[][] centers = new float[42][3];
        for (int i = 0; i < centers.length; ++i) {
            float y = 1.0f - ((float)i + 0.5f) * 2.0f / (float)centers.length;
            float a = (float)i * 2.399f;
            float r = (float)Math.sqrt(1.0f - y * y);
            centers[i] = new float[]{EnteiVfxMesh.cos(a) * r, y, EnteiVfxMesh.sin(a) * r};
        }
        float[][] vertices = new float[1225][6];
        for (int lat = 0; lat <= 24; ++lat) {
            for (int lon = 0; lon <= 48; ++lon) {
                float phi = (float)lat * (float)Math.PI / 24.0f;
                float theta = (float)(lon % 48) * (float)Math.PI * 2.0f / 48.0f;
                float x = EnteiVfxMesh.cos(theta) * EnteiVfxMesh.sin(phi);
                float y = EnteiVfxMesh.cos(phi);
                float z = EnteiVfxMesh.sin(theta) * EnteiVfxMesh.sin(phi);
                float nearest = -2.0f;
                float second = -2.0f;
                for (float[] center : centers) {
                    float dot = x * center[0] + y * center[1] + z * center[2];
                    if (dot > nearest) {
                        second = nearest;
                        nearest = dot;
                        continue;
                    }
                    if (!(dot > second)) continue;
                    second = dot;
                }
                float phase = x * 7.0f + y * 11.0f + z * 9.0f;
                vertices[lat * 49 + lon] = new float[]{x, y, z, nearest - second, EnteiVfxMesh.sin(phase), EnteiVfxMesh.cos(phase)};
            }
        }
        return vertices;
    }

    private static void line(KokuVfxMesh.Sink out, float x, float y, float z, float xx, float yy, float zz, float width, int color, float alpha) {
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
        EnteiVfxMesh.quad(out, x, y, z, xx, yy, zz, nx *= width / length, ny *= width / length, 0.0f, color, alpha);
        float bx = -dz * ny;
        float by = dz * nx;
        float bz = dx * ny - dy * nx;
        float l = (float)Math.sqrt(bx * bx + by * by + bz * bz);
        if (l > 1.0E-4f) {
            EnteiVfxMesh.quad(out, x, y, z, xx, yy, zz, bx * width / l, by * width / l, bz * width / l, color, alpha);
        }
    }

    private static void quad(KokuVfxMesh.Sink out, float x, float y, float z, float xx, float yy, float zz, float nx, float ny, float nz, int color, float alpha) {
        out.vertex(x + nx, y + ny, z + nz, color, alpha);
        out.vertex(xx + nx, yy + ny, zz + nz, color, alpha);
        out.vertex(xx - nx, yy - ny, zz - nz, color, alpha);
        out.vertex(x - nx, y - ny, z - nz, color, alpha);
    }

    private static int mix(int a, int b, float t) {
        int r = (int)((float)(a >> 16 & 0xFF) * (1.0f - t) + (float)(b >> 16 & 0xFF) * t);
        int g = (int)((float)(a >> 8 & 0xFF) * (1.0f - t) + (float)(b >> 8 & 0xFF) * t);
        int blue = (int)((float)(a & 0xFF) * (1.0f - t) + (float)(b & 0xFF) * t);
        return r << 16 | g << 8 | blue;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float sin(float value) {
        return (float)Math.sin(value);
    }

    private static float cos(float value) {
        return (float)Math.cos(value);
    }
}
