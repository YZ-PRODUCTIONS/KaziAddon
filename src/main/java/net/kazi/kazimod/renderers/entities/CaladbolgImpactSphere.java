package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.CaladbolgImpactEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Staggered red pressure spheres, centered on Caladbolg's ground impact. */
final class CaladbolgImpactSphere {
    private static final int LATITUDES = 32;
    private static final int LONGITUDES = 64;
    private static final int PULSE_COUNT = 6;
    private static final float PULSE_SPEED_MULTIPLIER = 1.5F;
    private static final float EXPAND_TICKS = 8.0F;
    private static final float FADE_START_TICKS = 8.0F;
    private static final float PULSE_LIFETIME_TICKS = 30.0F;
    private static final float PULSE_INTERVAL_TICKS =
            (CaladbolgImpactEntity.LIFETIME - PULSE_LIFETIME_TICKS) / (PULSE_COUNT - 1);
    private static final float[] PULSE_SCALES = {1.0F, 0.92F, 1.04F, 0.96F, 1.08F, 1.0F};
    private static final double CENTER_Y = 0.15D;
    private static final Vector3d[][] DIRECTIONS = createDirections();

    private static final class States extends RenderState {
        private States() { super(null, null, null); }
        static final TransparencyState ALPHA = TRANSLUCENT_TRANSPARENCY;
        static final CullState TWO_SIDED = NO_CULL;
        static final WriteMaskState COLOR_ONLY = COLOR_WRITE;
        static final DepthTestState DEPTH = LEQUAL_DEPTH_TEST;
        static final ShadeModelState SHADE = SMOOTH_SHADE;
    }

    // Real alpha blending keeps the sphere red, rather than washing it out to white.
    // No depth writes: the shell cannot cut opaque holes into the cloud or terrain.
    private static final RenderType SHELL = RenderType.create("kazimod_caladbolg_red_sphere",
            DefaultVertexFormats.POSITION_COLOR, 7, 262144, false, true,
            RenderType.State.builder().setTransparencyState(States.ALPHA)
                    .setCullState(States.TWO_SIDED).setWriteMaskState(States.COLOR_ONLY)
                    .setDepthTestState(States.DEPTH).setShadeModelState(States.SHADE)
                    .createCompositeState(false));

    private CaladbolgImpactSphere() { }

    static void render(IRenderTypeBuffer buffer, Matrix4f pose, float age,
                        float blastRadius, float seed, Vector3d cameraOffset, int detail) {
        // Speed up only the red spheres: the full pulse train now ends in 6 seconds instead of 9.
        age *= PULSE_SPEED_MULTIPLIER;
        if (age < 0.0F || age >= CaladbolgImpactEntity.LIFETIME) return;
        // Batch all translucent shells together, then their glows. The alpha pass
        // can sort overlapping spheres together instead of flushing once per pulse.
        pulses(buffer.getBuffer(SHELL), pose, age, blastRadius, seed, cameraOffset, detail, false);
        if (detail == 0) {
            pulses(buffer.getBuffer(CaladbolgVisualGeometry.ENERGY), pose, age,
                    blastRadius, seed, cameraOffset, detail, true);
        }
    }

    private static void pulses(IVertexBuilder out, Matrix4f pose, float age, float blastRadius,
                                 float seed, Vector3d camera, int detail, boolean glow) {
        // At most three pulses overlap. Coarser glow meshes keep the whole train
        // below the old high-detail single sphere's vertex budget; no new entities.
        int step = glow ? 4 : VfxDetail.count(detail, 2, 4, 4);
        for (int pulse = 0; pulse < PULSE_COUNT; pulse++) {
            float pulseAge = age - pulse * PULSE_INTERVAL_TICKS;
            if (pulseAge < 0.0F || pulseAge >= PULSE_LIFETIME_TICKS) continue;
            float fade = opacity(pulseAge) * (0.90F - pulse * 0.06F);
            if (fade <= 0.001F) continue;
            float radius = blastRadius * PULSE_SCALES[pulse] * expansion(pulseAge);
            if (glow) radius *= 1.012F;
            mesh(out, pose, radius, pulseAge, seed + pulse * 1.71F, fade, camera, glow, step);
        }
    }

    private static float expansion(float age) {
        float t = clamp(age / EXPAND_TICKS);
        float remaining = 1.0F - t;
        // Fast launch that decelerates into the impact's existing slow expansion.
        return 0.04F + 0.96F * (1.0F - remaining * remaining * remaining);
    }

    private static float opacity(float age) {
        float t = clamp((age - FADE_START_TICKS) / (PULSE_LIFETIME_TICKS - FADE_START_TICKS));
        float appear = clamp(age / 2.0F);
        appear = appear * appear * (3.0F - 2.0F * appear);
        return appear * (1.0F - t * t * (3.0F - 2.0F * t));
    }

    private static void mesh(IVertexBuilder out, Matrix4f pose, float radius, float age,
                              float seed, float fade, Vector3d camera, boolean glow, int step) {
        for (int latitude = 0; latitude < LATITUDES; latitude += step) {
            for (int longitude = 0; longitude < LONGITUDES; longitude += step) {
                emit(out, pose, DIRECTIONS[latitude][longitude], radius, age, seed, fade, camera, glow);
                emit(out, pose, DIRECTIONS[latitude + step][longitude], radius, age, seed, fade, camera, glow);
                emit(out, pose, DIRECTIONS[latitude + step][longitude + step], radius, age, seed, fade, camera, glow);
                emit(out, pose, DIRECTIONS[latitude][longitude + step], radius, age, seed, fade, camera, glow);
            }
        }
    }

    private static void emit(IVertexBuilder out, Matrix4f pose, Vector3d direction, float radius,
                              float age, float seed, float fade, Vector3d camera, boolean glow) {
        float ignition = 1.0F - clamp(age / 12.0F);
        // A shallow travelling pressure ripple keeps the outline explosive but spherical.
        double ripple = 1.0D + 0.014D * ignition
                * Math.sin(direction.x * 11.0D + direction.z * 9.0D + seed - age * 0.45D)
                * (1.0D - direction.y * direction.y);
        double x = direction.x * radius * ripple;
        double y = direction.y * radius * ripple + CENTER_Y;
        double z = direction.z * radius * ripple;
        double viewX = camera.x - x;
        double viewY = camera.y - y;
        double viewZ = camera.z - z;
        double length = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        // View-dependent rim works from outside, inside, and third-person cameras.
        float facing = length > 0.0001D ? (float) Math.abs(
                (direction.x * viewX + direction.y * viewY + direction.z * viewZ) / length) : 1.0F;
        float rim = 1.0F - clamp(facing);
        float edge = rim * rim;
        float alpha = fade * (glow ? 0.16F * edge * rim : 0.055F + 0.22F * edge);
        float green = glow ? 0.10F : 0.025F + ignition * 0.045F;
        float blue = glow ? 0.045F : 0.035F;
        out.vertex(pose, (float) x, (float) y, (float) z)
                .color(1.0F, green, blue, alpha).endVertex();
    }

    private static Vector3d[][] createDirections() {
        Vector3d[][] directions = new Vector3d[LATITUDES + 1][LONGITUDES + 1];
        for (int latitude = 0; latitude <= LATITUDES; latitude++) {
            double pitch = latitude * Math.PI / LATITUDES;
            double horizontal = Math.sin(pitch);
            double y = Math.cos(pitch);
            if (latitude == 0 || latitude == LATITUDES) horizontal = 0.0D;
            for (int longitude = 0; longitude < LONGITUDES; longitude++) {
                double yaw = longitude * Math.PI * 2.0D / LONGITUDES;
                directions[latitude][longitude] = new Vector3d(
                        Math.cos(yaw) * horizontal, y, Math.sin(yaw) * horizontal);
            }
            // Reuse the exact seam vertex to avoid cracks while the sphere ripples.
            directions[latitude][LONGITUDES] = directions[latitude][0];
        }
        return directions;
    }

    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
