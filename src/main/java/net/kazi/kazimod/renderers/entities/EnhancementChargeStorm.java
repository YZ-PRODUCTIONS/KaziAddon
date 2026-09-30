package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Inward wind and compression collars feeding the overhead eruption. */
final class EnhancementChargeStorm {
    private EnhancementChargeStorm() { }

    static void render(IVertexBuilder out, Matrix4f pose, float age, float formation,
                       int seed, float opacity, int detail) {
        if (opacity <= 0.001F || formation <= 0) return;
        float power = formation * formation;
        float phaseOffset = (seed & 255) * 0.071F;
        int streams = VfxDetail.count(detail, 12, 8, 5);
        int steps = VfxDetail.count(detail, 12, 9, 6);
        // Fixed paths rotate continuously; their moving bright fronts never reseed.
        for (int stream = 0; stream < streams; stream++) {
            double angle = stream * Math.PI * 2 / streams + phaseOffset - age * 0.028D;
            float travel = (age / 24.0F + stream / (float) streams) % 1.0F;
            for (int step = 0; step < steps; step++) {
                float t = step / (float) steps;
                float next = (step + 1) / (float) steps;
                float envelope = (float) Math.sin(t * Math.PI);
                float front = 0.5F + 0.5F * (float) Math.cos((t - travel) * Math.PI * 2);
                float alpha = opacity * power * envelope * (0.045F + 0.16F * front * front);
                Vector3d from = point(angle, t, power);
                Vector3d to = point(angle, next, power);
                CaladbolgVisualGeometry.beam(out, pose, from, to, 0.05F + power * 0.11F,
                        1.0F, 0.80F, 0.38F, alpha);
            }
        }
        // Air compresses into shrinking collars; each cycle fades at both ends.
        for (int collar = 0; collar < 4; collar++) {
            float t = (age / 30.0F + collar * 0.25F) % 1.0F;
            float envelope = (float) Math.sin(t * Math.PI);
            envelope *= envelope;
            float radius = (1 - t) * (3.0F + power * 5.0F) + 0.35F;
            float height = 0.65F + t * (2.0F + formation * 11.0F);
            CaladbolgVisualGeometry.ring(out, pose, radius, height,
                    0.08F + power * 0.17F, age * 0.07F + collar,
                    1.0F, 0.91F, 0.65F, opacity * power * envelope * 0.28F,
                    VfxDetail.count(detail, 64, 40, 24));
        }
    }

    private static Vector3d point(double angle, float t, float power) {
        double radius = (1 - t) * (1 - t) * (3.5D + power * 6) + 0.22D;
        double twist = angle + t * 1.6D;
        return new Vector3d(Math.cos(twist) * radius, 0.55D + t * (2.2D + power * 12),
                Math.sin(twist) * radius + 0.35D);
    }
}
