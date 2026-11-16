package net.kazi.kazimod.petal;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/** Curved, full-bright energy surfaces; no spawned particle entities or full-screen flash. */
public final class HakuteikenVisual {
    public static float smooth(float value) {
        float t = MathHelper.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void vertex(IVertexBuilder v, Matrix4f m, Vector3d p, float alpha, boolean core) {
        v.vertex(m, (float) p.x, (float) p.y, (float) p.z)
            .color(1, 1, 1, alpha).endVertex();
    }

    private static void strip(IVertexBuilder v, Matrix4f m, Vector3d a, Vector3d b,
                              Vector3d aw, Vector3d bw, float alpha, boolean core) {
        if (alpha <= 0) return;
        if (core) {
            vertex(v, m, a.subtract(aw), alpha, true);
            vertex(v, m, b.subtract(bw), alpha, true);
            vertex(v, m, b.add(bw), alpha, true);
            vertex(v, m, a.add(aw), alpha, true);
        } else {
            for (int side : new int[]{-1, 1}) {
                vertex(v, m, a, alpha, false);
                vertex(v, m, b, alpha, false);
                vertex(v, m, b.add(bw.scale(side)), 0, false);
                vertex(v, m, a.add(aw.scale(side)), 0, false);
            }
        }
    }

    private static Vector3d bezier(Vector3d a, Vector3d control, Vector3d b, double t) {
        return a.scale((1 - t) * (1 - t)).add(control.scale(2 * t * (1 - t))).add(b.scale(t * t));
    }

    private static Vector3d featherPoint(int feather, double t, float age, float release) {
        double f = feather / 23D;
        Vector3d root = new Vector3d(.23 + .8 * f, 1.12 + 2.65 * f, .48);
        Vector3d control = new Vector3d(2.0 + 1.3 * f, 1.05 + 3.3 * f, .65 + f * .15);
        Vector3d tip = new Vector3d(4.0 + .75 * Math.sin(f * Math.PI) + .8 * f,
            1.24 + 4.65 * f + (feather % 3 == 0 ? .14 : 0), .42 + .35 * f);
        Vector3d p = bezier(root, control, tip, t);
        double ripple = Math.sin(age * .085 - t * 3.4 + f * 2.2) * .075 * t * t;
        return p.add(0, ripple, ripple * 1.4 + release * t * t * 2.4);
    }

    private static void wing(MatrixStack stack, IRenderTypeBuffer buffers, float age,
                             float alpha, int side, boolean glow) {
        float unfurl = smooth((age - 22) / 38);
        float release = smooth((age - HakuteikenTiming.CHARGE) / 8);
        if (unfurl <= 0 || alpha <= 0) return;
        stack.pushPose();
        stack.scale(side, 1, 1);
        stack.translate(.23, 1.12, .48);
        stack.mulPose(Vector3f.YP.rotationDegrees(-release * 34));
        stack.scale(.2F + .8F * unfurl, .48F + .52F * unfurl, 1);
        stack.translate(-.23, -1.12, -.48);
        IVertexBuilder v = buffers.getBuffer(PetalMesh.TYPE);
        Matrix4f m = stack.last().pose();
        // Overlapping broad feathers make one luminous silhouette, not a fan of thin rods.
        for (int feather = 23; feather >= 0; feather--) {
            float appear = smooth((age - 21 - feather * .35F) / 18);
            for (int j = 0; j < 12; j++) {
                double t = j / 12D, next = (j + 1) / 12D;
                Vector3d a = featherPoint(feather, t, age, release);
                Vector3d b = featherPoint(feather, next, age, release);
                Vector3d normal = new Vector3d(-(b.y - a.y), b.x - a.x, 0).normalize();
                double width = (.18 + .23 * Math.sin(t * Math.PI)) * Math.pow(1 - t, .45);
                double nextWidth = (.18 + .23 * Math.sin(next * Math.PI)) * Math.pow(1 - next, .45);
                strip(v, m, a, b, normal.scale(width), normal.scale(nextWidth), alpha * appear, true);
                if (glow) strip(v, m, a, b, normal.scale(width + .24), normal.scale(nextWidth + .24 * (1 - next)), alpha * appear * .25F, false);
            }
        }
        stack.popPose();
    }

    private static void halo(IVertexBuilder v, Matrix4f m, float age, float alpha) {
        float reveal = smooth((age - 46) / 20), release = smooth((age - HakuteikenTiming.CHARGE) / 10);
        double radius = 1.08 + (1 - reveal) * .38 + release * .18;
        for (int i = 0; i < 96; i++) {
            double a = i * Math.PI / 48, b = (i + 1) * Math.PI / 48;
            Vector3d p = new Vector3d(Math.cos(a) * radius, 2.95 + Math.sin(a) * radius, .62);
            Vector3d q = new Vector3d(Math.cos(b) * radius, 2.95 + Math.sin(b) * radius, .62);
            Vector3d u = new Vector3d(Math.cos(a), Math.sin(a), 0), w = new Vector3d(Math.cos(b), Math.sin(b), 0);
            strip(v, m, p, q, u.scale(.045), w.scale(.045), alpha * reveal, true);
            strip(v, m, p, q, u.scale(.22), w.scale(.22), alpha * reveal * .35F, false);
        }
    }

    private static void gathering(IVertexBuilder v, Matrix4f m, float age, float alpha, int quality) {
        float fade = (1 - smooth((age - 34) / 18)) * smooth(age / 8);
        if (fade <= 0) return;
        int count = Math.min(220, quality);
        for (int i = 0; i < count; i++) {
            double t = (i / (double) count + age * .023) % 1;
            double a = i * 2.399 + age * .035 + t * 2.2;
            double radius = 5.5 * (1 - t) * (1 - t);
            PetalMesh.petal(v, m, Math.cos(a) * radius, .9 + (1 - t) * (1 + (i % 11) * .27),
                Math.sin(a) * radius + .4, .045 + .04 * (1 - t), a, alpha * fade * (float) Math.sin(t * Math.PI), t > .6);
        }
    }

    private static void eruption(IVertexBuilder v, Matrix4f m, float age, float alpha) {
        float rise = smooth((age - 13) / 12), fade = 1 - smooth((age - 37) / 19);
        if (rise * fade <= 0) return;
        for (int i = 0; i < 18; i++) {
            double a = i * Math.PI / 9 + age * .012, r = .65 + .35 * Math.sin(i * 7);
            Vector3d base = new Vector3d(Math.cos(a) * r, .1, .6 + Math.sin(a) * r);
            Vector3d top = base.add(Math.cos(a) * .6, rise * (4 + 1.6 * Math.sin(i * 2.3)), Math.sin(a) * .6);
            Vector3d width = new Vector3d(Math.cos(a) * .34, 0, Math.sin(a) * .34);
            strip(v, m, base, top, width, Vector3d.ZERO, alpha * fade * .68F, true);
            strip(v, m, base, top, width.scale(2.2), Vector3d.ZERO, alpha * fade * .3F, false);
        }
        for (int band = 0; band < 3; band++) for (int i = 0; i < 64; i++) {
            double a = i * Math.PI / 32, b = (i + 1) * Math.PI / 32;
            double height = .45 + band * .65 + rise * .8, radius = 1.25 + band * .25;
            Vector3d p = new Vector3d(Math.cos(a) * radius, height + Math.sin(a) * .35, Math.sin(a) * radius);
            Vector3d q = new Vector3d(Math.cos(b) * radius, height + Math.sin(b) * .35, Math.sin(b) * radius);
            strip(v, m, p, q, new Vector3d(0, .09, 0), new Vector3d(0, .09, 0), alpha * fade * .6F, false);
        }
    }

    public static void render(MatrixStack stack, IRenderTypeBuffer buffers, float age, float alpha, int quality, boolean firstPerson) {
        float fade = 1 - smooth((age - HakuteikenTiming.RUSH_END) / 16);
        if (fade <= 0) return;
        alpha *= fade;
        IVertexBuilder v = buffers.getBuffer(PetalMesh.TYPE);
        Matrix4f m = stack.last().pose();
        gathering(v, m, age, alpha, quality);
        if (!firstPerson) {
            eruption(v, m, age, alpha);
            wing(stack, buffers, age, alpha, -1, quality > 100);
            wing(stack, buffers, age, alpha, 1, quality > 100);
            halo(v, m, age, alpha);
        }
        float formed = smooth((age - 30) / 24);
        for (int side : new int[]{-1, 1}) for (int j = 0; j < 40; j++) {
            double t = j / 40D, next = (j + 1) / 40D;
            Vector3d a = mantle(t, age, side), b = mantle(next, age, side);
            double width = Math.sin(t * Math.PI) * .17 + .025;
            strip(v, m, a, b, new Vector3d(0, width, 0), new Vector3d(0, width, 0), alpha * formed * .82F, true);
            strip(v, m, a, b, new Vector3d(0, width * 3, 0), new Vector3d(0, width * 3, 0), alpha * formed * .3F, false);
        }
        if (age >= HakuteikenTiming.CHARGE) {
            float rush = smooth((age - HakuteikenTiming.CHARGE) / 3);
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8, r = .6 + (i % 3) * .22;
                Vector3d start = new Vector3d(Math.cos(a) * r, 1.2 + Math.sin(a) * r, .5);
                Vector3d end = start.add(Math.cos(a) * .9, Math.sin(a) * .9, 5 + (i % 4) * .8);
                strip(v, m, start, end, new Vector3d(.08, .08, 0), Vector3d.ZERO, alpha * rush * .5F, false);
            }
        }
    }

    private static Vector3d mantle(double t, float age, int side) {
        return new Vector3d(side * (.15 + t * 4.6), 1.05 + .25 * Math.sin(t * Math.PI) + Math.sin(t * 13 - age * .11) * .045 * t,
            -.33 + t * .45);
    }

    /** Rendered in the existing hand attachment's model-unit coordinates. */
    public static void sword(MatrixStack stack, IRenderTypeBuffer buffers, float age, float alpha) {
        float formed = smooth((age - 16) / 30);
        if (formed <= 0) return;
        IVertexBuilder v = buffers.getBuffer(PetalMesh.TYPE);
        Matrix4f m = stack.last().pose();
        Vector3d base = new Vector3d(0, -7, 0), tip = new Vector3d(0, 32, 0);
        strip(v, m, base, tip, new Vector3d(.8, 0, 0), Vector3d.ZERO, alpha * formed, true);
        strip(v, m, base, tip, new Vector3d(0, 0, .5), Vector3d.ZERO, alpha * formed, true);
        strip(v, m, base, tip, new Vector3d(3.2, 0, 0), Vector3d.ZERO, alpha * formed * .5F, false);
        strip(v, m, base, tip, new Vector3d(0, 0, 3.2), Vector3d.ZERO, alpha * formed * .5F, false);
        for (int side : new int[]{-1, 1}) for (int i = 0; i < 5; i++) {
            Vector3d root = new Vector3d(0, 1.2, 0), end = new Vector3d(side * (7.5 - i), 3.5 - i * .65, 0);
            strip(v, m, root, end, new Vector3d(0, 1.25, .35), Vector3d.ZERO, alpha * formed, true);
            strip(v, m, root, end, new Vector3d(0, 2.4, .7), Vector3d.ZERO, alpha * formed * .4F, false);
        }
    }

    private HakuteikenVisual() {}
}
