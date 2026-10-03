package net.kazi.kazimod.models.zoan;

import net.kazi.kazimod.models.abilities.KokuVfxMesh;

/** Fixed-budget, double-sided smoke ribbons in Blockbench model coordinates. */
public final class CerberusCloudMesh {
    private CerberusCloudMesh() {}
    public static void render(KokuVfxMesh.Sink sink, float age, float alpha) {
        for (int side = -1; side <= 1; side += 2) {
            ribbon(sink, side, age, alpha, false);
            ribbon(sink, side, age, alpha, true);
        }
    }
    private static void ribbon(KokuVfxMesh.Sink sink, int side, float age, float alpha, boolean curl) {
        final int count = curl ? 40 : 72;
        float[] previous = point(side, 0, age, curl);
        for (int i = 1; i <= count; i++) {
            float t = i / (float) count;
            float[] next = point(side, t, age, curl);
            float dx = next[0] - previous[0], dy = next[1] - previous[1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = -dy / Math.max(.001F, length), ny = dx / Math.max(.001F, length);
            float w0 = width((i - 1F) / count, age, curl), w1 = width(t, age, curl);
            // Narrow emissive borders outline the dark ribbon on both sides, without additive black.
            band(sink, previous, next, nx, ny, -w0-.22F, -w1-.22F, -w0, -w1, 0x91A7F4, alpha*.9F);
            band(sink, previous, next, nx, ny, w0, w1, w0+.22F, w1+.22F, 0x91A7F4, alpha*.9F);
            band(sink, previous, next, nx, ny, -w0, -w1, w0, w1, 0x12101F, alpha);
            previous = next;
        }
    }
    private static float width(float t, float age, boolean curl) {
        float envelope = curl ? (1-t)*1.8F+.08F : (float)Math.pow(Math.sin(Math.PI*t), .45)*3.7F+.08F;
        return envelope * (1 + .18F*(float)Math.sin(t*74-age*.10F) + .12F*(float)Math.sin(t*137+age*.06F));
    }
    private static float[] point(int side, float t, float age, boolean curl) {
        double a = t * Math.PI * (curl ? 3.6 : 1.32);
        if (curl) {
            float r = 8*(1-t);
            return new float[]{side*(35+r*(float)Math.cos(a)), 43+r*(float)Math.sin(a), 8+(float)Math.sin(age*.035+t*8)};
        }
        return new float[]{side*39*(float)Math.sin(t*Math.PI*.55),
                84-58*t+7*(float)Math.sin(a)+1.1F*(float)Math.sin(age*.045+t*16),
                8+3*(float)Math.sin(t*7+age*.025)};
    }
    private static void band(KokuVfxMesh.Sink sink, float[] a, float[] b, float nx, float ny,
                             float al, float bl, float ar, float br, int color, float alpha) {
        sink.vertex(a[0]+nx*al,a[1]+ny*al,a[2],color,alpha);
        sink.vertex(b[0]+nx*bl,b[1]+ny*bl,b[2],color,alpha);
        sink.vertex(b[0]+nx*br,b[1]+ny*br,b[2],color,alpha);
        sink.vertex(a[0]+nx*ar,a[1]+ny*ar,a[2],color,alpha);
    }
}
