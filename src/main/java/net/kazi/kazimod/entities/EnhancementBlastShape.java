package net.kazi.kazimod.entities;

import java.util.Random;
import net.minecraft.util.math.vector.Vector3d;

/**
 * The exact closed main blast and jagged lobes shared by rendering and collision.
 * Coordinates are local to the release yaw; the owning renderer/entity applies
 * RENDER_OFFSET_Y. Glow, dust, pressure rings and rays are decorative overlays.
 */
public final class EnhancementBlastShape {
    public static final double ORIGIN_Y = 3.0D;
    public static final double ORIGIN_Z = 0.5D;
    public static final double BASE_VISUAL_LENGTH = 53.0D;
    public static final double VISUAL_LENGTH = 80.0D;
    public static final double EXTRA_UPWARD_REACH = 10.0D;
    public static final double RENDER_OFFSET_Y = -1.5D;
    public static final float ANIMATION_SPEED = 2.0F;
    public static final float SURFACE_OPACITY = 0.35F;
    private static final int RINGS = 24;
    private static final int SIDES = 40;
    private static final int BURSTS = 22;

    private EnhancementBlastShape() { }

    /** A fresh immutable snapshot, with no renderer state or shared mutable cache. */
    public static Mesh generate(float effectAge, int seed) {
        Builder builder = new Builder();
        generate(builder, effectAge, seed);
        return new Mesh(builder.data, builder.componentStarts);
    }

    /** Match the visible main surface, including its eased deployment and dissolve. */
    public static float surfaceOpacity(float effectAge) {
        float endAge = EnhancementLightEntity.BLAST_TICKS / EnhancementLightEntity.EFFECT_DURATION_SCALE;
        if (!Float.isFinite(effectAge) || effectAge <= 0 || effectAge >= endAge) return 0;
        float deployment = Math.max(0, Math.min(1, effectAge / 3.0F));
        deployment = deployment * deployment * (3 - 2 * deployment);
        float remaining = 1 - fadeProgress(effectAge);
        return deployment * remaining * remaining * SURFACE_OPACITY;
    }

    private static void generate(Builder mesh, float age, int seed) {
        int RINGS = mesh.rings;
        int SIDES = mesh.sides;
        float releaseFlash = blastFlash(age);
        double phase = (seed & 1023) * 0.173D;
        double forward = launch(age, 8.0D);
        double outward = launch(age, 6.0D);
        // Keep deployment on the effect clock so faster surface motion cannot rush it.
        double kick = Math.sin(Math.min(1, age / 12.0D) * Math.PI);
        outward *= 1 + 0.18D * kick * kick;
        age *= ANIMATION_SPEED;
        Vector3d[] vertices = mesh.vertices;
        float[] heat = mesh.heat;
        float[][] uv = mesh.uv;
        float scroll = age * 0.035F;
        vertices[0] = new Vector3d(0, ORIGIN_Y, ORIGIN_Z);
        heat[0] = 1;
        uv[0][0] = -scroll;
        int tip = vertices.length - 1;
        vertices[tip] = new Vector3d(0, ORIGIN_Y + outward * 2, ORIGIN_Z + forward * BASE_VISUAL_LENGTH);
        heat[tip] = 1;
        uv[tip][0] = 6.0F - scroll;
        for (int ring = 1; ring < RINGS; ring++) {
            double t = ring / (double) RINGS;
            for (int side = 0; side < SIDES; side++) {
                double angle = side * Math.PI * 2 / SIDES;
                int index = 1 + (ring - 1) * SIDES + side;
                vertices[index] = surfacePoint(t, angle, age, phase, forward, outward);
                double pressure = 0.5D + 0.5D * Math.sin(t * 26 - age * 0.8D + angle * 2 + phase);
                heat[index] = (float) (0.76D + pressure * 0.19D + Math.sin(angle) * 0.05D);
                uv[index][0] = (float) t * 6.0F - scroll;
                uv[index][1] = side / (float) SIDES;
            }
        }
        // Every edge belongs to two triangles, including the rear and front caps.
        for (int side = 0; side < SIDES; side++) {
            int next = (side + 1) % SIDES;
            triangle(mesh, vertices, heat, uv, 0, 1 + next, 1 + side, true);
            for (int ring = 0; ring < RINGS - 2; ring++) {
                int a = 1 + ring * SIDES + side;
                int b = 1 + ring * SIDES + next;
                int c = b + SIDES;
                int d = a + SIDES;
                triangle(mesh, vertices, heat, uv, a, b, c, true);
                triangle(mesh, vertices, heat, uv, a, c, d, true);
            }
            int last = 1 + (RINGS - 2) * SIDES;
            triangle(mesh, vertices, heat, uv, last + side, last + next, tip, true);
        }
        mesh.componentStarts[1] = mesh.cursor / 6;
        renderEruptionFaces(mesh, age, seed, phase, forward, outward, releaseFlash);
    }

    private static Vector3d surfacePoint(double t, double angle, float age, double phase,
                                          double forward, double outward) {
        // A brighter, slimmer center leaves deep gaps between the broad outer bursts.
        // Triangle-wave teeth produce distinct corners instead of rounded ripples.
        double profile = (0.45D + 5.4D * (1 - Math.exp(-t * 8))) * (1 - Math.pow(t, 8));
        double tooth = pointedWave(angle * 7 + t * 17 + phase);
        double notch = pointedWave(angle * 3 - t * 11 + phase * 0.7D);
        double pressure = Math.sin(t * 25 - age * 0.8D + angle * 2 + phase);
        double radius = profile * (0.72D + 0.62D * tooth * tooth - 0.24D * notch
                + 0.10D * pressure + 0.16D * Math.sin(angle + phase));
        double lean = Math.sin(t * Math.PI);
        double x = Math.cos(angle) * radius + Math.sin(phase) * lean * 1.3D;
        double y = Math.sin(angle) * radius * (0.9D + 0.12D * Math.sin(phase)) + t * 2;
        double z = t * BASE_VISUAL_LENGTH + (pointedWave(angle * 5 + phase) - 0.5D) * lean * 4;
        return new Vector3d(x * outward, ORIGIN_Y + y * outward, ORIGIN_Z + z * forward);
    }

    private static void renderEruptionFaces(Builder mesh, float age,
                                             int seed, double phase, double forward,
                                             double outward, float releaseFlash) {
        Random random = mesh.random;
        random.setSeed(seed * 7919L);
        // Large angular wedges overlap the core. They are broad explosive lobes,
        // not disconnected projectiles, fine particle spray, or long curling tubes.
        for (int burst = 0; burst < mesh.bursts; burst++) {
            double t = 0.10D + random.nextDouble() * 0.76D;
            double angle = random.nextDouble() * Math.PI * 2;
            double reach = 3.5D + random.nextDouble() * 5.5D;
            double length = 3.0D + random.nextDouble() * 8.0D;
            double breadth = 1.4D + random.nextDouble() * 2.2D;
            double offset = random.nextDouble() * Math.PI * 2;
            // The same face compresses and punches outward; its base stays embedded.
            double surge = 0.65D + 0.35D * Math.pow(
                    0.5D + 0.5D * Math.sin(age * 0.6D - t * 12 + offset), 3);
            // Opening and final pressure kicks push the jagged faces outward.
            surge *= 1 + releaseFlash * 0.35D;
            Vector3d edge = surfacePoint(t, angle, age, phase, 1, 1);
            Vector3d center = new Vector3d(Math.sin(phase) * Math.sin(t * Math.PI) * 1.3D,
                    ORIGIN_Y + t * 2, edge.z);
            Vector3d base = center.add(edge.subtract(center).scale(0.58D));
            Vector3d radial = new Vector3d(Math.cos(angle), Math.sin(angle), 0);
            Vector3d side = new Vector3d(-Math.sin(angle), Math.cos(angle), 0).scale(breadth);
            Vector3d back = new Vector3d(0, 0, -2.0D - breadth);
            Vector3d tip = edge.add(radial.scale(reach * surge)).add(0, 0, length * surge);
            Vector3d[] wedge = {base.add(side).add(back), base.subtract(side).add(back),
                    base.subtract(side).add(0, 0, breadth), base.add(side).add(0, 0, breadth), tip};
            for (int i = 0; i < wedge.length; i++) {
                Vector3d point = wedge[i];
                wedge[i] = new Vector3d(point.x * outward,
                        ORIGIN_Y + (point.y - ORIGIN_Y) * outward,
                        ORIGIN_Z + (point.z - ORIGIN_Z) * forward);
            }
            float[] heat = {0.30F, 0.5F, 0.85F, 0.75F, 1.0F};
            float scroll = age * 0.035F - (float) offset;
            float[][] uv = {{-scroll, 0}, {-scroll, 1}, {0.5F - scroll, 1},
                    {0.5F - scroll, 0}, {1.6F - scroll, 0.5F}};
            for (int face = 0; face < 4; face++) {
                triangle(mesh, wedge, heat, uv, face, (face + 1) % 4, 4, false);
            }
            triangle(mesh, wedge, heat, uv, 0, 2, 1, false);
            triangle(mesh, wedge, heat, uv, 0, 3, 2, false);
            mesh.componentStarts[burst + 2] = mesh.cursor / 6;
        }
    }

    private static double pointedWave(double phase) {
        double cycle = phase / (Math.PI * 2);
        cycle -= Math.floor(cycle);
        return 1 - Math.abs(cycle * 2 - 1);
    }

    public static float releaseFlash(float age) {
        float remaining = Math.max(0, Math.min(1, 1 - age / 6.0F));
        float onset = Math.max(0, Math.min(1, age / 1.5F));
        return onset * onset * (3 - 2 * onset) * remaining * remaining;
    }

    public static float fadeStartAge() {
        return EnhancementLightEntity.BLAST_HOLD_TICKS / EnhancementLightEntity.EFFECT_DURATION_SCALE;
    }

    /** Replay the opening light eruption once, exactly when the hold ends. */
    public static float blastFlash(float effectAge) {
        return Math.max(releaseFlash(effectAge), releaseFlash(effectAge - fadeStartAge()));
    }

    public static float fadeProgress(float effectAge) {
        // Hold for six real seconds, then dissipate before the entity and hitbox expire.
        float fadeStart = fadeStartAge();
        float fadeDuration = EnhancementLightEntity.BLAST_FADE_TICKS / EnhancementLightEntity.EFFECT_DURATION_SCALE;
        float progress = Math.max(0, Math.min(1, (effectAge - fadeStart) / fadeDuration));
        return progress * progress * (3 - 2 * progress);
    }

    public static double launch(float age, double ticks) {
        double progress = Math.max(0, Math.min(1, age / ticks));
        // Zero velocity and acceleration at both ends avoid a snap into motion or rest.
        double eased = progress * progress * progress * (progress * (progress * 6 - 15) + 10);
        return 0.025D + 0.975D * eased;
    }

    private static void triangle(Builder mesh, Vector3d[] vertices,
                                  float[] heat, float[][] uv, int a, int b, int c, boolean wrapped) {
        float va = uv[a][1];
        float vb = uv[b][1];
        float vc = uv[c][1];
        if (wrapped) {
            // Unwrap the cylinder seam before interpolation; caps inherit their rim UV.
            if (a == 0) va = vb;
            if (c == vertices.length - 1) vc = va;
            if (Math.max(va, Math.max(vb, vc)) - Math.min(va, Math.min(vb, vc)) > 0.5F) {
                if (va < 0.5F) va += 1;
                if (vb < 0.5F) vb += 1;
                if (vc < 0.5F) vc += 1;
            }
            if (a == 0) va = (vb + vc) * 0.5F;
            if (c == vertices.length - 1) vc = (va + vb) * 0.5F;
            va *= 2;
            vb *= 2;
            vc *= 2;
        }
        mesh.add(vertices[a], heat[a], uv[a][0], va);
        mesh.add(vertices[b], heat[b], uv[b][0], vb);
        mesh.add(vertices[c], heat[c], uv[c][0], vc);
        mesh.add(vertices[c], heat[c], uv[c][0], vc); // Quad buffer, triangular face.
    }

    /**
     * One triangle occupies four vertices; its final vertex repeats the third for
     * the render quad buffer. Component ranges are inclusive start/exclusive end.
     * Each range is a separate closed volume: the core, then each of 22 lobes.
     */
    public static final class Mesh {
        private final float[] data;
        private final int[] componentStarts;

        private Mesh(float[] data, int[] componentStarts) {
            // Ownership transfers from a method-local builder that never escapes.
            this.data = data;
            this.componentStarts = componentStarts;
        }

        public int vertexCount() { return data.length / 6; }
        public int componentCount() { return componentStarts.length - 1; }
        public int componentStart(int component) { return componentStarts[component]; }
        public int componentEnd(int component) { return componentStarts[component + 1]; }
        public float heat(int vertex) { return data[vertex * 6 + 3]; }
        public float u(int vertex) { return data[vertex * 6 + 4]; }
        public float v(int vertex) { return data[vertex * 6 + 5]; }

        // Preserve the original final float rounding, so the server intersects the
        // same vertex coordinates passed to the client renderer at scale 1.
        public float x(int vertex, float scale) { return data[vertex * 6] * scale; }

        public float y(int vertex, float scale) {
            double y = ORIGIN_Y + (data[vertex * 6 + 1] - ORIGIN_Y) * scale;
            return (float) (y + upwardLift(y));
        }

        public float z(int vertex, float scale) {
            double forwardScale = (1 + (scale - 1) * 0.2D) * VISUAL_LENGTH / BASE_VISUAL_LENGTH;
            return (float) (ORIGIN_Z + (data[vertex * 6 + 2] - ORIGIN_Z) * forwardScale);
        }
    }

    private static final class Builder {
        final int rings = RINGS;
        final int sides = SIDES;
        final int bursts = BURSTS;
        final Vector3d[] vertices = new Vector3d[(RINGS - 1) * SIDES + 2];
        final float[] heat = new float[vertices.length];
        final float[][] uv = new float[vertices.length][2];
        final float[] data = new float[((RINGS - 1) * SIDES * 2 + BURSTS * 6) * 4 * 6];
        final int[] componentStarts = new int[BURSTS + 2];
        final Random random = new Random();
        int cursor;

        void add(Vector3d point, float heat, float u, float v) {
            data[cursor++] = (float) point.x;
            data[cursor++] = (float) point.y;
            data[cursor++] = (float) point.z;
            data[cursor++] = heat;
            data[cursor++] = u;
            data[cursor++] = v;
        }
    }

    public static double upwardLift(double y) {
        // Keep the lower edge anchored while the upper surface reaches skyward.
        double upper = Math.max(0, Math.min(1, (y - ORIGIN_Y) / 8.0D));
        return EXTRA_UPWARD_REACH * upper * upper * (3 - 2 * upper);
    }
}
