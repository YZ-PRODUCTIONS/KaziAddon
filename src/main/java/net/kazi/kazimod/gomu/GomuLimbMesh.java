package net.kazi.kazimod.gomu;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.vector.Vector3d;

/** A continuous, tapered skin-mapped arm, including the hand at its leading end. */
public final class GomuLimbMesh {
    private GomuLimbMesh() {}

    public static void draw(MatrixStack stack, IVertexBuilder buffer, Vector3d root, Vector3d tip,
                            Vector3d right, double baseWidth, double fistWidth, boolean left,
                            boolean slim, boolean sleeve, int light, float red, float green, float blue, float alpha) {
        Vector3d axis = tip.subtract(root);
        double length = axis.length();
        if (length < .02 || !Double.isFinite(length)) return;
        Vector3d forward = axis.scale(1 / length);
        Vector3d across = right.subtract(forward.scale(right.dot(forward))).normalize();
        if (across.lengthSqr() < .01) across = new Vector3d(0, 0, 1).cross(forward).normalize();
        Vector3d up = forward.cross(across).normalize();
        double inflate = sleeve ? 1.035 : 1;
        double hand = Math.min(length * .35, fistWidth * .55);
        double[] distances = {0, Math.min(length * .2, .45), Math.max(length * .2, length - hand * 2), length - hand, length, length + hand};
        double[] widths = {baseWidth, baseWidth, Math.max(baseWidth, Math.min(fistWidth * .45, baseWidth * 2)), fistWidth, fistWidth, fistWidth};
        float[] skinY = {0, 2, 7, 9, 10.5F, 12};
        float u = left ? 32 : 40, v = left ? 48 : 16;
        if (sleeve) { u = left ? 48 : 40; v = left ? 48 : 32; }
        float armPixels = slim ? 3 : 4;
        for (int i = 0; i < distances.length - 1; i++) {
            Vector3d a = root.add(forward.scale(distances[i])), b = root.add(forward.scale(distances[i + 1]));
            double w0 = widths[i] * inflate * .5, w1 = widths[i + 1] * inflate * .5;
            Vector3d[] ar = ring(a, across, up, w0 * (slim ? .75 : 1), w0);
            Vector3d[] br = ring(b, across, up, w1 * (slim ? .75 : 1), w1);
            float[] us = {u, u + 4, u + 4 + armPixels, u + 8 + armPixels, u + 8 + 2 * armPixels};
            for (int face = 0; face < 4; face++) {
                int next = (face + 1) & 3;
                quad(stack, buffer, ar[face], ar[next], br[next], br[face], us[face], v + 4 + skinY[i],
                        us[face + 1], v + 4 + skinY[i + 1], light, red, green, blue, alpha);
            }
            if (i == distances.length - 2) quad(stack, buffer, br[0], br[1], br[2], br[3],
                    u + 4 + armPixels, v, u + 4 + 2 * armPixels, v + 4, light, red, green, blue, alpha);
        }
    }

    private static Vector3d[] ring(Vector3d c, Vector3d right, Vector3d up, double x, double y) {
        return new Vector3d[]{c.add(right.scale(-x)).add(up.scale(y)), c.add(right.scale(x)).add(up.scale(y)),
                c.add(right.scale(x)).add(up.scale(-y)), c.add(right.scale(-x)).add(up.scale(-y))};
    }

    private static void quad(MatrixStack stack, IVertexBuilder buffer, Vector3d a, Vector3d b, Vector3d c, Vector3d d,
                             float u0, float v0, float u1, float v1, int light, float red, float green, float blue, float alpha) {
        Vector3d n = c.subtract(a).cross(b.subtract(a)).normalize();
        vertex(stack, buffer, a, n, u0, v0, light, red, green, blue, alpha);
        vertex(stack, buffer, b, n, u1, v0, light, red, green, blue, alpha);
        vertex(stack, buffer, c, n, u1, v1, light, red, green, blue, alpha);
        vertex(stack, buffer, d, n, u0, v1, light, red, green, blue, alpha);
    }

    private static void vertex(MatrixStack s, IVertexBuilder b, Vector3d p, Vector3d n, float u, float v,
                               int light, float r, float g, float blue, float a) {
        b.vertex(s.last().pose(), (float)p.x, (float)p.y, (float)p.z).color(r, g, blue, a).uv(u / 64, v / 64)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(s.last().normal(), (float)n.x, (float)n.y, (float)n.z).endVertex();
    }
}
