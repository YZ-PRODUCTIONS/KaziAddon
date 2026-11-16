package net.kazi.kazimod.mochi;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.ArrayList;
import java.util.List;
import net.MrMagicalCart.cartaddon.abilities.mochi2.MochiHelper;
import net.MrMagicalCart.cartaddon.abilities.mochi2.ZanGiriMochiAbility;
import net.MrMagicalCart.cartaddon.models.abilities.ZanGiriMochiNewModel;
import net.MrMagicalCart.cartaddon.renderers.layers.morphs.ZanGiriMochiNewLayer;
import net.kazi.kazimod.abilities.MochiRework.ZanGiriMochiClone;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModRenderTypes;

/** Original Zan Giri geometry and UVs, with gently rounded edges. */
public final class ZanGiriMochiVisual {
    private static final Vertex[] ORIGINAL;
    private static final Vertex[] ROUNDED;
    private static final float TWO_PI = (float) (Math.PI * 2);

    static {
        ZanGiriMochiNewModel model = new ZanGiriMochiNewModel();
        model.wholeArm.setPos(0, 0, 0);
        Capture capture = new Capture();
        model.renderToBuffer(new MatrixStack(), capture, 0, 0, 1, 1, 1, 1);
        ORIGINAL = capture.vertices.toArray(new Vertex[0]);
        List<Vertex> rounded = new ArrayList<>();
        for (int start = 0; start < ORIGINAL.length; start += 24) {
            roundCube(rounded, start);
        }
        ROUNDED = rounded.toArray(new Vertex[0]);
    }

    private ZanGiriMochiVisual() {}

    public static void render(LivingEntity entity, MatrixStack stack, IRenderTypeBuffer buffer,
                              float age, float partialTicks, int light) {
        ZanGiriMochiClone clone = AbilityDataCapability.get(entity).getEquippedAbility(ZanGiriMochiClone.INSTANCE);
        ZanGiriMochiAbility cart = AbilityDataCapability.get(entity).getEquippedAbility(ZanGiriMochiAbility.INSTANCE);
        if (clone == null && cart == null) return;
        boolean charging = clone != null ? clone.isCharging() : cart.isCharging();
        float charge = clone != null ? clone.getChargeComponent(entity).getChargeTime()
                : cart.getChargeComponent(entity).getChargeTime();
        boolean raised = clone != null ? clone.isContinuous() : cart.getContinuousComponent(entity).getContinueTime() < 60;
        renderPose(stack, buffer, charging, charge + partialTicks, raised, age, light,
                MochiHelper.hasArmamentOn(entity), !MochiEffects.detailed(entity));
    }

    static float spin(float chargeTicks) {
        float time = Math.max(0, chargeTicks);
        // Integrate both original spin speeds so the 40-tick transition cannot jump.
        return (.5F * Math.min(40, time) + 2 * Math.max(0, time - 40)) % TWO_PI;
    }

    static void renderPose(MatrixStack stack, IRenderTypeBuffer buffer, boolean charging,
                           float chargeTicks, boolean raised, float age, int light, boolean haki, boolean far) {
        stack.pushPose();
        try {
            stack.translate(-1.2 + 15.0 / 16, .2 - 2.0 / 16, 0);
            if (charging) stack.mulPose(Vector3f.YP.rotation(spin(chargeTicks)));
            else stack.mulPose(Vector3f.ZP.rotation((raised ? -300F : 300F) % TWO_PI));
            IVertexBuilder out = buffer.getBuffer(ModRenderTypes.getZoanWithCullingRenderType(
                    haki ? ZanGiriMochiNewLayer.TEXTURE_HAKI : ZanGiriMochiNewLayer.TEXTURE));
            renderGeometry(stack, out, age, light, far);
        } finally {
            stack.popPose();
        }
    }

    static void renderGeometry(MatrixStack stack, IVertexBuilder out, float age, int light, boolean far) {
        Vector4f point = new Vector4f(0, 0, 0, 1);
        Vector3f normal = new Vector3f();
        MatrixStack.Entry pose = stack.last();
        for (Vertex v : far ? ORIGINAL : ROUNDED) {
            double progress = MathHelper.clamp(-v.position.x / 15.25, 0, 1);
            // Fade to zero at the shoulder and club: no detached joints or moving spikes.
            double envelope = Math.sin(progress * Math.PI);
            double ripple = far ? 0 : .045 * envelope * envelope * Math.sin(progress * 8 - age * .18);
            point.set((float) v.position.x, (float) (v.position.y + ripple), (float) v.position.z, 1);
            point.transform(pose.pose());
            normal.set((float) v.normal.x, (float) v.normal.y, (float) v.normal.z);
            normal.transform(pose.normal());
            out.vertex(point.x(), point.y(), point.z(), 1, 1, 1, 1, v.u, v.v,
                    OverlayTexture.NO_OVERLAY, light, normal.x(), normal.y(), normal.z());
        }
    }

    private static void roundCube(List<Vertex> out, int start) {
        Vector3d center = Vector3d.ZERO;
        for (int i = 0; i < 24; i++) center = center.add(ORIGINAL[start + i].position.scale(1.0 / 24));
        Vector3d a = ORIGINAL[start + 1].position.subtract(ORIGINAL[start].position).normalize();
        Vector3d b = ORIGINAL[start + 3].position.subtract(ORIGINAL[start].position).normalize();
        Vector3d[] axes = {a, b, a.cross(b).normalize()};
        double[] half = new double[3];
        for (int i = 0; i < 24; i++) for (int j = 0; j < 3; j++) {
            half[j] = Math.max(half[j], Math.abs(ORIGINAL[start + i].position.subtract(center).dot(axes[j])));
        }
        double smallest = Math.min(half[0], Math.min(half[1], half[2]));
        double radius = Math.min(.1125, smallest * .42);
        for (int face = 0; face < 6; face++) {
            Vertex v0 = ORIGINAL[start + face * 4], v1 = ORIGINAL[start + face * 4 + 1];
            Vertex v2 = ORIGINAL[start + face * 4 + 2], v3 = ORIGINAL[start + face * 4 + 3];
            double[] u = steps(radius, v0.position.distanceTo(v1.position), smallest > .25);
            double[] v = steps(radius, v0.position.distanceTo(v3.position), smallest > .25);
            for (int y = 0; y < v.length - 1; y++) for (int x = 0; x < u.length - 1; x++) {
                out.add(rounded(v0, v1, v2, v3, u[x], v[y], center, axes, half, radius));
                out.add(rounded(v0, v1, v2, v3, u[x + 1], v[y], center, axes, half, radius));
                out.add(rounded(v0, v1, v2, v3, u[x + 1], v[y + 1], center, axes, half, radius));
                out.add(rounded(v0, v1, v2, v3, u[x], v[y + 1], center, axes, half, radius));
            }
        }
    }

    private static double[] steps(double radius, double length, boolean detailed) {
        double edge = Math.min(.24, radius / length);
        if (detailed) return new double[]{0, edge * .5, edge, .5, 1 - edge, 1 - edge * .5, 1};
        return new double[]{0, edge, .5, 1 - edge, 1};
    }

    private static Vertex rounded(Vertex a, Vertex b, Vertex c, Vertex d, double u, double v,
                                  Vector3d center, Vector3d[] axes, double[] half, double radius) {
        Vector3d p = a.position.scale((1 - u) * (1 - v)).add(b.position.scale(u * (1 - v)))
                .add(c.position.scale(u * v)).add(d.position.scale((1 - u) * v));
        Vector3d local = p.subtract(center), core = center;
        for (int i = 0; i < 3; i++) {
            double extent = Math.max(0, half[i] - radius);
            core = core.add(axes[i].scale(MathHelper.clamp(local.dot(axes[i]), -extent, extent)));
        }
        Vector3d normal = p.subtract(core).normalize();
        float texU = (float) (a.u * (1 - u) * (1 - v) + b.u * u * (1 - v) + c.u * u * v + d.u * (1 - u) * v);
        float texV = (float) (a.v * (1 - u) * (1 - v) + b.v * u * (1 - v) + c.v * u * v + d.v * (1 - u) * v);
        return new Vertex(core.add(normal.scale(radius)), normal, texU, texV);
    }

    private static final class Vertex {
        final Vector3d position, normal;
        final float u, v;
        Vertex(Vector3d position, Vector3d normal, float u, float v) {
            this.position = position;
            this.normal = normal;
            this.u = u;
            this.v = v;
        }
    }

    // Capture once through the public model API, preserving every part transform and UV.
    private static final class Capture implements IVertexBuilder {
        final List<Vertex> vertices = new ArrayList<>();
        Vector3d position, normal;
        float u, v;
        public IVertexBuilder vertex(double x, double y, double z) { position = new Vector3d(x, y, z); return this; }
        public IVertexBuilder color(int r, int g, int b, int a) { return this; }
        public IVertexBuilder uv(float u, float v) { this.u = u; this.v = v; return this; }
        public IVertexBuilder overlayCoords(int u, int v) { return this; }
        public IVertexBuilder uv2(int u, int v) { return this; }
        public IVertexBuilder normal(float x, float y, float z) { normal = new Vector3d(x, y, z); return this; }
        public void endVertex() { vertices.add(new Vertex(position, normal, u, v)); }
    }
}
