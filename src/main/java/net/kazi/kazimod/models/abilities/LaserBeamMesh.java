package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.effects.LaserBeamProfile;
import net.kazi.kazimod.models.abilities.KokuVfxMesh;

public final class LaserBeamMesh {
    private static final double TAU = Math.PI * 2;

    private LaserBeamMesh() {
    }

    public static void render(KokuVfxMesh.Sink out, float length, float age, float damage, int color, float alpha) {
        int i;
        if (!Float.isFinite(length) || !Float.isFinite(age) || !Float.isFinite(alpha) || length <= 0.0f || alpha <= 0.0f) {
            return;
        }
        alpha = Math.min(1.0f, alpha);
        float r = LaserBeamProfile.radius(damage);
        float pulse = 1.0f + 0.035f * (float)Math.sin((double)age * 0.7);
        LaserBeamMesh.tube(out, length, r * 1.85f * pulse, color, alpha * 0.075f);
        LaserBeamMesh.tube(out, length, r * pulse, color, alpha * 0.3f);
        LaserBeamMesh.tube(out, length, r * 0.38f, 0xF7FFFF, alpha * 0.95f);
        float ringScale = Math.min(1.0f, length / Math.max(0.1f, r * 5.0f));
        LaserBeamMesh.ring(out, 0.03f, r * 3.9f * ringScale, r * 0.8f * ringScale, color, alpha * 0.55f, age * 0.04f);
        LaserBeamMesh.ring(out, 0.03f, r * 3.9f * ringScale, r * 0.18f * ringScale, 0xF4FFFF, alpha * 0.65f, age * 0.04f);
        int rings = damage >= 80.0f ? 5 : 3;
        for (i = 0; i < rings; ++i) {
            float travel = (age * 0.026f + (float)i / (float)rings) % 1.0f;
            float z = length * (0.025f + travel * 0.4f);
            float visibility = (float)Math.sin((double)travel * Math.PI);
            LaserBeamMesh.ring(out, z, r * (1.45f + travel * 0.9f) * ringScale, r * 0.16f * ringScale, color, alpha * visibility * 0.65f, age * 0.08f + (float)i);
        }
        for (i = 0; i < 2; ++i) {
            LaserBeamMesh.spiral(out, length, r * 0.75f, age, (double)i * Math.PI, color, alpha * 0.4f);
        }
        LaserBeamMesh.flare(out, 0.0f, r * 1.5f * ringScale, color, alpha, age);
        LaserBeamMesh.flare(out, length, r * 2.2f * ringScale, color, alpha, -age);
    }

    private static void tube(KokuVfxMesh.Sink out, float length, float radius, int color, float alpha) {
        for (int row = 0; row < 8; ++row) {
            float t0 = (float)row / 8.0f;
            float t1 = (float)(row + 1) / 8.0f;
            float r0 = radius * LaserBeamMesh.taper(t0);
            float r1 = radius * LaserBeamMesh.taper(t1);
            for (int side = 0; side < 12; ++side) {
                double a = (double)side * (Math.PI * 2) / 12.0;
                double b = (double)(side + 1) * (Math.PI * 2) / 12.0;
                LaserBeamMesh.v(out, Math.cos(a) * (double)r0, Math.sin(a) * (double)r0, length * t0, color, alpha);
                LaserBeamMesh.v(out, Math.cos(b) * (double)r0, Math.sin(b) * (double)r0, length * t0, color, alpha);
                LaserBeamMesh.v(out, Math.cos(b) * (double)r1, Math.sin(b) * (double)r1, length * t1, color, alpha);
                LaserBeamMesh.v(out, Math.cos(a) * (double)r1, Math.sin(a) * (double)r1, length * t1, color, alpha);
            }
        }
    }

    private static float taper(float t) {
        return 1.0f - 0.62f * t;
    }

    private static void ring(KokuVfxMesh.Sink out, float z, float radius, float width, int color, float alpha, float phase) {
        for (int i = 0; i < 40; ++i) {
            double a = (double)i * (Math.PI * 2) / 40.0;
            double b = (double)(i + 1) * (Math.PI * 2) / 40.0;
            float shimmer = 0.78f + 0.22f * (float)Math.sin(a * 3.0 + (double)phase);
            LaserBeamMesh.v(out, Math.cos(a) * (double)radius, Math.sin(a) * (double)radius, z, color, alpha * shimmer);
            LaserBeamMesh.v(out, Math.cos(b) * (double)radius, Math.sin(b) * (double)radius, z, color, alpha * shimmer);
            LaserBeamMesh.v(out, Math.cos(b) * (double)(radius + width), Math.sin(b) * (double)(radius + width), z, color, 0.0f);
            LaserBeamMesh.v(out, Math.cos(a) * (double)(radius + width), Math.sin(a) * (double)(radius + width), z, color, 0.0f);
        }
    }

    private static void spiral(KokuVfxMesh.Sink out, float length, float radius, float age, double phase, int color, float alpha) {
        for (int i = 0; i < 40; ++i) {
            double a = phase + (double)i * (Math.PI * 2) * 2.0 / 40.0 - (double)age * 0.2;
            double b = phase + (double)(i + 1) * (Math.PI * 2) * 2.0 / 40.0 - (double)age * 0.2;
            float t = (float)i / 40.0f;
            float u = (float)(i + 1) / 40.0f;
            float r0 = radius * LaserBeamMesh.taper(t);
            float r1 = radius * LaserBeamMesh.taper(u);
            float w = radius * 0.09f;
            LaserBeamMesh.v(out, Math.cos(a) * (double)r0, Math.sin(a) * (double)r0, length * t, color, alpha);
            LaserBeamMesh.v(out, Math.cos(b) * (double)r1, Math.sin(b) * (double)r1, length * u, color, alpha);
            LaserBeamMesh.v(out, Math.cos(b) * (double)(r1 + w), Math.sin(b) * (double)(r1 + w), length * u, color, 0.0f);
            LaserBeamMesh.v(out, Math.cos(a) * (double)(r0 + w), Math.sin(a) * (double)(r0 + w), length * t, color, 0.0f);
        }
    }

    private static void flare(KokuVfxMesh.Sink out, float z, float radius, int color, float alpha, float age) {
        for (int plane = 0; plane < 3; ++plane) {
            for (int layer = 0; layer < 2; ++layer) {
                float r = radius * (layer == 0 ? 1.4f : 0.42f);
                int tint = layer == 0 ? color : 0xFFFFFF;
                for (int i = 0; i < 24; ++i) {
                    double a = (double)i * (Math.PI * 2) / 24.0;
                    double b = (double)(i + 1) * (Math.PI * 2) / 24.0;
                    LaserBeamMesh.disc(out, plane, 0.0, 0.0, z, tint, alpha * (layer == 0 ? 0.26f : 0.85f));
                    LaserBeamMesh.disc(out, plane, Math.cos(a) * (double)r, Math.sin(a) * (double)r, z, tint, 0.0f);
                    LaserBeamMesh.disc(out, plane, Math.cos(b) * (double)r, Math.sin(b) * (double)r, z, tint, 0.0f);
                    LaserBeamMesh.disc(out, plane, 0.0, 0.0, z, tint, alpha * (layer == 0 ? 0.26f : 0.85f));
                }
            }
            for (int i = 0; i < 8; ++i) {
                double a = (double)i * (Math.PI * 2) / 8.0 + (double)age * 0.025;
                float reach = radius * (i % 2 == 0 ? 1.8f : 1.1f);
                LaserBeamMesh.disc(out, plane, -Math.sin(a) * (double)radius * 0.075, Math.cos(a) * (double)radius * 0.075, z, 0xE9FFFF, alpha * 0.7f);
                LaserBeamMesh.disc(out, plane, Math.cos(a) * (double)reach, Math.sin(a) * (double)reach, z, color, 0.0f);
                LaserBeamMesh.disc(out, plane, Math.sin(a) * (double)radius * 0.075, -Math.cos(a) * (double)radius * 0.075, z, 0xE9FFFF, alpha * 0.7f);
                LaserBeamMesh.disc(out, plane, 0.0, 0.0, z, 0xFFFFFF, alpha);
            }
        }
    }

    private static void disc(KokuVfxMesh.Sink out, int plane, double x, double y, float z, int color, float alpha) {
        if (plane == 0) {
            LaserBeamMesh.v(out, x, y, z, color, alpha);
        } else if (plane == 1) {
            LaserBeamMesh.v(out, x, 0.0, (double)z + y, color, alpha);
        } else {
            LaserBeamMesh.v(out, 0.0, x, (double)z + y, color, alpha);
        }
    }

    private static void v(KokuVfxMesh.Sink out, double x, double y, double z, int color, float alpha) {
        out.vertex((float)x, (float)y, (float)z, color, alpha);
    }
}

