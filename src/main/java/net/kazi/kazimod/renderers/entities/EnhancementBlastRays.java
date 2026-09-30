package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Random;
import net.kazi.kazimod.entities.EnhancementLightEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Translucent outward-moving rays layered over, not replacing, the main blast. */
final class EnhancementBlastRays {
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final int PAIRS_PER_TICK = 3;
    private static final int MAX_RAY_AGE = 19;

    private EnhancementBlastRays() { }

    static void render(IVertexBuilder out, Matrix4f pose, float age, int seed, float fade, int detail) {
        if (age < 0 || age >= EnhancementLightEntity.BLAST_TICKS || fade <= 0.001F) return;
        // Ray lifetimes and emission intervals scale with the rest of the effect.
        age /= EnhancementLightEntity.EFFECT_DURATION_SCALE;
        // Emit fresh rays every animation tick, staggered within it for a continuous flow.
        // Only regenerate still-visible emissions: no growing list or server entities.
        int latestEmission = (int) Math.floor(age);
        int earliestEmission = Math.max(0, latestEmission - MAX_RAY_AGE);
        int finalBurstEmission = (int) Math.ceil(EnhancementBlastModel.fadeStartAge());
        for (int emission = earliestEmission; emission <= latestEmission; emission++) {
            boolean burstEmission = emission == 0 || emission == finalBurstEmission;
            if (detail == 2 && emission % 2 != 0 && !burstEmission) continue;
            float emissionAge = age - emission;
            Random random = new Random(seed * 1297L + emission * 7919L);
            double rotation = random.nextDouble() * Math.PI * 2;
            Vector3d origin = emission == 0 ? new Vector3d(0, 3, 0.5D)
                    : emission == finalBurstEmission ? new Vector3d(0, 8, 28)
                    : new Vector3d(0, 7 + random.nextDouble() * 4, 16 + random.nextDouble() * 40);
            // Anchor each emission inside the blast's size when it was born.
            origin = new Vector3d(origin.x,
                    3 + (origin.y - 3) * EnhancementBlastModel.launch(emission, 6.0D),
                    0.5D + (origin.z - 0.5D) * EnhancementBlastModel.launch(emission, 8.0D));
            int pairs = burstEmission ? VfxDetail.count(detail, 24, 12, 6)
                    : VfxDetail.count(detail, PAIRS_PER_TICK, 2, 1);
            for (int pair = 0; pair < pairs; pair++) {
                float delay = random.nextFloat();
                float lifetime = 13 + random.nextFloat() * 5;
                double speed = 1.4D + random.nextDouble() * 0.8D;
                float length = 7 + random.nextFloat() * 8;
                float width = 0.25F + random.nextFloat() * 0.45F;
                float rayAge = emissionAge - delay;
                if (rayAge <= 0 || rayAge >= lifetime) continue;
                Vector3d direction = rayDirection(pair, pairs, rotation);
                float progress = rayAge / lifetime;
                // Continue emitting, but let late rays emerge softly as the blast dies.
                float emissionFade = 1 - EnhancementBlastModel.fadeProgress(emission + delay);
                float lifetimeFade = (1 - progress) * (1 - progress) * (1 + 2 * progress);
                float onset = Math.min(1, rayAge / 2.0F);
                onset = onset * onset * (3 - 2 * onset);
                float opacity = fade * onset
                        * lifetimeFade * emissionFade * (burstEmission ? 0.34F : 0.23F);
                double distance = 1 + rayAge * speed;
                double rayLength = length * onset;
                // Opposite pairs guarantee front/back, left/right and up/down coverage.
                for (int sign = -1; sign <= 1; sign += 2) {
                    Vector3d outward = direction.scale(sign);
                    Vector3d head = origin.add(outward.scale(distance));
                    Vector3d tail = origin.add(outward.scale(Math.max(0, distance - rayLength)));
                    ray(out, pose, tail, head, outward, width, opacity);
                }
            }
        }
    }

    private static Vector3d rayDirection(int pair, int pairs, double rotation) {
        // Evenly spaced latitudes on a hemisphere; opposite rays fill the other half.
        double y = (pair + 0.5D) / pairs;
        double radius = Math.sqrt(1 - y * y);
        double angle = pair * GOLDEN_ANGLE + rotation;
        return new Vector3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static void ray(IVertexBuilder out, Matrix4f pose, Vector3d tail, Vector3d head,
                             Vector3d direction, float width, float alpha) {
        Vector3d reference = Math.abs(direction.y) > 0.9D
                ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d side = direction.cross(reference).normalize();
        Vector3d other = direction.cross(side).normalize();
        Vector3d middle = tail.add(head.subtract(tail).scale(0.4D));
        // Crossed, feathered faces remain visible in F5 without becoming opaque rods.
        for (Vector3d axis : new Vector3d[]{side, other}) {
            ribbon(out, pose, tail, middle, head, axis.scale(width * 2.4D),
                    1, 0.73F, 0.2F, alpha * 0.3F);
            ribbon(out, pose, tail, middle, head, axis.scale(width),
                    1, 1, 0.82F, alpha);
        }
    }

    private static void ribbon(IVertexBuilder out, Matrix4f pose, Vector3d tail, Vector3d middle,
                                Vector3d head, Vector3d side, float r, float g, float b, float alpha) {
        Vector3d endWidth = side.scale(0.08D);
        CaladbolgVisualGeometry.vertex(out, pose, tail.subtract(endWidth), r, g, b, 0);
        CaladbolgVisualGeometry.vertex(out, pose, tail.add(endWidth), r, g, b, 0);
        CaladbolgVisualGeometry.vertex(out, pose, middle.add(side), r, g, b, alpha);
        CaladbolgVisualGeometry.vertex(out, pose, middle.subtract(side), r, g, b, alpha);
        CaladbolgVisualGeometry.vertex(out, pose, middle.subtract(side), r, g, b, alpha);
        CaladbolgVisualGeometry.vertex(out, pose, middle.add(side), r, g, b, alpha);
        CaladbolgVisualGeometry.vertex(out, pose, head.add(endWidth), r, g, b, 0);
        CaladbolgVisualGeometry.vertex(out, pose, head.subtract(endWidth), r, g, b, 0);
    }
}
