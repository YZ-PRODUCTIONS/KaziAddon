package net.kazi.kazimod.client.asriel;

/** Deterministic, allocation-free suction field. Everything contracts into the open maw. */
public final class HyperGonerGravity {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2;
    private static final float[] CORNER_X = {1, 0, -1, 0};
    private static final float[] CORNER_Z = {0, 1, 0, -1};
    private static final int[] ROCK_COLORS = {0xA9ABB8, 0x797C8B, 0xCBCDD5, 0x9295A5,
            0x676A7A, 0xA0A4B3, 0x777B8D, 0xC3C8D7};

    private HyperGonerGravity() {}

    /** Coordinates are local to the skull; +Z points out of the mouth toward the viewer. */
    public static void render(HyperGonerMesh.Sink body, HyperGonerMesh.Sink glow,
                              float age, int detail, float mouthY, float mouthH,
                              float show, float pull) {
        if (!Float.isFinite(age) || !Float.isFinite(mouthY) || !Float.isFinite(mouthH)
                || !Float.isFinite(show) || !Float.isFinite(pull) || show <= 0 || pull <= 0) return;
        float opacity = clamp(show * pull);
        float vertical = Math.max(.5F, Math.min(.74F, mouthH * .29F));
        int rocks = detail == 0 ? 168 : detail == 1 ? 96 : 56;
        int trailSteps = detail == 0 ? 4 : detail == 1 ? 3 : 2;
        int streams = detail == 0 ? 28 : detail == 1 ? 20 : 12;
        int streamSteps = detail == 0 ? 12 : detail == 1 ? 9 : 6;

        // Long, curved strands form a loose funnel instead of an opaque vortex disc.
        // Broad, faint ribbons sit beneath a narrow luminous filament along the same path.
        for (int i = 0; i < streams; i++) {
            int key = 1100 + i;
            float head = fract((age - 44) * (.006F + seed(key + 11) * .003F) + seed(key));
            float length = .17F + seed(key + 45) * .15F;
            float previous = Math.max(0, head - length);
            float px = pathX(key, previous), py = mouthY + pathY(key, previous, vertical);
            float pz = pathZ(key, previous);
            float envelope = opacity * smooth(head * 12) * smooth((1 - head) * 12);
            for (int j = 1; j <= streamSteps; j++) {
                float t = j / (float) streamSteps;
                float p = Math.max(0, head - length * (1 - t));
                float x = pathX(key, p), y = mouthY + pathY(key, p, vertical), z = pathZ(key, p);
                float alpha = envelope * (float) Math.sin(t * PI) * (.24F + .32F * p);
                float width = .014F + .017F * (1 - p);
                line(glow, px, py, pz, x, y, z, width * 3.6F, 0xA8B7E8, alpha * .11F);
                line(glow, px, py, pz + .002F, x, y, z + .002F, width, 0xE4EBFF, alpha);
                px = x; py = y; pz = z;
            }
        }

        // Uneven rock shards tumble on independent paths. Their inward speed rises as
        // radius contracts, and both scale and opacity disappear before reaching the throat.
        for (int i = 0; i < rocks; i++) {
            int key = i + 231;
            float phase = fract((age - 52) * (.0036F + seed(key + 91) * .0035F) + seed(key));
            float remain = 1 - phase;
            float alpha = opacity * smooth(phase * 10) * smooth(remain * 11);
            if (alpha <= .001F) continue;
            float x = pathX(key, phase), y = mouthY + pathY(key, phase, vertical);
            float z = pathZ(key, phase);
            float size = (.10F + seed(key + 38) * .27F) * (.12F + .88F * remain);
            if (i % 11 == 0) size *= 1.55F;
            rock(body, x, y, z, size, age * (.018F + seed(key + 7) * .024F), key, alpha);
            if ((i & 1) == 0) {
                float px = x, py = y, pz = z;
                for (int j = 1; j <= trailSteps; j++) {
                    float p = Math.max(0, phase - j * (.012F + phase * .008F));
                    float xx = pathX(key, p), yy = mouthY + pathY(key, p, vertical), zz = pathZ(key, p);
                    float a = alpha * (1 - j / (float) (trailSteps + 1)) * (.13F + .18F * phase);
                    line(glow, px, py, pz, xx, yy, zz, .008F + size * .035F, 0xC5D5FA, a);
                    px = xx; py = yy; pz = zz;
                }
            }
            if (i % 3 == 0) {
                // Small grazing glints suggest polished edges without outlining every face.
                float glint = alpha * (.35F + .3F * (float) Math.sin(age * .11F + i));
                line(glow, x - size * .65F, y + size * .37F, z + size * .4F,
                        x + size * .18F, y + size * .64F, z + size * .2F, .009F, 0xEDF3FF, glint);
                line(glow, x + size * .18F, y + size * .64F, z + size * .2F,
                        x + size * .58F, y + size * .12F, z + size * .4F, .006F, 0xBBC9E8, glint * .55F);
            }
        }

        // Broken, rotating gravity shear close to the maw adds depth without a full ring.
        int arcs = detail == 0 ? 8 : detail == 1 ? 6 : 4;
        int arcSteps = detail == 0 ? 16 : detail == 1 ? 12 : 8;
        for (int i = 0; i < arcs; i++) {
            float cycle = fract(age * (.005F + i * .0003F) + seed(i + 403));
            float radius = .32F + (1 - cycle) * 4.1F;
            float start = i * 2.399963F + age * (.021F + i * .0015F);
            float sweep = .68F + seed(i + 409) * .75F;
            float alpha = opacity * smooth(cycle * 8) * smooth((1 - cycle) * 8) * .19F;
            for (int j = 0; j < arcSteps; j++) {
                float t = j / (float) arcSteps, tt = (j + 1) / (float) arcSteps;
                float a = start + sweep * t, b = start + sweep * tt;
                float r = radius * (1 - .16F * t), rr = radius * (1 - .16F * tt);
                float z = 1.36F + (1 - cycle) * 1.8F;
                float width = .012F + .012F * (float) Math.sin(t * PI);
                line(glow, cos(a) * r, mouthY + sin(a) * r * vertical, z,
                        cos(b) * rr, mouthY + sin(b) * rr * vertical, z - .025F,
                        width, 0xCDD8FB, alpha * (float) Math.sin((t + tt) * .5F * PI));
            }
        }

        // Fine accelerated dust fills the gaps between larger, slower pieces of rubble.
        int dust = detail == 0 ? 112 : detail == 1 ? 72 : 40;
        for (int i = 0; i < dust; i++) {
            int key = 2200 + i;
            float phase = fract((age - 50) * (.009F + seed(key + 1) * .008F) + seed(key));
            float tail = Math.max(0, phase - .012F - phase * .025F);
            float alpha = opacity * smooth(phase * 14) * smooth((1 - phase) * 14) * .43F;
            line(glow, pathX(key, tail), mouthY + pathY(key, tail, vertical), pathZ(key, tail),
                    pathX(key, phase), mouthY + pathY(key, phase, vertical), pathZ(key, phase),
                    .007F + seed(key + 61) * .012F, i % 4 == 0 ? 0xB7B7E5 : 0xE5EDFF, alpha);
        }
    }

    private static float pathRadius(int key, float p) {
        return .08F + (5.8F + seed(key + 5) * 8.2F) * (1 - p * p);
    }
    private static float pathAngle(int key, float p) {
        return seed(key + 29) * TAU + p * (1.5F + seed(key + 31) * 1.05F)
                + sin(p * PI) * .22F;
    }
    private static float pathX(int key, float p) {
        return cos(pathAngle(key, p)) * pathRadius(key, p);
    }
    private static float pathY(int key, float p, float vertical) {
        return sin(pathAngle(key, p)) * pathRadius(key, p) * vertical;
    }
    private static float pathZ(int key, float p) {
        float remain = 1 - p;
        return 1.25F + remain * remain * (2.3F + seed(key + 18) * 3.0F);
    }

    private static void rock(HyperGonerMesh.Sink out, float x, float y, float z,
                             float size, float spin, int key, float alpha) {
        float yaw = spin + seed(key + 13) * TAU, pitch = spin * 1.31F + seed(key + 22) * TAU;
        float roll = spin * .73F + seed(key + 41) * TAU;
        float cy = cos(yaw), sy = sin(yaw), cp = cos(pitch), sp = sin(pitch), cr = cos(roll), sr = sin(roll);
        float m00 = cy * cr + sy * sp * sr, m01 = -cy * sr + sy * sp * cr, m02 = sy * cp;
        float m10 = cp * sr, m11 = cp * cr, m12 = -sp;
        float m20 = -sy * cr + cy * sp * sr, m21 = sy * sr + cy * sp * cr, m22 = cy * cp;
        float tall = .6F + seed(key + 2) * .9F, wide = .65F + seed(key + 3) * .55F;
        for (int side = 0; side < 2; side++) for (int i = 0; i < 4; i++) {
            int next = (i + 1) & 3;
            float r1 = .7F + seed(key * 7 + i) * .45F, r2 = .7F + seed(key * 7 + next) * .45F;
            float u = CORNER_X[i] * r1 * wide, w = CORNER_Z[i] * r1;
            float uu = CORNER_X[next] * r2 * wide, ww = CORNER_Z[next] * r2;
            float tipU = side == 0 ? .18F : -.13F, tipV = (side == 0 ? 1 : -.75F) * tall;
            float tipW = side == 0 ? -.08F : .12F;
            int rgb = ROCK_COLORS[(i + side * 4 + key) & 7];
            transformed(out, x, y, z, size, u, .08F, w, m00,m01,m02,m10,m11,m12,m20,m21,m22,rgb,alpha);
            transformed(out, x, y, z, size, uu, .08F, ww, m00,m01,m02,m10,m11,m12,m20,m21,m22,rgb,alpha);
            transformed(out, x, y, z, size, tipU, tipV, tipW, m00,m01,m02,m10,m11,m12,m20,m21,m22,rgb,alpha);
            transformed(out, x, y, z, size, tipU, tipV, tipW, m00,m01,m02,m10,m11,m12,m20,m21,m22,rgb,alpha);
        }
    }
    private static void transformed(HyperGonerMesh.Sink out, float x, float y, float z, float s,
                                    float u, float v, float w, float a,float b,float c,float d,float e,float f,
                                    float g,float h,float i,int rgb,float alpha) {
        out.vertex(x + s * (a*u+b*v+c*w), y + s * (d*u+e*v+f*w), z + s * (g*u+h*v+i*w), rgb, alpha);
    }
    private static void line(HyperGonerMesh.Sink out, float x, float y, float z,
                             float xx, float yy, float zz, float width, int color, float alpha) {
        if (alpha <= .0001F) return;
        float dx = xx - x, dy = yy - y, length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < .00001F) return;
        float nx = -dy / length * width, ny = dx / length * width;
        out.vertex(x - nx, y - ny, z, color, alpha);
        out.vertex(xx - nx, yy - ny, zz, color, alpha);
        out.vertex(xx + nx, yy + ny, zz, color, alpha);
        out.vertex(x + nx, y + ny, z, color, alpha);
    }
    private static float seed(int value) {
        int x = value * 0x45d9f3b;
        x = (x ^ (x >>> 16)) * 0x45d9f3b;
        x ^= x >>> 16;
        return (x & 0xFFFFFF) / (float) 0x1000000;
    }
    private static float smooth(float t) { t = clamp(t); return t * t * (3 - 2 * t); }
    private static float clamp(float t) { return Math.max(0, Math.min(1, t)); }
    private static float fract(float value) { return value - (float) Math.floor(value); }
    private static float sin(float value) { return (float) Math.sin(value); }
    private static float cos(float value) { return (float) Math.cos(value); }
}
