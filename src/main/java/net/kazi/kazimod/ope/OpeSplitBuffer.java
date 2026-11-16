package net.kazi.kazimod.ope;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.*;

/** Clips model quads at the waist instead of stretching cubes across the separation. */
public final class OpeSplitBuffer implements IRenderTypeBuffer {
    private final IRenderTypeBuffer parent;
    private final Matrix4f inverse;
    private final Vector4f upper, lower;
    private final float height;
    private final boolean valid;

    public OpeSplitBuffer(IRenderTypeBuffer parent, Matrix4f root, float height, float yaw, float amount, float width) {
        this.parent = parent; this.height = height;
        inverse = root.copy();
        // 1.16's invert() multiplies by the determinant, so normalize the adjugate explicitly.
        float determinant = inverse.adjugateAndDet();
        valid = Float.isFinite(determinant) && Math.abs(determinant) > 1.0E-6F;
        if (valid) inverse.multiply(1 / determinant);
        float distance = Math.min(.35F, width * .28F) * amount;
        float angle = yaw * (float)Math.PI / 180;
        upper = new Vector4f((float)Math.cos(angle) * distance, .28F * amount,
                (float)Math.sin(angle) * distance, 0);
        lower = new Vector4f(-upper.x() * .35F, 0, -upper.z() * .35F, 0);
        upper.transform(root); lower.transform(root);
    }
    public IRenderTypeBuffer original() { return parent; }
    @Override public IVertexBuilder getBuffer(RenderType type) {
        IVertexBuilder out = parent.getBuffer(type);
        if (!valid || type.mode() != 7 || (type.format() != DefaultVertexFormats.NEW_ENTITY && type.format() != DefaultVertexFormats.BLOCK)) return out;
        return new Cut(out);
    }

    private final class Cut implements IVertexBuilder {
        private final IVertexBuilder out;
        private final float[][] quad = new float[4][17], clipped = new float[6][17];
        private int count;
        Cut(IVertexBuilder out) { this.out = out; }
        private float[] v() { return quad[count]; }
        @Override public IVertexBuilder vertex(double x, double y, double z) {
            float[] v = v(); v[0] = (float)x; v[1] = (float)y; v[2] = (float)z; return this;
        }
        @Override public IVertexBuilder color(int r, int g, int b, int a) {
            float[] v = v(); v[3] = r; v[4] = g; v[5] = b; v[6] = a; return this;
        }
        @Override public IVertexBuilder uv(float u, float v) { v()[7] = u; v()[8] = v; return this; }
        @Override public IVertexBuilder overlayCoords(int u, int v) { v()[9] = u; v()[10] = v; return this; }
        @Override public IVertexBuilder uv2(int u, int v) { v()[11] = u; v()[12] = v; return this; }
        @Override public IVertexBuilder normal(float x, float y, float z) { v()[13] = x; v()[14] = y; v()[15] = z; return this; }
        @Override public void endVertex() {
            float[] v = v(); Vector4f point = new Vector4f(v[0], v[1], v[2], 1); point.transform(inverse);
            v[16] = point.y() / point.w() - height;
            if (++count == 4) { clip(true); clip(false); count = 0; }
        }
        private void clip(boolean top) {
            int n = 0;
            for (int i = 0; i < 4; i++) {
                float[] a = quad[i], b = quad[(i + 1) & 3];
                boolean insideA = top ? a[16] >= 0 : a[16] < 0;
                boolean insideB = top ? b[16] >= 0 : b[16] < 0;
                if (insideA) System.arraycopy(a, 0, clipped[n++], 0, 17);
                if (insideA != insideB) {
                    float t = a[16] / (a[16] - b[16]);
                    float[] at = clipped[n++];
                    for (int j = 0; j < 17; j++) at[j] = a[j] + (b[j] - a[j]) * t;
                }
            }
            if (n < 3) return;
            Vector4f offset = top ? upper : lower;
            if (n == 4) { for (int i = 0; i < 4; i++) emit(clipped[i], offset); }
            else for (int i = 1; i < n - 1; i++) {
                emit(clipped[0], offset); emit(clipped[i], offset);
                emit(clipped[i + 1], offset); emit(clipped[i + 1], offset);
            }
        }
        private void emit(float[] v, Vector4f offset) {
            out.vertex(v[0] + offset.x(), v[1] + offset.y(), v[2] + offset.z())
                    .color(Math.round(v[3]), Math.round(v[4]), Math.round(v[5]), Math.round(v[6]))
                    .uv(v[7], v[8]).overlayCoords(Math.round(v[9]), Math.round(v[10]))
                    .uv2(Math.round(v[11]), Math.round(v[12])).normal(v[13], v[14], v[15]).endVertex();
        }
    }
}
