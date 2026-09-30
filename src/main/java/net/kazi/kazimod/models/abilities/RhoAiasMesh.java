package net.kazi.kazimod.models.abilities;

/**
 * The procedural seven-petal Rho Aias effect from the alternate implementation.
 * It is untextured so every layer stays bright and translucent.
 */
public final class RhoAiasMesh {
    public static final int PETALS = 7;
    private static final int SEGMENTS = 24;
    private static final float FLOWER_RADIUS = 2.5F;
    private static final int PULSE_COUNT = 3;
    private static final float PULSE_DURATION = 36.0F;
    private static final float PI = (float) Math.PI;
    private static final int PINK = 0xFF62D9;
    private static final int PURPLE = 0x922BEE;
    private static final int WHITE = 0xFFF2FF;
    private static final int PULSE_PURPLE = 0xBD77FF;

    private static final float[][][] PETAL_POINTS = createPetalPoints();

    private RhoAiasMesh() { }

    public static void render(VertexConsumer out, float progress, float time, float opacity) {
        render(out, progress, time, opacity, 0);
    }

    public static void render(VertexConsumer out, float progress, float time, float opacity, int detail) {
        int step = detail + 1;
        int circleSegments = detail == 0 ? 64 : detail == 1 ? 32 : 16;
        float normalizedProgress = clamp(progress);
        float alpha = clamp(opacity) * smooth(normalizedProgress / 0.06F);
        if (alpha <= 0.0F) return;

        for (int layer = 6; layer >= 0; layer -= step) {
            float formation = smooth((normalizedProgress - 0.12F - layer * 0.055F) / 0.36F);
            if (formation <= 0.0F) continue;

            float radius = (FLOWER_RADIUS - layer * 0.16F) * formation;
            float layerAlpha = alpha * (layer == 0 ? 1.0F : 0.3F * step);
            for (int petal = 0; petal < PETALS; petal++) {
                float unfold = smooth((normalizedProgress - 0.12F - petal * 0.045F) / 0.34F);
                flowerPetal(out, petal, radius * unfold, -layer * 0.18F, layerAlpha, step);
            }
        }

        float flower = smooth((normalizedProgress - 0.15F) / 0.55F);
        // Staggered circles spread to the petal tips, then fade before restarting.
        for (int ring = 0; ring < PULSE_COUNT; ring++) {
            float elapsed = time - 6.0F - ring * (PULSE_DURATION / PULSE_COUNT);
            if (elapsed < 0.0F) continue;
            float phase = (elapsed % PULSE_DURATION) / PULSE_DURATION;
            float radius = FLOWER_RADIUS * flower * phase;
            float ringAlpha = alpha * flower * 0.35F * smooth(phase / 0.12F)
                    * (1.0F - smooth((phase - 0.8F) / 0.2F));
            expandingRing(out, radius, 0.08F + ring * 0.005F, PULSE_PURPLE, ringAlpha, circleSegments);
        }

        int arcs = detail == 0 ? 5 : detail == 1 ? 3 : 2;
        for (int arc = 0; arc < arcs; arc++) {
            for (int segment = 0; segment < 10; segment++) {
                float firstAngle = arc * PI * 2.0F / arcs + segment * 0.55F + time * 0.12F;
                float secondAngle = firstAngle + 0.55F;
                float firstRadius = 0.12F + 0.13F * sin(segment * 2.1F + time * 0.4F);
                float secondRadius = 0.12F + 0.13F * sin((segment + 1) * 2.1F + time * 0.4F);
                line(out, cos(firstAngle) * firstRadius, sin(firstAngle) * firstRadius,
                        -1.65F + segment * 0.15F,
                        cos(secondAngle) * secondRadius, sin(secondAngle) * secondRadius,
                        -1.5F + segment * 0.15F,
                        0.022F, 0x48FFBB, alpha * (1.0F - flower * 0.45F));
            }
        }

        for (int petal = 0; petal < PETALS; petal++) {
            float angle = PI * 0.5F + petal * PI * 2.0F / PETALS;
            float unfold = smooth((normalizedProgress - 0.12F - petal * 0.045F) / 0.34F);
            float tipRadius = FLOWER_RADIUS * unfold * smooth((normalizedProgress - 0.12F) / 0.36F);
            if (tipRadius > 1.0F) {
                star(out, cos(angle) * tipRadius, sin(angle) * tipRadius, 0.02F,
                        0.14F + 0.04F * sin(time * 0.4F + petal), 0xFFE4A3, alpha * unfold);
            }
        }

        float pulse = 1.0F + 0.1F * sin(time * 0.8F);
        disc(out, 0.35F * pulse + 0.24F * flower, 0.03F, PINK, alpha * 0.5F, circleSegments / 2);
        disc(out, 0.16F * pulse + 0.12F * flower, 0.04F, WHITE, alpha, circleSegments / 2);
        star(out, 0.0F, 0.0F, 0.06F, (0.35F + flower * 0.55F) * pulse, WHITE, alpha);
    }

    private static void flowerPetal(VertexConsumer out, int petal, float radius, float z, float alpha, int step) {
        if (radius < 0.01F) return;

        float centerAngle = PI * 0.5F + petal * PI * 2.0F / PETALS;
        for (int segment = 0; segment < SEGMENTS; segment += step) {
            float firstX = PETAL_POINTS[petal][segment][0] * radius;
            float firstY = PETAL_POINTS[petal][segment][1] * radius;
            float secondX = PETAL_POINTS[petal][segment + step][0] * radius;
            float secondY = PETAL_POINTS[petal][segment + step][1] * radius;

            out.vertex(0.0F, 0.0F, z, PINK, alpha * 0.25F);
            out.vertex(firstX, firstY, z, PURPLE, alpha * 0.22F);
            out.vertex(secondX, secondY, z, PURPLE, alpha * 0.22F);
            out.vertex(0.0F, 0.0F, z, PINK, alpha * 0.25F);
            line(out, firstX, firstY, z, secondX, secondY, z, 0.065F, PINK, alpha * 0.15F);
            line(out, firstX, firstY, z, secondX, secondY, z, 0.014F, PINK, alpha * 0.95F);
        }
        line(out, 0.0F, 0.0F, z, cos(centerAngle) * radius, sin(centerAngle) * radius,
                z, 0.008F, PINK, alpha * 0.22F);
    }

    private static float[][][] createPetalPoints() {
        float[][][] points = new float[PETALS][SEGMENTS + 1][2];
        float halfWidth = 0.43982297F;
        for (int petal = 0; petal < PETALS; petal++) {
            float center = PI * 0.5F + petal * PI * 2.0F / PETALS;
            for (int segment = 0; segment <= SEGMENTS; segment++) {
                float angle = -halfWidth + 2.0F * halfWidth * segment / SEGMENTS;
                float radius = petalRadius(1, angle, halfWidth);
                points[petal][segment][0] = cos(center + angle) * radius;
                points[petal][segment][1] = sin(center + angle) * radius;
            }
        }
        return points;
    }

    private static float petalRadius(float radius, float angle, float halfWidth) {
        return radius * (float) Math.pow(Math.max(0.0F, cos(angle / halfWidth * PI * 0.5F)), 0.55F);
    }

    /** A feathered annulus, offset in front of the petals to avoid surface overlap. */
    private static void expandingRing(VertexConsumer out, float radius, float z, int color, float alpha, int segments) {
        if (radius < 0.01F || alpha <= 0.0F) return;
        float halfWidth = Math.min(0.10F, radius * 0.25F);
        for (int band = 0; band < 2; band++) {
            float innerRadius = radius - halfWidth + band * halfWidth;
            float outerRadius = innerRadius + halfWidth;
            float innerAlpha = band == 0 ? 0.0F : alpha;
            float outerAlpha = band == 0 ? alpha : 0.0F;
            for (int segment = 0; segment < segments; segment++) {
                float firstAngle = segment * PI * 2.0F / segments;
                float secondAngle = (segment + 1) * PI * 2.0F / segments;
                out.vertex(cos(firstAngle) * innerRadius, sin(firstAngle) * innerRadius, z, color, innerAlpha);
                out.vertex(cos(secondAngle) * innerRadius, sin(secondAngle) * innerRadius, z, color, innerAlpha);
                out.vertex(cos(secondAngle) * outerRadius, sin(secondAngle) * outerRadius, z, color, outerAlpha);
                out.vertex(cos(firstAngle) * outerRadius, sin(firstAngle) * outerRadius, z, color, outerAlpha);
            }
        }
    }

    private static void disc(VertexConsumer out, float radius, float z, int color, float alpha, int segments) {
        for (int segment = 0; segment < segments; segment++) {
            float firstAngle = segment * PI * 2.0F / segments;
            float secondAngle = (segment + 1) * PI * 2.0F / segments;
            out.vertex(0.0F, 0.0F, z, color, alpha);
            out.vertex(cos(firstAngle) * radius, sin(firstAngle) * radius, z, color, 0.0F);
            out.vertex(cos(secondAngle) * radius, sin(secondAngle) * radius, z, color, 0.0F);
            out.vertex(0.0F, 0.0F, z, color, alpha);
        }
    }

    private static void star(VertexConsumer out, float x, float y, float z, float radius, int color, float alpha) {
        for (int arm = 0; arm < 4; arm++) {
            float angle = arm * PI * 0.5F;
            float deltaX = cos(angle);
            float deltaY = sin(angle);
            out.vertex(x - deltaY * radius * 0.12F, y + deltaX * radius * 0.12F, z, color, alpha);
            out.vertex(x + deltaX * radius, y + deltaY * radius, z, color, 0.0F);
            out.vertex(x + deltaY * radius * 0.12F, y - deltaX * radius * 0.12F, z, color, alpha);
            out.vertex(x, y, z, color, alpha);
        }
    }

    private static void line(VertexConsumer out, float firstX, float firstY, float firstZ,
                             float secondX, float secondY, float secondZ, float width,
                             int color, float alpha) {
        float deltaX = secondX - firstX;
        float deltaY = secondY - firstY;
        float length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length < 0.00001F) return;

        float normalX = -deltaY / length * width;
        float normalY = deltaX / length * width;
        out.vertex(firstX + normalX, firstY + normalY, firstZ, color, alpha);
        out.vertex(secondX + normalX, secondY + normalY, secondZ, color, alpha);
        out.vertex(secondX - normalX, secondY - normalY, secondZ, color, alpha);
        out.vertex(firstX - normalX, firstY - normalY, firstZ, color, alpha);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float smooth(float value) {
        float clamped = clamp(value);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float sin(float value) {
        return (float) Math.sin(value);
    }

    private static float cos(float value) {
        return (float) Math.cos(value);
    }

    @FunctionalInterface
    public interface VertexConsumer {
        void vertex(float x, float y, float z, int color, float alpha);
    }
}
