package net.kazi.kazimod.renderers.entities;

/** Allocation-free half-space clipping for a textured item primitive. */
final class BabylonQuadClipper {
    // XYZ, RGBA, UV, overlay UV, light UV, normal XYZ.
    static final int STRIDE = 16;
    interface Sink { void vertex(double[] attributes); }

    private final double[][] polygon = new double[4][STRIDE];
    private double nx, ny, nz, offset, normalSquared;

    void plane(double x, double y, double z, double d) {
        nx = x; ny = y; nz = z; offset = d;
        normalSquared = x * x + y * y + z * z;
    }

    double distance(double[] vertex) {
        return nx * vertex[0] + ny * vertex[1] + nz * vertex[2] + offset;
    }

    /** A quad's original triangle diagonal survives clipping, preserving its UV interpolation. */
    void quad(double[][] vertices, Sink sink) {
        int inside = 0;
        for (int i = 0; i < 4; i++) if (distance(vertices[i]) >= 0D) inside++;
        if (inside == 0) return;
        if (inside == 4) {
            for (int i = 0; i < 4; i++) sink.vertex(vertices[i]);
            return;
        }
        triangle(vertices[0], vertices[1], vertices[2], true, sink);
        triangle(vertices[0], vertices[2], vertices[3], true, sink);
    }

    void triangle(double[] a, double[] b, double[] c, boolean emitQuads, Sink sink) {
        int count = edge(c, a, 0);
        count = edge(a, b, count);
        count = edge(b, c, count);
        if (count < 3) return;
        if (emitQuads) {
            sink.vertex(polygon[0]); sink.vertex(polygon[1]); sink.vertex(polygon[2]);
            sink.vertex(polygon[count == 4 ? 3 : 2]);
        } else {
            for (int i = 1; i < count - 1; i++) {
                sink.vertex(polygon[0]); sink.vertex(polygon[i]); sink.vertex(polygon[i + 1]);
            }
        }
    }

    void line(double[] a, double[] b, Sink sink) {
        double da = distance(a), db = distance(b);
        if (da < 0D && db < 0D) return;
        if (da >= 0D && db >= 0D) { sink.vertex(a); sink.vertex(b); return; }
        intersect(a, b, da, db, polygon[0]);
        sink.vertex(da >= 0D ? a : polygon[0]);
        sink.vertex(db >= 0D ? b : polygon[0]);
    }

    private int edge(double[] a, double[] b, int count) {
        double da = distance(a), db = distance(b);
        boolean inA = da >= 0D, inB = db >= 0D;
        if (inA != inB) {
            intersect(a, b, da, db, polygon[count++]);
        }
        if (inB) System.arraycopy(b, 0, polygon[count++], 0, STRIDE);
        return count;
    }

    private void intersect(double[] a, double[] b, double da, double db, double[] out) {
        double t = da / (da - db);
        for (int i = 0; i < STRIDE; i++) out[i] = a[i] + (b[i] - a[i]) * t;
        // Correct floating-point drift so clipped intersections remain on the same plane.
        if (normalSquared > 0D) {
            double error = distance(out) / normalSquared;
            out[0] -= nx * error; out[1] -= ny * error; out[2] -= nz * error;
        }
        double length = Math.sqrt(out[13] * out[13] + out[14] * out[14] + out[15] * out[15]);
        if (length > 1.0E-12D) { out[13] /= length; out[14] /= length; out[15] /= length; }
    }
}
