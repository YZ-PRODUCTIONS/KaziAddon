package net.kazi.kazimod.tusk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.*;

/** Bounded, depth-tested meshes. Effects never need a particle swarm. */
final class TuskMesh {
    static final int GOLD = 0xffdc7e, PINK = 0xe48cce, WHITE = 0xfff3d6;
    private static final class State extends RenderState {
        State() { super(null, null, null); }
        static RenderType type(boolean glow) {
            return RenderType.create(glow ? "tusk_glow" : "tusk_shadow", DefaultVertexFormats.POSITION_COLOR, 7, 65536, false, true,
                    RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                            .setTransparencyState(glow ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setLightmapState(NO_LIGHTMAP)
                            .setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setWriteMaskState(COLOR_WRITE)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
        }
    }
    private static final RenderType GLOW = State.type(true), SHADOW = State.type(false);
    interface Sink { void point(Vector3d p, int color, float alpha); }
    static Sink sink(MatrixStack matrix, IRenderTypeBuffer buffers, boolean glow) {
        IVertexBuilder buffer = buffers.getBuffer(glow ? GLOW : SHADOW); Matrix4f pose = matrix.last().pose();
        return (p, color, alpha) -> buffer.vertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, (int) (Math.max(0, Math.min(1, alpha)) * 255)).endVertex();
    }
    static float smooth(float value) { value = Math.max(0, Math.min(1, value)); return value * value * (3 - 2 * value); }
    static void ring(Sink s, double radius, double width, double y, int color, float alpha) {
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI / 24, b = (i + 1) * Math.PI / 24, inner = Math.max(0, radius - width);
            s.point(new Vector3d(Math.cos(a) * inner, y, Math.sin(a) * inner), color, alpha);
            s.point(new Vector3d(Math.cos(a) * radius, y, Math.sin(a) * radius), color, alpha);
            s.point(new Vector3d(Math.cos(b) * radius, y, Math.sin(b) * radius), color, alpha);
            s.point(new Vector3d(Math.cos(b) * inner, y, Math.sin(b) * inner), color, alpha);
        }
    }
    static void line(Sink s, Vector3d a, Vector3d b, double width, int color, float alpha) {
        Vector3d d = b.subtract(a).normalize(), u = d.cross(new Vector3d(0, 1, 0)).normalize();
        if (u.lengthSqr() < .01) u = new Vector3d(1, 0, 0);
        Vector3d v = d.cross(u).normalize();
        for (int side = 0; side < 4; side++) {
            double x = side * Math.PI / 2, y = (side + 1) * Math.PI / 2;
            Vector3d p = u.scale(Math.cos(x) * width).add(v.scale(Math.sin(x) * width));
            Vector3d q = u.scale(Math.cos(y) * width).add(v.scale(Math.sin(y) * width));
            s.point(a.add(p), color, alpha); s.point(a.add(q), color, alpha); s.point(b.add(q), color, alpha); s.point(b.add(p), color, alpha);
        }
    }
    static void helix(Sink s, double radius, double height, double turns, float time, int color, float alpha) {
        for (int i = 0; i < 64; i++) {
            double a = i / 64D, b = (i + 1) / 64D, angleA = a * Math.PI * 2 * turns + time, angleB = b * Math.PI * 2 * turns + time;
            double rA = radius * (.65 + .35 * Math.sin(a * Math.PI)), rB = radius * (.65 + .35 * Math.sin(b * Math.PI));
            Vector3d p = new Vector3d(Math.cos(angleA) * rA, a * height, Math.sin(angleA) * rA);
            Vector3d q = new Vector3d(Math.cos(angleB) * rB, b * height, Math.sin(angleB) * rB);
            double width = .018 + .018 * Math.sin(a * Math.PI);
            s.point(p.add(0, -width, 0), color, alpha); s.point(p.add(0, width, 0), color, alpha);
            s.point(q.add(0, width, 0), color, alpha); s.point(q.add(0, -width, 0), color, alpha);
        }
    }
    static void portal(MatrixStack stack, IRenderTypeBuffer buffers, float time, float radius, float alpha) {
        Sink dark = sink(stack, buffers, false);
        ring(dark, radius, radius, 0, 0x130b22, .9F * alpha);
        Sink light = sink(stack, buffers, true);
        ring(light, radius * 1.08, radius * .12, .005, PINK, .5F * alpha);
        ring(light, radius, .025, .01, GOLD, alpha);
        for (int arm = 0; arm < 3; arm++) for (int i = 0; i < 36; i++) {
            double t = i / 36D, next = (i + 1) / 36D, a = arm * Math.PI * 2 / 3 + time * .14 + t * Math.PI * 3, b = arm * Math.PI * 2 / 3 + time * .14 + next * Math.PI * 3;
            Vector3d p = new Vector3d(Math.cos(a) * radius * t, .015, Math.sin(a) * radius * t), q = new Vector3d(Math.cos(b) * radius * next, .015, Math.sin(b) * radius * next);
            Vector3d direction = q.subtract(p);
            Vector3d offset = new Vector3d(-direction.z, 0, direction.x).normalize().scale(Math.max(.016, radius * .025));
            light.point(p.add(offset), arm == 1 ? PINK : GOLD, alpha * .65F); light.point(p.subtract(offset), arm == 1 ? PINK : GOLD, alpha * .65F);
            light.point(q.subtract(offset), arm == 1 ? PINK : GOLD, alpha * .65F); light.point(q.add(offset), arm == 1 ? PINK : GOLD, alpha * .65F);
        }
    }
    static void impact(MatrixStack stack, IRenderTypeBuffer buffers, float age, float life, float size) {
        float t = Math.min(1, age / life), alpha = (1 - t) * (1 - t); Sink s = sink(stack, buffers, true);
        ring(s, (.2 + smooth(t) * 1.2) * size, .12 * (1 - t) + .015, .05, GOLD, alpha);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6 + i * .12, start = (.1 + t * .8) * size, end = (.5 + t * 1.5) * size;
            line(s, new Vector3d(Math.cos(a) * start, .15 + Math.sin(i * 4) * t * size * .3, Math.sin(a) * start),
                    new Vector3d(Math.cos(a) * end, .15 + Math.sin(i * 4) * t * size * .5, Math.sin(a) * end), .035 * (1 - t), i % 3 == 0 ? PINK : WHITE, alpha);
        }
    }
    static void nail(MatrixStack stack, IRenderTypeBuffer buffers, int act, float time, boolean infinite) {
        Sink s = sink(stack, buffers, true); int color = infinite || act >= 2 ? GOLD : PINK;
        double size = infinite ? 2.4 : 1;
        line(s, new Vector3d(0, 0, -.45 * size), new Vector3d(0, 0, .22 * size), .065 * size, WHITE, 1);
        for (int strand = 0; strand < 2; strand++) for (int i = 0; i < 28; i++) {
            double t = i / 28D, n = (i + 1) / 28D, a = time * .7 + strand * Math.PI + t * 16, b = time * .7 + strand * Math.PI + n * 16;
            Vector3d p = new Vector3d(Math.cos(a) * .12 * size, Math.sin(a) * .12 * size, -t * 2.3 * size);
            Vector3d q = new Vector3d(Math.cos(b) * .12 * size, Math.sin(b) * .12 * size, -n * 2.3 * size);
            line(s, p, q, .018 * size, color, (float) (1 - t) * .85F);
        }
    }
    private TuskMesh() {}
}
