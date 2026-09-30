package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Small untextured meshes shared by Caladbolg's two client renderers. */
final class CaladbolgVisualGeometry {
    // Additive color, two-sided geometry, depth-tested but no depth writes: overlapping
    // plumes stay translucent and do not cut holes into one another or the world.
    private static final class States extends RenderState {
        private States() { super(null, null, null); }
        static final TransparencyState GLOW = LIGHTNING_TRANSPARENCY;
        static final CullState CULL = NO_CULL;
        static final WriteMaskState WRITE = COLOR_WRITE;
        static final DepthTestState DEPTH = LEQUAL_DEPTH_TEST;
        static final ShadeModelState SHADE = SMOOTH_SHADE;
    }

    static final RenderType ENERGY = RenderType.create("kazimod_caladbolg_energy",
            DefaultVertexFormats.POSITION_COLOR, 7, 16384,
            RenderType.State.builder().setTransparencyState(States.GLOW)
                    .setCullState(States.CULL).setWriteMaskState(States.WRITE)
                    .setDepthTestState(States.DEPTH).setShadeModelState(States.SHADE)
                    .createCompositeState(false));

    private CaladbolgVisualGeometry() { }

    static void vertex(IVertexBuilder out, Matrix4f pose, Vector3d point,
                       float red, float green, float blue, float alpha) {
        out.vertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }

    // Crossed ribbons make each bolt visible from every camera angle, including F5.
    static void beam(IVertexBuilder out, Matrix4f pose, Vector3d from, Vector3d to,
                     float width, float red, float green, float blue, float alpha) {
        if (alpha <= 0.001F || width <= 0) return;
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.00001D) return;
        dx /= length; dy /= length; dz /= length;
        boolean vertical = Math.abs(dy) > 0.9D;
        double sx = vertical ? 0 : -dz, sy = vertical ? dz : 0, sz = vertical ? -dy : dx;
        double sideLength = Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx = sx / sideLength * width; sy = sy / sideLength * width; sz = sz / sideLength * width;
        ribbon(out, pose, from, to, sx, sy, sz, red, green, blue, alpha);
        ribbon(out, pose, from, to, dy * sz - dz * sy, dz * sx - dx * sz,
                dx * sy - dy * sx, red, green, blue, alpha);
    }

    private static void ribbon(IVertexBuilder out, Matrix4f pose, Vector3d from, Vector3d to,
                               double sx, double sy, double sz, float red, float green, float blue, float alpha) {
        vertex(out, pose, from.x + sx, from.y + sy, from.z + sz, red, green, blue, alpha);
        vertex(out, pose, from.x - sx, from.y - sy, from.z - sz, red, green, blue, alpha);
        vertex(out, pose, to.x - sx, to.y - sy, to.z - sz, red, green, blue, alpha * 0.7F);
        vertex(out, pose, to.x + sx, to.y + sy, to.z + sz, red, green, blue, alpha * 0.7F);
    }

    static void ring(IVertexBuilder out, Matrix4f pose, float radius, float height,
                     float width, float time, float red, float green, float blue, float alpha) {
        ring(out, pose, radius, height, width, time, red, green, blue, alpha, 64);
    }

    static void ring(IVertexBuilder out, Matrix4f pose, float radius, float height,
                     float width, float time, float red, float green, float blue, float alpha, int segments) {
        if (alpha <= 0.001F || radius <= 0) return;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0D / segments;
            double b = (i + 1) * Math.PI * 2.0D / segments;
            float ra = radius * (1.0F + 0.025F * (float) Math.sin(a * 9.0D + time));
            float rb = radius * (1.0F + 0.025F * (float) Math.sin(b * 9.0D + time));
            double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
            vertex(out, pose, ca * (ra - width), height, sa * (ra - width), red, green, blue, alpha);
            vertex(out, pose, cb * (rb - width), height, sb * (rb - width), red, green, blue, alpha);
            vertex(out, pose, cb * (rb + width), height, sb * (rb + width), red, green, blue, 0);
            vertex(out, pose, ca * (ra + width), height, sa * (ra + width), red, green, blue, 0);
        }
    }

    static Vector3d polar(double angle, double radius, double height) {
        return new Vector3d(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    static void vertex(IVertexBuilder out, Matrix4f pose, double x, double y, double z,
                        float red, float green, float blue, float alpha) {
        out.vertex(pose, (float) x, (float) y, (float) z).color(red, green, blue, alpha).endVertex();
    }
}
