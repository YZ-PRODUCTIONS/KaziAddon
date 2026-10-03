package net.kazi.kazimod.models.abilities;

/** Closed, full-length heart of Ea's rupture, with Caliburn-style pressure surges. */
public final class EaBeamCoreMesh {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2;
    // Caliburn's surface animation runs at 2x its effect clock (1.5 real ticks).
    private static final float MOTION_SPEED = 2.0F / 1.5F;
    private static final int WHITE = 0xFFFFCE;
    private static final int GOLD = 0xFFE539;
    private static final int FIRE = 0xFF731C;
    private static final int RED = 0xEE1030;
    private static final int DARK_RED = 0x93091F;
    private static final ThreadLocal<SurfaceScratch> SURFACE = ThreadLocal.withInitial(SurfaceScratch::new);

    private EaBeamCoreMesh() { }

    /** Soften only the new core when the camera is immediately behind or inside it. */
    public static float viewOpacity(float cameraX, float cameraY, float cameraZ, float length, float radius) {
        if (radius <= 0) return 1;
        float radial = (float) Math.sqrt(cameraX * cameraX + cameraY * cameraY);
        float nearAxis = 1 - smooth((radial - radius * 0.35F) / (radius * 0.35F));
        float insideLength = smooth((cameraZ + 4) / 2) * (1 - smooth((cameraZ - length) / 3));
        return 1 - nearAxis * insideLength * 0.85F;
    }

    public static void draw(EaVfxMesh.Sink body, EaVfxMesh.Sink glow, int detail,
                            float age, float length, float radius, float opacity, int seed) {
        if (!Float.isFinite(age) || !Float.isFinite(length) || !Float.isFinite(radius)
                || !Float.isFinite(opacity) || age <= 0 || length <= 0 || radius <= 0) return;
        float alpha = clamp(opacity) * smooth(age / 6.0F);
        if (alpha <= 0.001F) return;
        float motion = age * MOTION_SPEED, phase = (seed & 1023) * 0.173F;
        float pulse = surge(motion * 0.6F + phase);
        int rings = count(detail, 48, 32, 20), sides = count(detail, 28, 20, 12);
        // A closed bright surface supplies an uninterrupted core from muzzle to tip.
        // The red pressure shell remains translucent so the heart stays readable.
        tube(body, rings, sides, length, radius, motion, phase, pulse, alpha, false);
        tube(glow, rings, sides, length, radius, motion, phase, pulse, alpha, true);
        eruptions(body, glow, detail, length, radius, motion, alpha, seed);
    }

    private static void tube(EaVfxMesh.Sink out, int rings, int sides, float length, float radius,
                              float motion, float phase, float pulse, float alpha, boolean shell) {
        // Adjacent quads share their corners. Animate each corner once, then emit
        // the original quad order, including the wrapped seam and both flat caps.
        SurfaceScratch scratch = SURFACE.get();
        scratch.count = 0;
        for (int ring = 0; ring <= rings; ring++) {
            float u = (float) ring / rings;
            for (int side = 0; side < sides; side++) {
                surface(scratch, u, TAU * side / sides, length, radius, motion, phase, pulse, alpha, shell);
            }
        }
        for (int ring = 0; ring < rings; ring++) {
            for (int side = 0; side < sides; side++) {
                int next = (side + 1) % sides, row = ring * sides;
                scratch.emit(out, row + side);
                scratch.emit(out, row + next);
                scratch.emit(out, row + sides + next);
                scratch.emit(out, row + sides + side);
            }
        }
        // Close both ends. The core never pinches to zero or leaves a hollow tube.
        for (int side = 0; side < sides; side++) {
            int next = (side + 1) % sides;
            float capAlpha = alpha * (shell ? 0.20F : 0.94F + pulse * 0.04F);
            int color = shell ? RED : WHITE;
            out.vertex(0, 0, 0, color, capAlpha);
            scratch.emit(out, next);
            scratch.emit(out, side);
            out.vertex(0, 0, 0, color, capAlpha);
            out.vertex(0, 0, length, color, capAlpha);
            scratch.emit(out, rings * sides + side);
            scratch.emit(out, rings * sides + next);
            out.vertex(0, 0, length, color, capAlpha);
        }
    }

    private static final class SurfaceScratch implements EaVfxMesh.Sink {
        private final float[] points = new float[49 * 28 * 4];
        private final int[] colors = new int[49 * 28];
        private int count;
        @Override public void vertex(float x, float y, float z, int color, float alpha) {
            int offset = count * 4;
            points[offset] = x; points[offset + 1] = y; points[offset + 2] = z; points[offset + 3] = alpha;
            colors[count++] = color;
        }
        private void emit(EaVfxMesh.Sink out, int index) {
            int offset = index * 4;
            out.vertex(points[offset], points[offset + 1], points[offset + 2], colors[index], points[offset + 3]);
        }
    }

    private static void surface(EaVfxMesh.Sink out, float u, float angle, float length, float radius,
                                 float motion, float phase, float pulse, float alpha, boolean shell) {
        float pressure = sin(u * 25.0F - motion * 0.8F + angle * 2 + phase);
        float traveling = surge(motion * 0.6F - u * 12.0F + phase);
        float profile = 0.62F + 0.38F * sin(PI * u * 0.86F);
        float r;
        int color;
        float visible;
        if (shell) {
            float tooth = pointedWave(angle * 7 + u * 17 + phase);
            r = radius * profile * (0.60F + pulse * 0.18F)
                    * (0.92F + tooth * tooth * 0.12F + pressure * 0.065F);
            color = blend(DARK_RED, RED, 0.55F + traveling * 0.45F);
            visible = alpha * (0.20F + traveling * 0.12F + pulse * 0.06F);
        } else {
            r = radius * profile * (0.34F + pulse * 0.15F)
                    * (1.0F + pressure * 0.09F + traveling * 0.08F);
            // Overlapping traveling waves stretch hot patches into uneven tongues
            // of fire, matching the golden interior and crimson edge of the reference.
            float heat = clamp(0.52F
                    + 0.28F * sin(u * 37 - motion * 1.7F + 2.4F * sin(angle * 3 + u * 7 - phase))
                    + 0.24F * sin(u * 81 - motion * 2.2F + angle * 5 + phase));
            float vein = smooth((sin(u * 58 - motion * 2 + sin(angle * 4 + u * 12) * 2) - 0.62F) / 0.38F);
            heat = clamp(heat * 0.80F + vein * 0.33F);
            color = heat < 0.5F ? blend(FIRE, GOLD, heat * 2) : blend(GOLD, WHITE, (heat - 0.5F) * 2);
            visible = alpha * (0.92F + pulse * 0.06F);
        }
        out.vertex(cos(angle) * r, sin(angle) * r, u * length, color, visible);
    }

    private static void eruptions(EaVfxMesh.Sink body, EaVfxMesh.Sink glow, int detail,
                                   float length, float radius, float motion, float alpha, int seed) {
        int bursts = count(detail, 22, 14, 8);
        for (int i = 0; i < bursts; i++) {
            float u = 0.08F + hash(seed + i * 701) * 0.79F;
            float angle = hash(seed + i * 709) * TAU;
            float offset = hash(seed + i * 719) * TAU;
            // The same embedded face contracts and punches out, like Caliburn's lobes.
            float kick = surge(motion * 0.6F - u * 12 + offset);
            float profile = 0.62F + 0.38F * sin(PI * u * 0.86F);
            float root = radius * profile * 0.28F;
            float reach = radius * profile * (0.56F + kick * 0.40F);
            float breadth = radius * (0.085F + hash(seed + i * 727) * 0.06F);
            float span = Math.min(length * 0.08F, 5.5F) * (0.65F + kick * 0.35F);
            float z = u * length, back = Math.max(0, z - span * 0.55F), front = Math.min(length, z + span);
            float x = cos(angle), y = sin(angle), dx = -y * breadth, dy = x * breadth;
            float baseX = x * root, baseY = y * root, tipX = x * reach, tipY = y * reach;
            float visible = alpha * (0.38F + kick * 0.24F);
            // Four pointed faces surround a rectangular base embedded in the core.
            face(body, baseX + dx, baseY + dy, back, baseX - dx, baseY - dy, back,
                    tipX, tipY, front, visible);
            face(body, baseX - dx, baseY - dy, back, baseX - dx, baseY - dy, z,
                    tipX, tipY, front, visible);
            face(body, baseX - dx, baseY - dy, z, baseX + dx, baseY + dy, z,
                    tipX, tipY, front, visible);
            face(body, baseX + dx, baseY + dy, z, baseX + dx, baseY + dy, back,
                    tipX, tipY, front, visible);
            face(glow, baseX + dx, baseY + dy, back, baseX - dx, baseY - dy, z,
                    tipX, tipY, front, alpha * (0.16F + kick * 0.25F));
        }
    }

    private static void face(EaVfxMesh.Sink out, float x, float y, float z,
                              float xx, float yy, float zz, float tx, float ty, float tz, float alpha) {
        out.vertex(x, y, z, DARK_RED, alpha * 0.65F);
        out.vertex(xx, yy, zz, RED, alpha);
        out.vertex(tx, ty, tz, GOLD, alpha);
        out.vertex(tx, ty, tz, GOLD, alpha);
    }

    /** Caliburn's cubed sine gives a sharp outward punch and a longer compression. */
    private static float surge(float phase) {
        float wave = 0.5F + 0.5F * sin(phase);
        return wave * wave * wave;
    }
    private static float pointedWave(float phase) {
        float cycle = phase / TAU; cycle -= (float) Math.floor(cycle);
        return 1 - Math.abs(cycle * 2 - 1);
    }
    private static int blend(int a, int b, float t) {
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int blue = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | blue;
    }
    private static int count(int detail, int high, int medium, int low) {
        return detail == 0 ? high : detail == 1 ? medium : low;
    }
    private static float hash(int value) {
        value ^= value >>> 16; value *= 0x7feb352d; value ^= value >>> 15;
        value *= 0x846ca68b; value ^= value >>> 16;
        return (value & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }
    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private static float smooth(float v) { v = clamp(v); return v * v * (3 - 2 * v); }
    private static float sin(float v) { return (float) Math.sin(v); }
    private static float cos(float v) { return (float) Math.cos(v); }
}
