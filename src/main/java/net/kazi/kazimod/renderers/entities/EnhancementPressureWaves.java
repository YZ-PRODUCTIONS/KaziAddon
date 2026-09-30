package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.EnhancementLightEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;

/** Concussion fronts and rolling ash around Third's existing white-gold eruption. */
final class EnhancementPressureWaves {
    private static final double TAU = Math.PI * 2.0D;
    // The owning renderer lowers its pose by 1.5 blocks. Keep ash just above the floor.
    private static final double GROUND_Y = 1.58D;

    private static final class States extends RenderState {
        private States() { super(null, null, null); }
        static final TransparencyState ALPHA = TRANSLUCENT_TRANSPARENCY;
        static final CullState CULL = NO_CULL;
        static final WriteMaskState WRITE = COLOR_WRITE;
        static final DepthTestState DEPTH = LEQUAL_DEPTH_TEST;
        static final ShadeModelState SHADE = SMOOTH_SHADE;
    }

    // Sorted, depth-tested ash stays translucent even when the player stands inside it.
    private static final RenderType ASH = RenderType.create("kazimod_enhancement_pressure_ash",
            DefaultVertexFormats.POSITION_COLOR, 7, 65536, false, true,
            RenderType.State.builder().setTransparencyState(States.ALPHA)
                    .setCullState(States.CULL).setWriteMaskState(States.WRITE)
                    .setDepthTestState(States.DEPTH).setShadeModelState(States.SHADE)
                    .createCompositeState(false));

    /** Geometry can be checked without a renderer, world, camera, or spawned particles. */
    interface VertexSink {
        void vertex(double x, double y, double z, float red, float green, float blue, float alpha);
    }

    private EnhancementPressureWaves() { }

    static void render(IRenderTypeBuffer buffer, MatrixStack stack,
                       float effectAge, int seed, float fade, int detail) {
        Matrix4f pose = stack.last().pose();
        // Finish each buffer's geometry before requesting another render type. Custom
        // buffers may flush on a type switch, so builders must never be interleaved.
        IVertexBuilder ash = buffer.getBuffer(ASH);
        generate((x, y, z, r, g, b, a) -> ash.vertex(pose, (float) x, (float) y, (float) z)
                .color(r, g, b, a).endVertex(), effectAge, seed, fade, detail, true);
        IVertexBuilder energy = buffer.getBuffer(CaladbolgVisualGeometry.ENERGY);
        generate((x, y, z, r, g, b, a) -> energy.vertex(pose, (float) x, (float) y, (float) z)
                .color(r, g, b, a).endVertex(), effectAge, seed, fade, detail, false);
    }

    static void generate(VertexSink out, float age, int seed, float fade, int detail, boolean dust) {
        float endAge = EnhancementLightEntity.BLAST_TICKS / EnhancementLightEntity.EFFECT_DURATION_SCALE;
        if (!Float.isFinite(age) || !Float.isFinite(fade) || age <= 0 || age >= endAge || fade <= 0.001F) return;
        fade = clamp(fade);
        int segments = VfxDetail.count(detail, 40, 28, 16);
        double rotation = (seed & 65535) * 0.017D;
        float finalAge = EnhancementBlastModel.fadeStartAge();

        if (dust) {
            // An initial ground roll gives the launch mass. Later, weaker rolls keep
            // the long hold alive without repeating the opening explosion every tick.
            dustWall(out, age - 2, 40, 1, 8, rotation, fade, segments, detail);
            dustWall(out, age - 32, 28, 0.50F, 29, rotation + 2.1D, fade, segments, detail);
            dustWall(out, age - 56, 28, 0.48F, 40, rotation + 4.3D, fade, segments, detail);
            dustWall(out, age - finalAge, endAge - finalAge, 0.80F, 48,
                    rotation + 5.7D, fade, segments, detail);
            return;
        }

        // Staggered fronts overtake the blast's expanding silhouette. Every detail
        // level retains the complete opening, restrained surges and final release.
        pressureFront(out, age, 20, 1, rotation, fade, segments, detail);
        pressureFront(out, age - 5, 20, 0.74F, rotation + 1.7D, fade, segments, detail);
        pressureFront(out, age - 10, 20, 0.53F, rotation + 3.4D, fade, segments, detail);
        pressureFront(out, age - 30, 22, 0.39F, rotation + 5.1D, fade, segments, detail);
        pressureFront(out, age - 54, 22, 0.37F, rotation + 6.8D, fade, segments, detail);
        pressureFront(out, age - finalAge, endAge - finalAge, 0.86F,
                rotation + 8.5D, fade, segments, detail);
        groundFront(out, age, 27, 0, 1, rotation, fade, segments);
        groundFront(out, age - 7, 27, 5, 0.58F, rotation + 3, fade, segments);
        groundFront(out, age - finalAge, endAge - finalAge, 48, 0.72F,
                rotation + 7, fade, segments);
    }

    private static void pressureFront(VertexSink out, float age, float life, float strength,
                                      double phase, float fade, int segments, int detail) {
        float envelope = envelope(age, life, 1.7F, 6) * fade * strength;
        if (envelope <= 0.001F) return;
        float t = age / life;
        double forward = 0.5D + 83.0D * easeOut(t);
        double radius = 1.5D + 19.5D * Math.sin(t * Math.PI * 0.78D);
        double centerY = 3.0D + 4.0D * Math.sin(t * Math.PI);
        // Bowed, feathered annuli read as compressed air; the open center preserves
        // the white-gold core and avoids covering the screen with a solid flash.
        for (int strip = 0; strip < 3; strip++) {
            for (int i = 0; i < segments; i++) {
                frontVertex(out, i, segments, strip, forward, centerY, radius, age, phase, envelope);
                frontVertex(out, i + 1, segments, strip, forward, centerY, radius, age, phase, envelope);
                frontVertex(out, i + 1, segments, strip + 1, forward, centerY, radius, age, phase, envelope);
                frontVertex(out, i, segments, strip + 1, forward, centerY, radius, age, phase, envelope);
            }
        }

        // Short ballistic streaks peel away from the moving front, with a bright
        // head and transparent tail instead of long opaque laser rods.
        int streaks = VfxDetail.count(detail, 20, 12, 6);
        for (int i = 0; i < streaks; i++) {
            double angle = i * TAU / streaks + phase;
            double variation = 0.5D + 0.5D * Math.sin(i * 7.13D + phase);
            double radial = radius * (0.90D + variation * 0.22D);
            double x = Math.cos(angle) * radial;
            double y = centerY + Math.sin(angle) * radial * 0.80D;
            double tail = 2.0D + variation * 6.0D;
            streak(out, x * 0.86D, centerY + (y - centerY) * 0.86D, forward - tail,
                    x, y, forward + variation, 0.05D + variation * 0.12D,
                    1, 0.89F, 0.55F, envelope * 0.65F);
        }
    }

    private static void frontVertex(VertexSink out, int segment, int segments, int band,
                                     double z, double centerY, double radius, float age,
                                     double phase, float alpha) {
        double angle = (segment % segments) * TAU / segments;
        double fraction = band == 0 ? 0.77D : band == 1 ? 0.96D : band == 2 ? 1.0D : 1.095D;
        double ripple = 1 + 0.025D * Math.sin(angle * 7 + phase - age * 0.21D)
                + 0.012D * Math.sin(angle * 13 - phase + age * 0.13D);
        double r = radius * fraction * ripple;
        float opacity = band == 0 || band == 3 ? 0 : band == 1 ? 0.17F : 0.52F;
        out.vertex(Math.cos(angle) * r, centerY + Math.sin(angle) * r * 0.80D,
                z - (1.095D - fraction) * 12.0D, 1, band == 2 ? 0.97F : 0.77F,
                band == 2 ? 0.78F : 0.31F, alpha * opacity);
    }

    private static void groundFront(VertexSink out, float age, float life, double centerZ,
                                    float strength, double phase, float fade, int segments) {
        float alpha = envelope(age, life, 1.5F, 9) * fade * strength;
        if (alpha <= 0.001F) return;
        double radius = 1.2D + 37.0D * easeOut(age / life);
        for (int band = 0; band < 2; band++) {
            for (int i = 0; i < segments; i++) {
                groundVertex(out, i, segments, band, radius, centerZ, age, phase, alpha);
                groundVertex(out, i + 1, segments, band, radius, centerZ, age, phase, alpha);
                groundVertex(out, i + 1, segments, band + 1, radius, centerZ, age, phase, alpha);
                groundVertex(out, i, segments, band + 1, radius, centerZ, age, phase, alpha);
            }
        }
    }

    private static void groundVertex(VertexSink out, int segment, int segments, int band,
                                      double radius, double centerZ, float age, double phase, float alpha) {
        double a = (segment % segments) * TAU / segments;
        double ragged = 1 + 0.022D * Math.sin(a * 9 + phase - age * 0.19D);
        double r = radius * ragged + (band - 1) * (0.6D + radius * 0.035D);
        out.vertex(Math.cos(a) * r, GROUND_Y + 0.08D * Math.sin(a * 5 + phase) + band * 0.035D,
                centerZ + Math.sin(a) * r, 1, 0.91F, 0.57F, band == 1 ? alpha * 0.68F : 0);
    }

    private static void dustWall(VertexSink out, float age, float life, float strength,
                                 double centerZ, double phase, float fade, int segments, int detail) {
        float alpha = envelope(age, life, 4, Math.min(12, life * 0.5F)) * fade * strength;
        if (alpha <= 0.001F) return;
        float t = age / life;
        double radius = 2.5D + (19.0D + strength * 8.0D) * easeOut(t);
        double width = (1.0D + 2.7D * Math.sin(t * Math.PI)) * strength;
        double height = (1.0D + 5.0D * Math.sin(t * Math.PI * 0.85D)) * strength;
        int bands = VfxDetail.count(detail, 6, 4, 3);
        for (int band = 0; band < bands; band++) {
            for (int i = 0; i < segments; i++) {
                dustVertex(out, i, segments, band, bands, radius, width, height, centerZ, age, phase, alpha);
                dustVertex(out, i + 1, segments, band, bands, radius, width, height, centerZ, age, phase, alpha);
                dustVertex(out, i + 1, segments, band + 1, bands, radius, width, height, centerZ, age, phase, alpha);
                dustVertex(out, i, segments, band + 1, bands, radius, width, height, centerZ, age, phase, alpha);
            }
        }
    }

    private static void dustVertex(VertexSink out, int segment, int segments, int band, int bands,
                                    double radius, double width, double height, double centerZ,
                                    float age, double phase, float alpha) {
        double angle = (segment % segments) * TAU / segments;
        double roll = band * Math.PI / bands;
        double lobes = 0.82D + 0.18D * Math.sin(angle * 7 + phase - age * 0.13D)
                + 0.10D * Math.sin(angle * 11 - phase + roll * 2 + age * 0.09D);
        double radial = radius * (1 + 0.04D * Math.sin(angle * 9 + phase)) + Math.cos(roll) * width * lobes;
        double crest = Math.sin(roll);
        double y = GROUND_Y + height * crest * lobes;
        // Amber inner light cools to earth-colored ash on the outward rolling edge.
        float heat = (float) (0.5D + 0.5D * Math.cos(roll + age * 0.11D + angle * 3 + phase));
        float shade = (float) (0.72D + 0.28D * lobes);
        float opacity = (band == 0 || band == bands) ? 0 : alpha * (float) crest * 0.26F;
        out.vertex(Math.cos(angle) * radial, y, centerZ + Math.sin(angle) * radial,
                (0.43F + heat * 0.32F) * shade, (0.34F + heat * 0.23F) * shade,
                (0.21F + heat * 0.12F) * shade, opacity);
    }

    private static void streak(VertexSink out, double ax, double ay, double az,
                                double bx, double by, double bz, double width,
                                float red, float green, float blue, float alpha) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.00001D || alpha <= 0.001F) return;
        dx /= length; dy /= length; dz /= length;
        double sx = -dz, sz = dx;
        double sideLength = Math.sqrt(sx * sx + sz * sz);
        if (sideLength < 0.00001D) return;
        sx = sx * width / sideLength;
        sz = sz * width / sideLength;
        streakFace(out, ax, ay, az, bx, by, bz, sx, 0, sz, red, green, blue, alpha);
        streakFace(out, ax, ay, az, bx, by, bz, dy * sz, dz * sx - dx * sz,
                -dy * sx, red, green, blue, alpha);
    }

    private static void streakFace(VertexSink out, double ax, double ay, double az,
                                    double bx, double by, double bz, double sx, double sy, double sz,
                                    float red, float green, float blue, float alpha) {
        out.vertex(ax - sx * 0.15D, ay - sy * 0.15D, az - sz * 0.15D, red, green, blue, 0);
        out.vertex(ax + sx * 0.15D, ay + sy * 0.15D, az + sz * 0.15D, red, green, blue, 0);
        out.vertex(bx + sx, by + sy, bz + sz, red, green, blue, alpha);
        out.vertex(bx - sx, by - sy, bz - sz, red, green, blue, alpha);
    }

    private static float envelope(float age, float life, float attack, float decay) {
        if (life <= 0 || age <= 0 || age >= life) return 0;
        return smooth(clamp(age / attack)) * smooth(clamp((life - age) / decay));
    }

    private static double easeOut(double progress) {
        double t = Math.max(0, Math.min(1, progress));
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }

    private static float smooth(float value) { return value * value * (3 - 2 * value); }
}
