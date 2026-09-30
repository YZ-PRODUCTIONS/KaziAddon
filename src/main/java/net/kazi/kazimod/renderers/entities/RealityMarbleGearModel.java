package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.ResourceLocation;

/** Client-only, cached clockwork mesh. All dimensions are relative to its centered axle. */
final class RealityMarbleGearModel {
    private static final double TAU = Math.PI * 2.0D;
    private static final int TEETH = 24;
    private static final int SPOKES = 6;
    // Four corners retain the original tooth roots and flat tips without rounded shoulders.
    private static final double[] TOOTH_PROFILE = {0.86, 1.0, 1.0, 0.86};
    private static final double[] TOOTH_ANGLES = {0.125, 0.375, 0.5, 0.75};
    private static final int BODY = 0;
    private static final int BEVEL = 1;
    private static final int RECESS = 2;

    private static final Palette MUGEN = new Palette(
            new float[]{0.73F, 0.74F, 0.69F}, new float[]{1.0F, 0.92F, 0.72F},
            new float[]{0.25F, 0.28F, 0.29F}, new float[]{0.69F, 0.87F, 1.0F});
    private static final Palette INFINITE = new Palette(
            new float[]{0.69F, 0.36F, 0.18F}, new float[]{1.0F, 0.72F, 0.39F},
            new float[]{0.20F, 0.10F, 0.07F}, new float[]{1.0F, 0.48F, 0.16F});

    private static final class States extends RenderState {
        private States() { super(null, null, null); }
        static final TransparencyState SOLID = NO_TRANSPARENCY;
        static final TransparencyState FADE = TRANSLUCENT_TRANSPARENCY;
        static final TransparencyState GLOW = LIGHTNING_TRANSPARENCY;
        static final WriteMaskState DEPTH_WRITE = COLOR_DEPTH_WRITE;
        static final WriteMaskState COLOR_ONLY = COLOR_WRITE;
        static final CullState TWO_SIDED = NO_CULL;
        static final DepthTestState DEPTH = LEQUAL_DEPTH_TEST;
        static final ShadeModelState SHADE = SMOOTH_SHADE;
    }

    static final RenderType METAL = renderType("kazimod_marble_gear_metal", 0);
    static final RenderType FORMING = renderType("kazimod_marble_gear_forming", 1);
    static final RenderType ENERGY = renderType("kazimod_marble_gear_energy", 2);
    static final ResourceLocation LOST_TEXTURE = new ResourceLocation("kazimod",
            "textures/entities/unlimited_lost_works_blackened_metal.png");
    static final RenderType LOST_METAL = lostType("kazimod_lost_gear_metal", true);
    static final RenderType LOST_FORMING = lostType("kazimod_lost_gear_forming", false);
    // Build the metal once, not once per frame or once per gear. No particle entities.
    private static final Vertex[][] MESH = {createMesh(0), createMesh(1), createMesh(2)};

    private RealityMarbleGearModel() { }

    private static RenderType lostType(String name, boolean solid) {
        return RenderType.create(name, DefaultVertexFormats.POSITION_COLOR_TEX, 7, 262144, false, !solid,
                RenderType.State.builder().setTextureState(new RenderState.TextureState(LOST_TEXTURE, false, false))
                        .setTransparencyState(solid ? States.SOLID : States.FADE)
                        .setWriteMaskState(solid ? States.DEPTH_WRITE : States.COLOR_ONLY)
                        .setCullState(States.TWO_SIDED).setDepthTestState(States.DEPTH)
                        .setShadeModelState(States.SHADE).createCompositeState(false));
    }

    /** Same cached gear geometry, with a separate scorched-metal texture and readable bevels. */
    static void renderLostBody(IVertexBuilder out, Matrix4f pose, float alpha, int detail) {
        if (alpha <= 0.001F) return;
        for (Vertex v : MESH[detail]) {
            float brightness = v.shade * (v.material == BEVEL ? 1.0F : v.material == RECESS ? 0.42F : 0.72F);
            out.vertex(pose, v.x, v.y, v.z).color(brightness, brightness, brightness, alpha)
                    .uv(v.x * 0.48F + 0.5F, v.y * 0.48F + 0.5F).endVertex();
        }
    }

    private static RenderType renderType(String name, int pass) {
        return RenderType.create(name, DefaultVertexFormats.POSITION_COLOR, 7, 262144,
                false, pass == 1, RenderType.State.builder()
                        .setTransparencyState(pass == 0 ? States.SOLID : pass == 1 ? States.FADE : States.GLOW)
                        .setWriteMaskState(pass == 0 ? States.DEPTH_WRITE : States.COLOR_ONLY)
                        .setCullState(States.TWO_SIDED).setDepthTestState(States.DEPTH)
                        .setShadeModelState(States.SHADE).createCompositeState(false));
    }

    static void renderBody(IVertexBuilder out, Matrix4f pose, boolean mugen, float alpha, int detail) {
        if (alpha <= 0.0F) return;
        Palette palette = mugen ? MUGEN : INFINITE;
        for (Vertex v : MESH[detail]) {
            float[] color = palette.metal[v.material];
            out.vertex(pose, v.x, v.y, v.z)
                    .color(color[0] * v.shade, color[1] * v.shade, color[2] * v.shade, alpha)
                    .endVertex();
        }
    }

    static void renderEnergy(IVertexBuilder out, Matrix4f pose, boolean mugen,
                             float age, float spinOffset, float alpha, int detail) {
        if (alpha <= 0.0F) return;
        Palette palette = mugen ? MUGEN : INFINITE;
        double phase = Math.toRadians(spinOffset);
        float pulse = alpha * (0.78F + 0.12F * (float) Math.sin(age * 0.07D + phase));
        // Inlays sit just outside the metal on BOTH faces, preventing z-fighting in F5.
        for (int side = -1; side <= 1; side += 2) {
            double z = side * 0.153D;
            arc(out, pose, 0.710D, 0.003D, z, 0, TAU, pulse * 0.75F, palette.glow, detail);
            if (detail == 0) arc(out, pose, 0.710D, 0.014D, z + side * 0.002D, 0, TAU,
                    pulse * 0.12F, palette.glow, detail);
            arc(out, pose, 0.204D, 0.004D, side * 0.195D, 0, TAU, pulse, palette.glow, detail);
            for (int spoke = 0; spoke < SPOKES; spoke++) {
                double angle = spoke * TAU / SPOKES;
                strip(out, pose, point(0.285D, angle, side * 0.108D),
                        point(0.58D, angle, side * 0.108D), 0.004D, palette.glow, pulse * 0.7F);
            }
        }
        // The halo turns backwards relative to the body's 20-second revolution.
        double orbit = phase - age * TAU / 240.0D;
        for (int section = 0; section < 3; section++) {
            double start = orbit + section * TAU / 3.0D;
            arc(out, pose, 1.035D, 0.003D, 0, start, 1.28D, pulse * 0.65F, palette.glow, detail);
            if (detail == 0) arc(out, pose, 1.035D, 0.018D, 0.002D, start, 1.28D, pulse * 0.09F, palette.glow, detail);
            if (detail < 2) arc(out, pose, 1.09D, 0.002D, 0, -start, 0.65D, pulse * 0.3F, palette.glow, detail);
        }
        // A bounded handful of glints drifts off the rim, with a soft life envelope.
        for (int mote = 0; mote < VfxDetail.count(detail, 8, 4, 0); mote++) {
            double life = (age / 64.0D + mote / 12.0D + spinOffset / 360.0D) % 1.0D;
            double angle = mote * 2.399963D + phase - age * 0.018D;
            Vector3d center = point(1.025D + life * 0.18D, angle,
                    Math.sin(mote * 1.7D) * 0.10D);
            float brightness = alpha * (float) Math.pow(Math.sin(life * Math.PI), 2) * 0.55F;
            glint(out, pose, center, 0.007D + 0.009D * (1 - life), palette.glow, brightness);
        }
    }

    private static Vertex[] createMesh(int detail) {
        MeshBuilder mesh = new MeshBuilder();
        annulus(mesh, 0, 0, 0.61D, 1.0D, 0.15D, TEETH * TOOTH_PROFILE.length, true, detail);
        annulus(mesh, 0, 0, 0.105D, 0.255D, 0.19D, ringSegments(detail), false, detail);
        for (int spoke = 0; spoke < SPOKES; spoke++) {
            double angle = spoke * TAU / SPOKES;
            addSpoke(mesh, angle);
            if (detail < 2) annulus(mesh, Math.cos(angle) * 0.77D, Math.sin(angle) * 0.77D,
                    0.007D, 0.022D, 0.164D, 6, false, detail);
        }
        // Recessed channels and regularly spaced engraved marks on both metal faces.
        for (int side = -1; side <= 1; side += 2) {
            flatBand(mesh, 0.699D, 0.721D, side * 0.151D, RECESS, detail);
            flatBand(mesh, 0.814D, 0.824D, side * 0.151D, RECESS, detail);
            flatBand(mesh, 0.191D, 0.217D, side * 0.191D, RECESS, detail);
            int marks = VfxDetail.count(detail, 24, 12, 0);
            for (int mark = 0; mark < marks; mark++) {
                double a = mark * TAU / marks;
                double inner = mark % 4 == 0 ? 0.738D : 0.776D;
                mesh.quad(point(inner, a - 0.004D, side * 0.152D),
                        point(0.802D, a - 0.004D, side * 0.152D),
                        point(0.802D, a + 0.004D, side * 0.152D),
                        point(inner, a + 0.004D, side * 0.152D), RECESS, 1.0F);
            }
        }
        return mesh.vertices.toArray(new Vertex[0]);
    }

    /** Watertight beveled annulus, optionally with a repeating tooth profile. */
    private static void annulus(MeshBuilder mesh, double cx, double cy, double inner,
                                 double outer, double halfDepth, int segments, boolean teeth, int detail) {
        double bevel = detail == 2 ? 0.0D : Math.min(0.014D, (outer - inner) * 0.20D);
        for (int i = 0; i < segments; i++) {
            double a = teeth ? toothAngle(i) : i * TAU / segments;
            double b = teeth ? toothAngle(i + 1) : (i + 1) * TAU / segments;
            double ra = teeth ? TOOTH_PROFILE[i % TOOTH_PROFILE.length] : outer;
            double rb = teeth ? TOOTH_PROFILE[(i + 1) % TOOTH_PROFILE.length] : outer;
            float shade = (float) (0.80D + 0.15D * Math.cos(a - 0.8D));
            for (int side = -1; side <= 1; side += 2) {
                double face = side * halfDepth;
                double shoulder = side * (halfDepth - bevel);
                mesh.quad(at(cx, cy, inner + bevel, a, face), at(cx, cy, ra - bevel, a, face),
                        at(cx, cy, rb - bevel, b, face), at(cx, cy, inner + bevel, b, face), BODY, shade);
                if (detail < 2) {
                    mesh.quad(at(cx, cy, ra - bevel, a, face), at(cx, cy, ra, a, shoulder),
                            at(cx, cy, rb, b, shoulder), at(cx, cy, rb - bevel, b, face), BEVEL, shade);
                    mesh.quad(at(cx, cy, inner, a, shoulder), at(cx, cy, inner + bevel, a, face),
                            at(cx, cy, inner + bevel, b, face), at(cx, cy, inner, b, shoulder), BEVEL, shade);
                }
            }
            double depth = halfDepth - bevel;
            mesh.quad(at(cx, cy, ra, a, -depth), at(cx, cy, ra, a, depth),
                    at(cx, cy, rb, b, depth), at(cx, cy, rb, b, -depth), RECESS, shade);
            mesh.quad(at(cx, cy, inner, a, depth), at(cx, cy, inner, a, -depth),
                    at(cx, cy, inner, b, -depth), at(cx, cy, inner, b, depth), RECESS, shade);
        }
    }

    private static double toothAngle(int corner) {
        int tooth = corner / TOOTH_PROFILE.length;
        return (tooth + TOOTH_ANGLES[corner % TOOTH_PROFILE.length]) * TAU / TEETH;
    }

    // Match the metal channels and their light inlays so coarse edges stay aligned.
    private static int ringSegments(int detail) {
        return VfxDetail.count(detail, 32, 24, 16);
    }

    private static void addSpoke(MeshBuilder mesh, double angle) {
        double[][] outline = {{0.23, -0.034}, {0.30, -0.047}, {0.58, -0.069}, {0.63, -0.050},
                {0.63, 0.050}, {0.58, 0.069}, {0.30, 0.047}, {0.23, 0.034}};
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        for (int i = 0; i < outline.length; i++) {
            double[] a = outline[i];
            double[] b = outline[(i + 1) % outline.length];
            Vector3d edgeA = new Vector3d(a[0] * cosine - a[1] * sine, a[0] * sine + a[1] * cosine, 0);
            Vector3d edgeB = new Vector3d(b[0] * cosine - b[1] * sine, b[0] * sine + b[1] * cosine, 0);
            Vector3d center = point(0.43D, angle, 0);
            Vector3d insetA = center.add(edgeA.subtract(center).scale(0.91D));
            Vector3d insetB = center.add(edgeB.subtract(center).scale(0.91D));
            for (int side = -1; side <= 1; side += 2) {
                Vector3d faceCenter = center.add(0, 0, side * 0.105D);
                mesh.quad(faceCenter, insetA.add(0, 0, side * 0.105D),
                        insetB.add(0, 0, side * 0.105D), faceCenter, BODY, 0.86F);
                mesh.quad(insetA.add(0, 0, side * 0.105D), edgeA.add(0, 0, side * 0.088D),
                        edgeB.add(0, 0, side * 0.088D), insetB.add(0, 0, side * 0.105D), BEVEL, 0.9F);
            }
            mesh.quad(edgeA.add(0, 0, -0.088D), edgeA.add(0, 0, 0.088D),
                    edgeB.add(0, 0, 0.088D), edgeB.add(0, 0, -0.088D), RECESS, 0.85F);
        }
    }

    private static void flatBand(MeshBuilder mesh, double inner, double outer, double z, int material, int detail) {
        int segments = ringSegments(detail);
        for (int i = 0; i < segments; i++) {
            double a = i * TAU / segments;
            double b = (i + 1) * TAU / segments;
            mesh.quad(point(inner, a, z), point(outer, a, z), point(outer, b, z),
                    point(inner, b, z), material, 0.85F);
        }
    }

    /** Feather both the radial edges and the tips of each luminous arc. */
    private static void arc(IVertexBuilder out, Matrix4f pose, double radius, double width,
                             double z, double start, double length, float alpha, float[] color, int detail) {
        if (alpha <= 0.001F) return;
        int segments = Math.max(3, (int) Math.ceil(length / TAU * ringSegments(detail)));
        boolean closed = length >= TAU - 0.001D;
        for (int i = 0; i < segments; i++) {
            double u = i / (double) segments;
            double v = (i + 1) / (double) segments;
            double a = start + u * length;
            double b = start + v * length;
            double ca = Math.cos(a);
            double sa = Math.sin(a);
            double cb = Math.cos(b);
            double sb = Math.sin(b);
            float aa = alpha * (closed ? 1 : (float) Math.sin(u * Math.PI));
            float ab = alpha * (closed ? 1 : (float) Math.sin(v * Math.PI));
            for (int side = -1; side <= 1; side += 2) {
                double edge = radius + side * width;
                emit(out, pose, ca * radius, sa * radius, z, color, aa);
                emit(out, pose, cb * radius, sb * radius, z, color, ab);
                emit(out, pose, cb * edge, sb * edge, z, color, 0);
                emit(out, pose, ca * edge, sa * edge, z, color, 0);
            }
        }
    }

    private static void strip(IVertexBuilder out, Matrix4f pose, Vector3d from, Vector3d to,
                               double width, float[] color, float alpha) {
        Vector3d d = to.subtract(from).normalize();
        Vector3d across = new Vector3d(-d.y * width, d.x * width, 0);
        emit(out, pose, from.add(across), color, alpha);
        emit(out, pose, to.add(across), color, alpha);
        emit(out, pose, to.subtract(across), color, alpha);
        emit(out, pose, from.subtract(across), color, alpha);
    }

    private static void glint(IVertexBuilder out, Matrix4f pose, Vector3d center,
                               double size, float[] color, float alpha) {
        // Three perpendicular diamonds keep the tiny motes visible side-on, too.
        Vector3d[] axes = {new Vector3d(size, 0, 0), new Vector3d(0, size, 0), new Vector3d(0, 0, size)};
        for (int axis = 0; axis < 3; axis++) {
            Vector3d a = axes[axis];
            Vector3d b = axes[(axis + 1) % 3];
            for (int signA = -1; signA <= 1; signA += 2) {
                for (int signB = -1; signB <= 1; signB += 2) {
                    emit(out, pose, center, color, alpha);
                    emit(out, pose, center.add(a.scale(signA)), color, 0);
                    emit(out, pose, center.add(b.scale(signB)), color, 0);
                    emit(out, pose, center, color, alpha);
                }
            }
        }
    }

    private static Vector3d point(double radius, double angle, double z) {
        return new Vector3d(Math.cos(angle) * radius, Math.sin(angle) * radius, z);
    }

    private static Vector3d at(double x, double y, double radius, double angle, double z) {
        return point(radius, angle, z).add(x, y, 0);
    }

    private static void emit(IVertexBuilder out, Matrix4f pose, Vector3d p, float[] color, float alpha) {
        emit(out, pose, p.x, p.y, p.z, color, alpha);
    }

    private static void emit(IVertexBuilder out, Matrix4f pose, double x, double y, double z,
                               float[] color, float alpha) {
        out.vertex(pose, (float) x, (float) y, (float) z)
                .color(color[0], color[1], color[2], alpha).endVertex();
    }

    private static final class Palette {
        final float[][] metal;
        final float[] glow;

        Palette(float[] body, float[] bevel, float[] recess, float[] glow) {
            this.metal = new float[][]{body, bevel, recess};
            this.glow = glow;
        }
    }

    private static final class MeshBuilder {
        final List<Vertex> vertices = new ArrayList<>();

        void quad(Vector3d a, Vector3d b, Vector3d c, Vector3d d, int material, float shade) {
            for (Vector3d p : new Vector3d[]{a, b, c, d}) {
                vertices.add(new Vertex(p, material, shade));
            }
        }
    }

    private static final class Vertex {
        final float x;
        final float y;
        final float z;
        final int material;
        final float shade;

        Vertex(Vector3d p, int material, float shade) {
            this.x = (float) p.x;
            this.y = (float) p.y;
            this.z = (float) p.z;
            this.material = material;
            this.shade = shade;
        }
    }
}
