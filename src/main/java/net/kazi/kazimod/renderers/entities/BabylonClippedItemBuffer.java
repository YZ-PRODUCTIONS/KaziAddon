package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector4f;

/** Reusable item-buffer adapter retaining only the front half of a portal's plane. */
final class BabylonClippedItemBuffer implements IRenderTypeBuffer {
    private final List<Builder> builders = new ArrayList<>();
    private final Vector4f origin = new Vector4f(), xAxis = new Vector4f();
    private final Vector4f yAxis = new Vector4f(), zAxis = new Vector4f();
    private IRenderTypeBuffer target;
    private double nx, ny, nz, offset;
    private int used;

    void begin(IRenderTypeBuffer buffers, Matrix4f portalPose) {
        target = buffers;
        used = 0;
        origin.set(0F, 0F, 0F, 1F); origin.transform(portalPose);
        xAxis.set(1F, 0F, 0F, 0F); xAxis.transform(portalPose);
        yAxis.set(0F, 1F, 0F, 0F); yAxis.transform(portalPose);
        zAxis.set(0F, 0F, 1F, 0F); zAxis.transform(portalPose);
        // Tangent cross-products correctly handle rotated, scaled and mirrored portal poses.
        nx = (double) xAxis.y() * yAxis.z() - (double) xAxis.z() * yAxis.y();
        ny = (double) xAxis.z() * yAxis.x() - (double) xAxis.x() * yAxis.z();
        nz = (double) xAxis.x() * yAxis.y() - (double) xAxis.y() * yAxis.x();
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 1.0E-12D) { nx /= length; ny /= length; nz /= length; }
        if (nx * zAxis.x() + ny * zAxis.y() + nz * zAxis.z() < 0D) {
            nx = -nx; ny = -ny; nz = -nz;
        }
        offset = -nx * origin.x() - ny * origin.y() - nz * origin.z();
    }

    void end() {
        for (int i = 0; i < used; i++) builders.get(i).reset();
        target = null;
        used = 0;
    }

    @Override
    public IVertexBuilder getBuffer(RenderType type) {
        Builder builder = null;
        for (int i = 0; i < used; i++) {
            if (builders.get(i).type == type) { builder = builders.get(i); break; }
        }
        if (builder == null) {
            if (used == builders.size()) builders.add(new Builder());
            builder = builders.get(used++);
            builder.type = type;
            builder.mode = type.mode();
            builder.clipper.plane(nx, ny, nz, offset);
        }
        // Reacquire the underlying builder: a material switch may have flushed its shared buffer.
        builder.target = target.getBuffer(type);
        return builder;
    }

    private static final class Builder implements IVertexBuilder {
        private final double[][] data = new double[4][BabylonQuadClipper.STRIDE];
        private final BabylonQuadClipper clipper = new BabylonQuadClipper();
        private final BabylonQuadClipper.Sink output = this::emit;
        private RenderType type;
        private IVertexBuilder target;
        private int count, mode;

        Builder() { for (double[] vertex : data) defaults(vertex); }

        @Override public IVertexBuilder vertex(double x, double y, double z) {
            data[count][0] = x; data[count][1] = y; data[count][2] = z; return this;
        }
        @Override public IVertexBuilder color(int r, int g, int b, int a) {
            data[count][3] = r; data[count][4] = g; data[count][5] = b; data[count][6] = a; return this;
        }
        @Override public IVertexBuilder uv(float u, float v) {
            data[count][7] = u; data[count][8] = v; return this;
        }
        @Override public IVertexBuilder overlayCoords(int u, int v) {
            data[count][9] = u; data[count][10] = v; return this;
        }
        @Override public IVertexBuilder uv2(int u, int v) {
            data[count][11] = u; data[count][12] = v; return this;
        }
        @Override public IVertexBuilder normal(float x, float y, float z) {
            data[count][13] = x; data[count][14] = y; data[count][15] = z; return this;
        }
        @Override public void endVertex() {
            if (mode == 7) {
                if (++count == 4) { clipper.quad(data, output); count = 0; }
            } else if (mode == 4) {
                if (++count == 3) { clipper.triangle(data[0], data[1], data[2], false, output); count = 0; }
            } else if (mode == 1) {
                if (++count == 2) { clipper.line(data[0], data[1], output); count = 0; }
            } else if (mode == 0) {
                if (clipper.distance(data[0]) >= 0D) emit(data[0]);
            }
            // Item models use QUADS; unsupported strip/fan streams are suppressed rather than leaked.
            defaults(data[count]);
        }

        private void emit(double[] v) {
            target.vertex(v[0], v[1], v[2]).color(integer(v[3]), integer(v[4]), integer(v[5]), integer(v[6]))
                    .uv((float) v[7], (float) v[8]).overlayCoords(integer(v[9]), integer(v[10]))
                    .uv2(integer(v[11]), integer(v[12])).normal((float) v[13], (float) v[14], (float) v[15]).endVertex();
        }
        private void reset() {
            count = 0; type = null; target = null;
            for (double[] vertex : data) defaults(vertex);
        }
        private static int integer(double value) { return (int) Math.round(value); }
        private static void defaults(double[] vertex) {
            for (int i = 0; i < vertex.length; i++) vertex[i] = 0D;
            vertex[3] = vertex[4] = vertex[5] = vertex[6] = 255D;
            vertex[15] = 1D;
        }
    }
}
