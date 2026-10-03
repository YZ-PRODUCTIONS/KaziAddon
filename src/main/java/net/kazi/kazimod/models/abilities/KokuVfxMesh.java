package net.kazi.kazimod.models.abilities;

public final class KokuVfxMesh {
    public static final int RED = 0;
    public static final int BLUE = 1;
    public static final int PURPLE = 2;
    private static final float PI = (float)Math.PI;
    private static final int[] COLORS = new int[]{16713787, 36095, 10822143};
    private static final int[] LIGHTS = new int[]{16736652, 0x87FFFF, 15246591};
    private static final ThreadLocal<RedNukeSink> RED_NUKE_SINK = ThreadLocal.withInitial(RedNukeSink::new);
    private static final RingGrid[] RING_GRIDS = {new RingGrid(96), new RingGrid(64), new RingGrid(40)};

    private KokuVfxMesh() {
    }

    public static void charge(Sink out, int mode, float progress, float time) {
        float p = KokuVfxMesh.clamp(progress);
        float appear = KokuVfxMesh.smooth(p / 0.1f);
        if (mode == 0) {
            float radius = 0.12f + 0.19f * KokuVfxMesh.smooth(p / 0.7f);
            KokuVfxMesh.orb(out, 0, time, radius, appear, false);
            float gather = 0.45f + 2.0f * (1.0f - KokuVfxMesh.smooth((p - 0.1f) / 0.7f));
            for (int i = 0; i < 6; ++i) {
                KokuVfxMesh.ribbon(out, time * 0.12f + (float)i * 1.2f, gather, 1.7f, (float)i * 0.7f, 0.12f * (1.0f - p) + 0.012f, COLORS[0], appear * (1.0f - KokuVfxMesh.smooth((p - 0.7f) / 0.2f)));
            }
            if (p > 0.85f) {
                KokuVfxMesh.sphere(out, radius * 0.82f, time, 0.0f, 16773850, KokuVfxMesh.smooth((p - 0.85f) / 0.15f) * 0.75f, false);
            }
        } else if (mode == 1 || mode == 5) {
            KokuVfxMesh.orb(out, 1, time, (mode == 5 ? 0.65f : 1.2f) * (0.25f + 0.75f * KokuVfxMesh.smooth(p)), appear, false);
            KokuVfxMesh.inwardStreams(out, time, mode == 5 ? 3.8f : 3.0f, appear);
        } else if (mode == 2) {
            float purple;
            float merge = KokuVfxMesh.smooth((p - 0.22f) / 0.43f);
            float separation = 2.5f * (1.0f - merge);
            float splitAlpha = 1.0f - KokuVfxMesh.smooth((p - 0.6f) / 0.12f);
            float radius = 0.9f + 0.2f * KokuVfxMesh.sin(time * 0.07f);
            if (splitAlpha > 0.0f) {
                KokuVfxMesh.chargeOrb(KokuVfxMesh.offset(out, separation, 0.2f, -0.7f), 0, time, radius, appear * splitAlpha);
                KokuVfxMesh.chargeOrb(KokuVfxMesh.offset(out, -separation, 0.2f, -0.7f), 1, time, radius, appear * splitAlpha);
            }
            if ((purple = KokuVfxMesh.smooth((p - 0.38f) / 0.36f)) > 0.0f) {
                float compress = 1.0f - KokuVfxMesh.smooth((p - 0.73f) / 0.2f) * 0.45f;
                KokuVfxMesh.chargeOrb(out, 2, time, (0.08f + 1.25f * purple) * compress, purple);
                KokuVfxMesh.lightning(out, time, 2.8f + purple, 8, LIGHTS[2], purple);
                KokuVfxMesh.debris(out, time, 2.3f * (1.0f - KokuVfxMesh.smooth((p - 0.7f) / 0.25f)), 12, purple * 0.7f, true);
            }
        }
    }

    private static void chargeOrb(Sink out, int style, float time, float radius, float alpha) {
        if (alpha < 0.01f || radius <= 0.0f) {
            return;
        }
        float phase = time * (style == 1 ? -0.095f : 0.08f);
        KokuVfxMesh.sphere(out, radius * 1.2f, phase, 0.018f, COLORS[style], alpha * 0.09f, false, 8, 16);
        KokuVfxMesh.sphere(out, radius * 0.82f, phase, style == 0 ? 0.035f : 0.13f, COLORS[style], alpha * 0.42f, true, 12, 24);
        KokuVfxMesh.sphere(out, radius, phase + 3.0f, style == 0 ? 0.07f : 0.2f, LIGHTS[style], alpha * 0.3f, true, 12, 24);
        KokuVfxMesh.sphere(out, radius * (style == 1 ? 0.36f : 0.45f), phase, 0.035f, style == 0 ? 16717132 : 0xE5FFFF, alpha * 0.6f, false, 8, 16);
        for (int i = 0; i < 4; ++i) {
            KokuVfxMesh.ribbon(out, (float)i * 1.77f + phase, radius * (1.0f + (float)i * 0.04f), 2.6f, (float)i * 0.9f, radius * (style == 0 ? 0.045f : 0.11f), LIGHTS[style], alpha * 0.72f);
        }
        if (style == 1) {
            KokuVfxMesh.inwardStreams(out, time, radius * 2.9f, alpha * 0.75f);
        }
        KokuVfxMesh.sparks(out, time, radius * 1.45f, 12, LIGHTS[style], alpha);
    }

    public static void orb(Sink out, int style, float time, float radius, float alpha, boolean flying) {
        float angle;
        if (alpha <= 0.0f || radius <= 0.0f) {
            return;
        }
        int color = COLORS[style];
        int light = LIGHTS[style];
        float phase = time * (style == 1 ? -0.095f : 0.08f);
        int shells = detailCount(out, 5, 3, 2);
        for (int layer = 0; layer < shells; ++layer) {
            int shell = 5 - layer * 4 / (shells - 1);
            // Keep the halo's inner/outer bounds and overall brightness at reduced detail.
            KokuVfxMesh.sphere(out, radius * (1.0f + (float)shell * 0.08f), phase, 0.018f, color, alpha * 0.018f * (5.0f / shells), false);
        }
        KokuVfxMesh.sphere(out, radius * 0.82f, phase, style == 0 ? 0.035f : 0.13f, color, alpha * 0.42f, true);
        KokuVfxMesh.sphere(out, radius, phase + 3.0f, style == 0 ? 0.07f : 0.2f, light, alpha * 0.3f, true);
        KokuVfxMesh.sphere(out, radius * (style == 0 ? 0.42f : (style == 1 ? 0.36f : 0.5f)), phase, style == 0 ? 0.035f : 0.14f, style == 0 ? 16717132 : 0xE5FFFF, alpha * 0.6f, false);
        for (int ribbon = 0; ribbon < detailCount(out, style == 0 ? 5 : 9, style == 0 ? 4 : 7, style == 0 ? 3 : 5); ++ribbon) {
            angle = (float)ribbon * 1.17f + phase;
            KokuVfxMesh.ribbon(out, angle, radius * (1.0f + (float)ribbon * 0.025f), 2.6f, (float)ribbon * 0.7f, radius * (style == 0 ? 0.045f : 0.11f), light, alpha * 0.72f);
        }
        if (style == 1) {
            KokuVfxMesh.inwardStreams(out, time, radius * 2.9f, alpha * 0.75f);
        }
        if (style == 2) {
            KokuVfxMesh.lightning(out, time, radius * 1.85f, 10, 16038655, alpha);
        }
        KokuVfxMesh.sparks(out, time, radius * 1.45f, style == 0 ? 26 : 42, light, alpha);
        if (flying) {
            int strands = detailCount(out, 8, 6, 4);
            int segments = detailCount(out, 18, 12, 8);
            for (int strand = 0; strand < strands; ++strand) {
                angle = (float)strand * (float)Math.PI / (strands / 2.0f) + phase;
                for (int segment = 0; segment < segments; ++segment) {
                    float u = (float)segment / segments;
                    float v = (float)(segment + 1) / segments;
                    float r1 = radius * (0.6f + u * 0.8f);
                    float r2 = radius * (0.6f + v * 0.8f);
                    float a = angle + u * 2.0f;
                    float b = angle + v * 2.0f;
                    KokuVfxMesh.line(out, KokuVfxMesh.cos(a) * r1, KokuVfxMesh.sin(a) * r1, -radius * (0.4f + u * 4.5f), KokuVfxMesh.cos(b) * r2, KokuVfxMesh.sin(b) * r2, -radius * (0.4f + v * 4.5f), radius * 0.1f * (1.0f - u), light, alpha * (1.0f - u) * 0.7f);
                }
            }
            if (style != 0) {
                KokuVfxMesh.debris(out, time, radius * 2.0f, 30, alpha, style == 1);
            }
        }
    }

    public static void impact(Sink out, int style, float progress, float time, float radius) {
        float p = KokuVfxMesh.clamp(progress);
        float fade = 1.0f - KokuVfxMesh.smooth(p);
        float wave = radius * (float)Math.pow(p, 0.55);
        KokuVfxMesh.sphere(out, Math.max(0.1f, wave), time * 0.15f, 0.04f, LIGHTS[style], fade * 0.12f, true);
        if (p < 0.3f) {
            KokuVfxMesh.sphere(out, radius * (0.15f + p), time, 0.08f, 0xFFF8FF, (1.0f - p / 0.3f) * 0.6f, false);
        }
        KokuVfxMesh.ring(out, wave, 0.0f, Math.max(0.025f, radius * 0.035f), LIGHTS[style], fade, false);
        KokuVfxMesh.ring(out, wave * 0.92f, 0.0f, Math.max(0.025f, radius * 0.025f), COLORS[style], fade * 0.6f, true);
        KokuVfxMesh.lightning(out, time, wave * 1.2f, style == 2 ? 18 : 8, LIGHTS[style], fade);
        KokuVfxMesh.debris(out, time, wave, 32, fade, false);
    }

    /** Ea's red impact: every original nuke feature is uniformly half-sized.
     * waveRadius and groundOffset are supplied in the final world-space dimensions.
     */
    public static void redNuke(Sink out, float age, float waveRadius, float opacity, float groundOffset) {
        RedNukeSink red = RED_NUKE_SINK.get();
        red.out = out;
        try {
            nuke(red, age, waveRadius * 2, opacity, groundOffset * 2);
        } finally {
            red.out = null;
        }
    }

    private static final class RedNukeSink implements Sink {
        private Sink out;
        private int previousColor = -1, crimson;
        @Override public int detail() { return out.detail(); }
        @Override public void vertex(float x, float y, float z, int color, float alpha) {
            // Sphere vertices share one color; remap it once instead of per corner.
            if (color != previousColor) {
                int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
                int bright = Math.max(r, Math.max(g, b)), dark = Math.min(r, Math.min(g, b));
                // Preserve brightness and white-hot highlights while moving the hue to red.
                crimson = bright << 16 | dark << 8 | dark;
                previousColor = color;
            }
            out.vertex(x * 0.5F, y * 0.5F, z * 0.5F, crimson, alpha);
        }
    }

    public static void nuke(Sink out, float age, float waveRadius, float opacity, float groundOffset) {
        if (opacity <= 0.0f) {
            return;
        }
        if (age < 40.0f) {
            float p = KokuVfxMesh.clamp(age / 40.0f);
            KokuVfxMesh.orb(out, 2, age, 0.8f + KokuVfxMesh.smooth(p) * 7.2f, opacity, false);
            KokuVfxMesh.ring(out, 2.0f + p * 9.0f, groundOffset, 0.18f, 10607871, opacity * 0.75f, true);
            return;
        }
        float radius = Math.max(1.0f, waveRadius);
        if (radius < 8.0f) {
            KokuVfxMesh.orb(out, 2, age, 8.0f, opacity * (1.0f - KokuVfxMesh.smooth(radius / 8.0f)), false);
        }
        KokuVfxMesh.sphere(out, radius, age * 0.065f, 0.05f, 13127423, opacity * 0.5f, true);
        KokuVfxMesh.sphere(out, radius * 0.9f, age * 0.13f, 0.07f, 15180543, opacity * 0.4f, true);
        KokuVfxMesh.sphere(out, radius * 0.83f, age * 0.11f, 0.035f, 0xFFE7FF, opacity * 0.5f, false);
        KokuVfxMesh.lightning(out, age, radius * 1.12f, 20, 16111359, opacity * 0.9f);
        float groundRadius = (float)Math.sqrt(Math.max(0.0f, radius * radius - groundOffset * groundOffset));
        for (int pulse = 0; pulse < 3; ++pulse) {
            float lag = (age - 40.0f + (float)(pulse * 7)) % 24.0f / 24.0f;
            KokuVfxMesh.ring(out, groundRadius * (1.0f - lag * 0.25f), groundOffset + (float)pulse * 0.4f, 0.2f + radius * 0.018f, pulse == 0 ? 16313343 : 12095980, opacity * (1.0f - lag) * 0.8f, true);
            KokuVfxMesh.ring(out, radius * (1.0f - (float)pulse * 0.075f), 0.0f, 0.12f + radius * 0.012f, 15450879, opacity * 0.45f, false);
        }
        KokuVfxMesh.debris(KokuVfxMesh.offset(out, 0.0f, groundOffset, 0.0f), age, groundRadius, 72, opacity, false);
    }

    private static void sphere(Sink out, float radius, float phase, float distortion, int color, float alpha, boolean turbulent) {
        int latitude = turbulent ? 24 : 12;
        int longitude = turbulent ? 48 : 24;
        KokuVfxMesh.sphere(out, radius, phase, distortion, color, alpha, turbulent, latitude, longitude);
    }

    private static void sphere(Sink out, float radius, float phase, float distortion, int color, float alpha, boolean turbulent, int latitude, int longitude) {
        if (radius <= 0.0f || alpha <= 0.0f) {
            return;
        }
        int rows = detailCount(out, latitude, Math.max(6, latitude * 2 / 3), Math.max(4, latitude / 2));
        KokuSphereGrid.render(out, radius, phase, distortion, color, alpha, turbulent, rows);
    }

    private static void ribbon(Sink out, float start, float radius, float length, float tilt, float width, int color, float alpha) {
        if (alpha <= 0.0F || radius <= 0.0F) return;
        int segments = detailCount(out, 28, 20, 12);
        float cosTilt = KokuVfxMesh.cos(tilt);
        float sinTilt = KokuVfxMesh.sin(tilt);
        for (int i = 0; i < segments; ++i) {
            float u = (float)i / segments;
            float v = (float)(i + 1) / segments;
            float a = start + u * length;
            float b = start + v * length;
            float taper = KokuVfxMesh.sin((float)Math.PI * u);
            float y1 = KokuVfxMesh.sin(a) * radius;
            float y2 = KokuVfxMesh.sin(b) * radius;
            KokuVfxMesh.line(out, KokuVfxMesh.cos(a) * radius, y1 * cosTilt, y1 * sinTilt, KokuVfxMesh.cos(b) * radius, y2 * cosTilt, y2 * sinTilt, width * Math.max(0.02f, taper), color, alpha * taper);
        }
    }

    private static void inwardStreams(Sink out, float time, float radius, float alpha) {
        if (alpha <= 0.0F || radius <= 0.0F) return;
        int segments = detailCount(out, 40, 28, 16);
        for (int strand = 0; strand < detailCount(out, 5, 4, 3); ++strand) {
            float start = (float)strand * 2.399963f - time * 0.085f + KokuVfxMesh.sin(time * 0.12f + (float)strand) * 0.2f;
            float tilt = KokuVfxMesh.sin((float)strand * 2.7f) * 1.4f;
            float reach = radius * (0.85f + KokuVfxMesh.sin(strand * 7) * 0.15f);
            float wrap = 2.1f + KokuVfxMesh.sin(strand * 7) * 0.7f;
            float cosTilt = KokuVfxMesh.cos(tilt);
            float sinTilt = KokuVfxMesh.sin(tilt);
            for (int segment = 0; segment < segments; ++segment) {
                float u = (float)segment / segments;
                float v = (float)(segment + 1) / segments;
                float a = start + u * wrap;
                float b = start + v * wrap;
                float r1 = reach * (1.0f - u * 0.83f);
                float r2 = reach * (1.0f - v * 0.83f);
                KokuVfxMesh.line(out, KokuVfxMesh.cos(a) * r1, KokuVfxMesh.sin(a) * r1 * cosTilt, KokuVfxMesh.sin(a) * r1 * sinTilt, KokuVfxMesh.cos(b) * r2, KokuVfxMesh.sin(b) * r2 * cosTilt, KokuVfxMesh.sin(b) * r2 * sinTilt, radius * 0.023f * KokuVfxMesh.sin((float)Math.PI * u), strand % 3 == 0 ? 1875199 : 11991295, alpha * KokuVfxMesh.sin((float)Math.PI * u) * 0.7f);
            }
        }
    }

    private static void lightning(Sink out, float time, float radius, int count, int color, float alpha) {
        if (alpha <= 0.0F || radius <= 0.0F) return;
        count = detailCount(out, count, (count * 3 + 3) / 4, (count + 1) / 2);
        float frame = (float)Math.floor(time / 2.0f);
        for (int bolt = 0; bolt < count; ++bolt) {
            float angle = (float)bolt * 2.399963f;
            float cosAngle = KokuVfxMesh.cos(angle);
            float sinAngle = KokuVfxMesh.sin(angle);
            float tilt = KokuVfxMesh.sin((float)bolt * 9.4f + frame * 0.1f);
            float previousX = cosAngle * radius * 0.45f;
            float previousY = sinAngle * radius * 0.45f;
            float previousZ = tilt * radius * 0.25f;
            for (int segment = 1; segment <= 7; ++segment) {
                float u = (float)segment / 7.0f;
                float jitter = KokuVfxMesh.sin((float)(bolt * 27 + segment * 13) + frame * 5.0f) * radius * 0.095f;
                float x = cosAngle * radius * (0.45f + u * 0.65f) - sinAngle * jitter;
                float y = sinAngle * radius * (0.45f + u * 0.65f) + cosAngle * jitter;
                float z = tilt * radius * (u - 0.2f) + jitter * 0.5f;
                KokuVfxMesh.line(out, previousX, previousY, previousZ, x, y, z, radius * 0.009f, color, alpha * (1.0f - u * 0.5f));
                KokuVfxMesh.line(out, previousX, previousY, previousZ, x, y, z, radius * 0.0028f, 0xFFFFFF, alpha * 0.9f);
                previousX = x;
                previousY = y;
                previousZ = z;
            }
        }
    }

    private static void sparks(Sink out, float time, float radius, int count, int color, float alpha) {
        if (alpha <= 0.0F || radius <= 0.0F) return;
        count = detailCount(out, count, (count * 3 + 3) / 4, (count + 1) / 2);
        for (int i = 0; i < count; ++i) {
            float t = KokuVfxMesh.fract((float)i * 0.618f + time * 0.015f);
            float a = (float)i * 2.399963f + time * 0.03f;
            float r = radius * (0.6f + t * 0.6f);
            float y = KokuVfxMesh.sin((float)i * 7.4f) * r;
            float x = KokuVfxMesh.cos(a) * r;
            float z = KokuVfxMesh.sin(a) * r;
            float size = radius * (0.012f + 0.015f * KokuVfxMesh.fract((float)i * 0.317f));
            KokuVfxMesh.line(out, x - size, y, z, x + size, y, z, size * 0.4f, color, alpha * KokuVfxMesh.sin(t * (float)Math.PI));
            KokuVfxMesh.line(out, x, y - size, z, x, y + size, z, size * 0.4f, color, alpha * KokuVfxMesh.sin(t * (float)Math.PI));
        }
    }

    private static void debris(Sink out, float time, float radius, int count, float alpha, boolean inward) {
        if (alpha <= 0.0F) return;
        count = detailCount(out, count, (count * 3 + 3) / 4, (count + 1) / 2);
        for (int i = 0; i < count; ++i) {
            float t = KokuVfxMesh.fract((float)i * 0.618f + time * 0.012f);
            float a = (float)i * 2.399963f + (inward ? time * 0.025f : 0.0f);
            float r = radius * (inward ? 1.0f - t * 0.8f : 0.7f + t * 0.5f);
            float x = KokuVfxMesh.cos(a) * r;
            float z = KokuVfxMesh.sin(a) * r;
            float y = inward ? KokuVfxMesh.sin((float)i * 7.3f) * r : KokuVfxMesh.sin(t * (float)Math.PI) * Math.min(7.0f, radius * 0.35f);
            float size = Math.min(0.4f, 0.05f + radius * 0.014f) * (0.7f + KokuVfxMesh.fract((float)i * 0.39f));
            int color = i % 3 == 0 ? 11249081 : 5327203;
            float fade = alpha * KokuVfxMesh.sin(t * (float)Math.PI) * 0.75f;
            KokuVfxMesh.quad(out, x - size, y - size, z, x + size, y - size, z, x + size, y + size, z, x - size, y + size, z, color, fade);
            KokuVfxMesh.quad(out, x, y - size, z - size, x, y - size, z + size, x, y + size, z + size, x, y + size, z - size, color, fade);
        }
    }

    private static void ring(Sink out, float radius, float y, float width, int color, float alpha, boolean horizontal) {
        if (radius <= 0.0f || alpha <= 0.0f) {
            return;
        }
        RingGrid grid = RING_GRIDS[detailCount(out, 0, 1, 2)];
        int segments = grid.cos.length - 1;
        for (int i = 0; i < segments; ++i) {
            float ca = grid.cos[i], sa = grid.sin[i];
            float cb = grid.cos[i + 1], sb = grid.sin[i + 1];
            if (horizontal) {
                KokuVfxMesh.quad(out, ca * (radius - width), y, sa * (radius - width),
                        ca * (radius + width), y, sa * (radius + width),
                        cb * (radius + width), y, sb * (radius + width),
                        cb * (radius - width), y, sb * (radius - width), color, alpha);
                continue;
            }
            KokuVfxMesh.line(out, ca * radius, sa * radius, 0.0f, cb * radius, sb * radius,
                    0.0f, width, color, alpha);
        }
    }

    /** Immutable unit circles shared by every shockwave; preserve the original seam endpoint. */
    private static final class RingGrid {
        private final float[] cos, sin;
        private RingGrid(int segments) {
            cos = new float[segments + 1];
            sin = new float[segments + 1];
            for (int i = 0; i <= segments; i++) {
                float angle = (float) i * (float) Math.PI / (segments / 2.0F);
                cos[i] = KokuVfxMesh.cos(angle);
                sin[i] = KokuVfxMesh.sin(angle);
            }
        }
    }

    private static Sink offset(Sink out, float x, float y, float z) {
        return withDetail((vx, vy, vz, color, alpha) -> out.vertex(vx + x, vy + y, vz + z, color, alpha), out.detail());
    }

    /** Per-draw quality; never global mutable state or a change to the shared material. */
    public static Sink withDetail(Sink out, int detail) {
        final int level = Math.max(0, Math.min(2, detail));
        if (out.detail() == level) return out;
        return new Sink() {
            public void vertex(float x, float y, float z, int color, float alpha) {
                out.vertex(x, y, z, color, alpha);
            }

            public int detail() { return level; }
        };
    }

    private static int detailCount(Sink out, int high, int medium, int low) {
        return out.detail() == 0 ? high : out.detail() == 1 ? medium : low;
    }

    private static void line(Sink out, float x1, float y1, float z1, float x2, float y2, float z2, float width, int color, float alpha) {
        if (width <= 0.0F || alpha <= 0.0F) return;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float nx = -dy;
        float ny = dx;
        float nz = 0.0f;
        float length = (float)Math.sqrt(nx * nx + ny * ny);
        if (length < 1.0E-5f) {
            nx = -dz;
            ny = 0.0f;
            nz = dx;
            length = (float)Math.sqrt(nx * nx + nz * nz);
        }
        if (length < 1.0E-5f || width <= 0.0f || alpha <= 0.0f) {
            return;
        }
        KokuVfxMesh.quad(out, x1 + (nx *= width / length), y1 + (ny *= width / length), z1 + (nz *= width / length), x2 + nx, y2 + ny, z2 + nz, x2 - nx, y2 - ny, z2 - nz, x1 - nx, y1 - ny, z1 - nz, color, KokuVfxMesh.clamp(alpha));
        float bx = dy * nz - dz * ny;
        float by = dz * nx - dx * nz;
        float bz = dx * ny - dy * nx;
        float crossLength = (float)Math.sqrt(bx * bx + by * by + bz * bz);
        if (crossLength > 1.0E-5f) {
            KokuVfxMesh.quad(out, x1 + (bx *= width / crossLength), y1 + (by *= width / crossLength), z1 + (bz *= width / crossLength), x2 + bx, y2 + by, z2 + bz, x2 - bx, y2 - by, z2 - bz, x1 - bx, y1 - by, z1 - bz, color, KokuVfxMesh.clamp(alpha * 0.7f));
        }
    }

    private static void quad(Sink out, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int color, float alpha) {
        out.vertex(ax, ay, az, color, alpha);
        out.vertex(bx, by, bz, color, alpha);
        out.vertex(cx, cy, cz, color, alpha);
        out.vertex(dx, dy, dz, color, alpha);
    }

    private static float sin(float x) {
        return (float)Math.sin(x);
    }

    private static float cos(float x) {
        return (float)Math.cos(x);
    }

    private static float fract(float x) {
        return x - (float)Math.floor(x);
    }

    private static float clamp(float x) {
        return Math.max(0.0f, Math.min(1.0f, x));
    }

    private static float smooth(float x) {
        float t = KokuVfxMesh.clamp(x);
        return t * t * (3.0f - 2.0f * t);
    }

    @FunctionalInterface
    public static interface Sink {
        public void vertex(float var1, float var2, float var3, int var4, float var5);

        default int detail() { return 0; }
    }
}
