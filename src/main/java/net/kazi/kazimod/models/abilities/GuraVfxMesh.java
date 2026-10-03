package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.models.abilities.KokuVfxMesh.Sink;

/** Fixed-topology, progressively revealed fractures, rather than flickering particle sprites. */
public final class GuraVfxMesh {
    private static final float TAU = (float) (Math.PI * 2);
    private GuraVfxMesh() {}

    public static float travelYaw(double x, double z) {
        return (float) Math.toDegrees(Math.atan2(x, z));
    }

    public static float travelPitch(double x, double y, double z) {
        return (float) -Math.toDegrees(Math.atan2(y, Math.hypot(x, z)));
    }

    public static float fade(float age, float life) {
        return Math.max(0, Math.min(1, (life - age) / 7));
    }

    public static void cracks(Sink out, float radius, float reveal, float alpha, int seed) {
        if (radius <= 0 || alpha <= 0 || reveal <= 0) return;
        // Every fork shares its parent's endpoint, so the fracture reads as broken glass.
        for (int ray = 0; ray < 13; ray++) {
            float angle = TAU * (ray + hash(seed + ray * 19) * .45F) / 13;
            float x = 0, y = 0;
            for (int step = 1; step <= 7; step++) {
                float end = step / 7F;
                float start = (step - 1) / 7F;
                if (reveal <= start) break;
                float bend = (hash(seed + ray * 71 + step * 11) - .5F) * .38F;
                float length = radius * end * (.76F + .24F * hash(seed + ray));
                float nx = cos(angle + bend) * length, ny = sin(angle + bend) * length;
                float t = Math.min(1, (reveal - start) * 7);
                float ex = x + (nx - x) * t, ey = y + (ny - y) * t;
                float width = radius * (.006F + .012F * (1 - end));
                fissure(out, x, y, ex, ey, width, alpha);
                if (step == 3 || step == 5) {
                    float a = angle + ((ray + step) % 2 == 0 ? .7F : -.7F);
                    float bx = x + cos(a) * radius * .22F * t;
                    float by = y + sin(a) * radius * .22F * t;
                    fissure(out, x, y, bx, by, width * .55F, alpha * .85F);
                    fissure(out, bx, by, bx + cos(a + .35F) * radius * .12F * t,
                            by + sin(a + .35F) * radius * .12F * t, width * .25F, alpha * .7F);
                }
                x = nx; y = ny;
            }
        }
    }

    private static void fissure(Sink out, float x, float y, float ex, float ey, float width, float alpha) {
        line(out, x, y, ex, ey, width * 3.5F, 0x9BDBEE, alpha * .12F);
        line(out, x, y, ex, ey, width * 1.7F, 0xC9F2FF, alpha * .38F);
        line(out, x, y, ex, ey, width, 0xFFFFFF, alpha);
    }

    private static void line(Sink out, float x, float y, float ex, float ey, float w, int c, float a) {
        float dx = ex - x, dy = ey - y, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < .00001F) return;
        float px = -dy / len * w, py = dx / len * w;
        out.vertex(x + px, y + py, 0, c, a); out.vertex(x - px, y - py, 0, c, a);
        out.vertex(ex - px * .35F, ey - py * .35F, 0, c, a);
        out.vertex(ex + px * .35F, ey + py * .35F, 0, c, a);
    }

    public static void bubble(Sink out, float radius, float age, float alpha) {
        if (alpha <= 0 || radius <= 0) return;
        // Charges are fractured air, with no enclosing sphere or orbital rings.
        float pulse = 1 + .035F * sin(age * .3F);
        cracks(out, radius * pulse, 1, alpha, 71);
        cracks((x, y, z, c, a) -> out.vertex(z, y, x, c, a), radius * .7F, 1, alpha * .55F, 93);
    }

    public static void projectile(Sink out, float radius, float age, float alpha, int seed) {
        if (radius <= 0 || alpha <= 0) return;
        // Open pressure fronts continually spread and peel backward; there is no closed shell.
        for (int wave = 2; wave >= 0; wave--) {
            float phase = ((age / 13 + wave / 3F) % 1 + 1) % 1;
            float size = radius * (.75F + phase * .28F);
            front(out, size, -radius * phase * .65F, age, alpha * (1 - phase) * .85F, seed + wave);
        }
        cracks((x, y, z, c, a) -> {
            float depth = Math.max(0, 1 - (x * x + y * y) / (radius * radius));
            out.vertex(x, y, depth * radius * .32F + .03F, c, a);
        }, radius, Math.min(1, .35F + age / 5), alpha * .85F, seed);
        for (int i = 0; i < 28; i++) {
            float angle = TAU * i / 28;
            float phase = ((age * .065F + hash(seed + i * 17)) % 1 + 1) % 1;
            float r = radius * (.76F + .23F * hash(seed + i * 31));
            float z = -radius * phase * .8F;
            float width = radius * (.008F + .014F * hash(seed + i));
            float length = radius * (.22F + .5F * hash(seed + i * 7));
            float x = cos(angle) * r, y = sin(angle) * r;
            float px = -sin(angle) * width, py = cos(angle) * width;
            float a = alpha * sin(phase * (float) Math.PI) * .55F;
            out.vertex(x + px, y + py, z, 0xF1FCFF, a);
            out.vertex(x - px, y - py, z, 0xF1FCFF, a);
            out.vertex(x * 1.09F, y * 1.09F, z - length, 0xD7F3FF, 0);
            out.vertex(x * 1.09F, y * 1.09F, z - length, 0xD7F3FF, 0);
        }
    }

    private static void front(Sink out, float radius, float z, float age, float alpha, int seed) {
        // Broad, feathered arcs with an irregular crest and a transparent center.
        for (int band = 0; band < 4; band++) for (int col = 0; col < 64; col++) {
            if ((col + seed) % 19 == 0) continue;
            for (int corner = 0; corner < 4; corner++) {
                int edge = band + (corner >= 2 ? 1 : 0);
                float lon = TAU * (col + (corner == 1 || corner == 2 ? 1 : 0)) / 64;
                float ripple = .023F * sin(lon * 17 + age * .38F) + .018F * sin(lon * 29 - age * .21F);
                float fraction = edge == 0 ? .48F : edge == 1 ? .8F : edge == 2 ? .955F : edge == 3 ? .98F : 1.05F;
                float intensity = edge == 0 || edge == 4 ? 0 : edge == 1 ? .08F : edge == 2 ? .4F : 1;
                float r = radius * (fraction + ripple);
                out.vertex(cos(lon) * r, sin(lon) * r, z + radius * (1 - fraction * fraction) * .32F,
                        0xEAFBFF, alpha * intensity);
            }
        }
    }
    private static float hash(int n) { n = (n ^ 61) ^ (n >>> 16); n *= 9; n ^= n >>> 4; n *= 0x27d4eb2d; return (n & 65535) / 65535F; }
    private static float sin(float v) { return (float) Math.sin(v); }
    private static float cos(float v) { return (float) Math.cos(v); }
}
