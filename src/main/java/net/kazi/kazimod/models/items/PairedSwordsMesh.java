package net.kazi.kazimod.models.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Cached, genuinely three-dimensional blades. Grip center is the hand attachment origin.
 * No game dependencies: the exact same geometry can be inspected by the offline model probe.
 */
public final class PairedSwordsMesh {
    @FunctionalInterface
    public interface VertexSink {
        void vertex(float x, float y, float z, float nx, float ny, float nz, int rgb);
    }

    // Height, cutting edge, spine. The two swords share their curved silhouette.
    private static final double[][] BLADE = {
            {0.18, -0.185, 0.150}, {0.32, -0.145, 0.119},
            {0.52, -0.144, 0.088}, {0.74, -0.145, 0.082},
            {0.96, -0.123, 0.103}, {1.16, -0.072, 0.145},
            {1.34, 0.005, 0.191}, {1.51, 0.099, 0.179},
            {1.65, 0.184, 0.184}
    };
    private static final double FACE_Z = 0.026;
    // Overedge form: a longer sweeping cutting edge and a torn, feather-like inner edge.
    private static final double[][] ENHANCED_BLADE = {
            {0.18, -0.185, 0.120}, {0.23, -0.197, 0.225}, {0.245, -0.200, 0.140},
            {0.28, -0.206, 0.240}, {0.30, -0.210, 0.195}, {0.34, -0.214, 0.320},
            {0.355, -0.215, 0.230}, {0.37, -0.216, 0.185}, {0.40, -0.216, 0.268},
            {0.425, -0.217, 0.160}, {0.47, -0.218, 0.180}, {0.515, -0.219, 0.260},
            {0.53, -0.219, 0.190}, {0.565, -0.219, 0.210}, {0.595, -0.220, 0.145},
            {0.64, -0.220, 0.150}, {0.715, -0.220, 0.225}, {0.732, -0.220, 0.120},
            {0.80, -0.219, 0.160}, {0.843, -0.218, 0.190}, {0.868, -0.218, 0.130},
            {0.91, -0.217, 0.130}, {0.975, -0.216, 0.180}, {1.005, -0.214, 0.110},
            {1.09, -0.210, 0.100}, {1.155, -0.207, 0.170}, {1.185, -0.204, 0.088},
            {1.29, -0.195, 0.100}, {1.337, -0.191, 0.141}, {1.36, -0.188, 0.080},
            {1.44, -0.178, 0.094}, {1.515, -0.167, 0.120}, {1.55, -0.161, 0.095},
            {1.76, -0.108, 0.125}, {1.97, -0.025, 0.150},
            {2.16, 0.075, 0.185}, {2.34, 0.203, 0.210}, {2.40, 0.216, 0.216}
    };
    private static final List<Face> DARK = create(true, false);
    private static final List<Face> LIGHT = create(false, false);
    private static final List<Face> ENHANCED_DARK = create(true, true);
    private static final List<Face> ENHANCED_LIGHT = create(false, true);

    private PairedSwordsMesh() { }

    public static void emit(boolean dark, VertexSink out) {
        emit(dark, false, out);
    }

    public static void emit(boolean dark, boolean enhanced, VertexSink out) {
        List<Face> mesh = enhanced ? (dark ? ENHANCED_DARK : ENHANCED_LIGHT) : (dark ? DARK : LIGHT);
        for (Face face : mesh) {
            for (double[] point : face.points) {
                out.vertex((float)point[0], (float)point[1], (float)point[2],
                        face.nx, face.ny, face.nz, face.color);
            }
        }
    }

    private static List<Face> create(boolean dark, boolean enhanced) {
        List<Face> mesh = new ArrayList<>();
        double[][] profile = enhanced ? ENHANCED_BLADE : BLADE;
        int body = dark ? 0x262E2C : 0xDDD9EB;
        int bevel = dark ? 0x707A75 : 0xF6F3FF;
        for (int row = 0; row < profile.length - 1; row++) {
            double[] a = profile[row], b = profile[row + 1];
            double insetA = Math.min(0.020, (a[2] - a[1]) * 0.23);
            double insetB = Math.min(0.020, (b[2] - b[1]) * 0.23);
            for (int side : new int[]{-1, 1}) {
                double z = side * FACE_Z;
                quad(mesh, body, p(a[1] + insetA, a[0], z), p(a[2] - insetA, a[0], z),
                        p(b[2] - insetB, b[0], z), p(b[1] + insetB, b[0], z), side);
                quad(mesh, bevel, p(a[1], a[0], side * 0.004), p(a[1] + insetA, a[0], z),
                        p(b[1] + insetB, b[0], z), p(b[1], b[0], side * 0.004), side);
                quad(mesh, dark ? 0x48504B : 0xB4ADC9,
                        p(a[2] - insetA, a[0], z), p(a[2], a[0], side * 0.008),
                        p(b[2], b[0], side * 0.008), p(b[2] - insetB, b[0], z), side);
            }
            quad(mesh, bevel, p(a[1], a[0], -0.004), p(a[1], a[0], 0.004),
                    p(b[1], b[0], 0.004), p(b[1], b[0], -0.004), 1);
            quad(mesh, body, p(a[2], a[0], 0.008), p(a[2], a[0], -0.008),
                    p(b[2], b[0], -0.008), p(b[2], b[0], 0.008), 1);
        }
        // Seal the heel of the blade; no paper-thin faces when viewed edge-on.
        quad(mesh, body, p(profile[0][1], 0.18, -FACE_Z), p(profile[0][2], 0.18, -FACE_Z),
                p(profile[0][2], 0.18, FACE_Z), p(profile[0][1], 0.18, FACE_Z), 1);
        if (dark) hexInlays(mesh, profile);

        // Curved octagonal leather handle and individual diagonal wrap seams.
        double[][] grip = {
                {-0.305, 0.035, 0.042}, {-0.24, 0.015, 0.048},
                {-0.13, -0.009, 0.050}, {-0.02, -0.022, 0.051},
                {0.095, -0.014, 0.055}, {0.15, 0.0, 0.063}
        };
        for (int i = 0; i < grip.length - 1; i++) {
            tube(mesh, grip[i], grip[i + 1], 0x202826, 0x313C36);
        }
        for (int band = 0; band < 12; band++) {
            double y = -0.28 + band * 0.034;
            double[] g = gripAt(grip, y);
            for (int edge = 0; edge < 8; edge++) {
                double a = edge * Math.PI / 4, b = (edge + 1) * Math.PI / 4;
                double ay = y + Math.cos(a) * 0.014, by = y + Math.cos(b) * 0.014;
                quad(mesh, band % 3 == 0 ? 0x526057 : 0x3C4941,
                        p(g[1] + Math.cos(a) * (g[2] + 0.001), ay, Math.sin(a) * 0.041),
                        p(g[1] + Math.cos(b) * (g[2] + 0.001), by, Math.sin(b) * 0.041),
                        p(g[1] + Math.cos(b) * (g[2] + 0.001), by + 0.005, Math.sin(b) * 0.041),
                        p(g[1] + Math.cos(a) * (g[2] + 0.001), ay + 0.005, Math.sin(a) * 0.041), 1);
            }
        }
        plaque(mesh, new double[][]{{-0.030, -0.27}, {0.078, -0.252}, {0.097, -0.319},
                {0.065, -0.370}, {0.012, -0.373}, {-0.025, -0.340}}, 0.042, 0xA67139, 0xD3A764);
        if (enhanced) {
            // The branching steel grows over the old medallion/guard in the reference.
            plaque(mesh, new double[][]{{-0.107, 0.153}, {-0.122, 0.210}, {-0.053, 0.234},
                    {0.011, 0.216}, {0.097, 0.239}, {0.115, 0.180}, {0.037, 0.129}},
                    0.048, 0x9D652E, 0xDBAF65);
            enhancedVeins(mesh, dark);
            return mesh;
        }
        plaque(mesh, new double[][]{{-0.182, 0.183}, {-0.115, 0.218}, {-0.07, 0.270},
                {0.0, 0.223}, {0.121, 0.327}, {0.188, 0.290},
                {0.127, 0.165}, {0.046, 0.123}, {-0.045, 0.128}, {-0.094, 0.167}},
                0.048, 0x9D652E, 0xDBAF65);

        for (int side : new int[]{-1, 1}) {
            double z = side * (FACE_Z + 0.002);
            triangle(mesh, dark ? 0xDCD5ED : 0x303832,
                    p(-0.066, 0.270, z), p(0.048, 0.270, z), p(-0.033, 0.721, z), side);
            // Raised bronze rim and two interlocking halves on BOTH blade faces.
            disc(mesh, -0.007, 0.273, side * 0.054, 0.073, 0x6D4A29, 0, Math.PI * 2, side);
            disc(mesh, -0.007, 0.273, side * 0.055, 0.066, 0xDAD3EC, 0, Math.PI * 2, side);
            disc(mesh, -0.007, 0.273, side * 0.056, 0.066, 0x202925, Math.PI / 2, Math.PI * 1.5, side);
            disc(mesh, -0.007, 0.306, side * 0.057, 0.033, 0x202925, 0, Math.PI * 2, side);
            disc(mesh, -0.007, 0.240, side * 0.058, 0.033, 0xDAD3EC, 0, Math.PI * 2, side);
            disc(mesh, 0.006, 0.161, side * 0.055, 0.009, 0xF4DFC0, 0, Math.PI * 2, side);
        }
        return mesh;
    }

    private static void hexInlays(List<Face> mesh, double[][] profile) {
        double radius = 0.054, height = Math.sqrt(3) * radius;
        for (int col = -4; col <= 4; col++) {
            for (int row = 0; row < (profile == BLADE ? 19 : 27); row++) {
                double cx = col * radius * 1.5;
                double cy = 0.18 + (row + (Math.floorMod(col, 2) == 0 ? 0 : 0.5)) * height;
                // Each shared edge only once; clipped to the blade's beveled border.
                for (int edge = 0; edge < 3; edge++) {
                    double a = edge * Math.PI / 3, b = (edge + 1) * Math.PI / 3;
                    double x0 = cx + Math.cos(a) * radius, y0 = cy + Math.sin(a) * radius;
                    double x1 = cx + Math.cos(b) * radius, y1 = cy + Math.sin(b) * radius;
                    for (int strip = 0; strip < profile.length - 1; strip++) {
                        double[] low = profile[strip], high = profile[strip + 1];
                        double[] t = {0, 1};
                        double dx = x1 - x0, dy = y1 - y0, h = high[0] - low[0];
                        double ls = (high[1] - low[1]) / h, rs = (high[2] - low[2]) / h;
                        if (!clip(t, y0 - low[0], dy) || !clip(t, high[0] - y0, -dy)
                                || !clip(t, x0 - low[1] - ls * (y0 - low[0]) - 0.024, dx - ls * dy)
                                || !clip(t, low[2] + rs * (y0 - low[0]) - x0 - 0.024, rs * dy - dx)) continue;
                        double ax = x0 + dx * t[0], ay = y0 + dy * t[0];
                        double bx = x0 + dx * t[1], by = y0 + dy * t[1];
                        for (int side : new int[]{-1, 1}) line(mesh, ax, ay, bx, by,
                                side * (FACE_Z + 0.0006), 0.0011, 0x69342F, side);
                    }
                }
            }
        }
    }

    private static void enhancedVeins(List<Face> mesh, boolean dark) {
        int vein = dark ? 0x7C8989 : 0x9891AC;
        int shadow = dark ? 0x3B4748 : 0x78738D;
        int highlight = dark ? 0xACB6B0 : 0xF3F0FC;
        for (int side : new int[]{-1, 1}) {
            // Fine branching marbling follows the long blade rather than repeating a tile.
            for (int branch = 0; branch < 19; branch++) {
                double y = 0.24 + branch * 0.086;
                double rootX = 0.035 + Math.sin(branch * 1.31) * 0.019;
                double endY = Math.min(2.30, y + 0.32 + (branch % 4) * 0.071);
                double endX = (branch % 2 == 0 ? -0.145 : 0.120) + Math.max(0, endY - 1.67) * 0.34;
                double lastX = rootX, lastY = y;
                for (int step = 1; step <= 5; step++) {
                    double t = step / 5.0;
                    double x = rootX + (endX - rootX) * t
                            + Math.sin(branch * 2.17 + step * 2.41) * 0.012 * Math.sin(t * Math.PI);
                    double nextY = y + (endY - y) * t;
                    clippedVein(mesh, lastX, lastY, x, nextY, side,
                            0.0032 * (1 - t) + 0.0006, branch % 3 == 0 ? highlight : vein);
                    lastX = x; lastY = nextY;
                }
            }
            // Raised, faceted feather ridges rooted in the guard. These are actual 3D
            // geometry, not alpha planes; both faces have the same depth and detail.
            // Fixed-seed variation happens only once when building the cached mesh.
            Random detail = new Random(0xBA4E77L);
            for (int feather = 0; feather < 22; feather++) {
                double y = 0.205 + feather * 0.044 + detail.nextDouble() * 0.025;
                double rootX = -0.020 + detail.nextDouble() * 0.075;
                double tipY = y + 0.12 + detail.nextDouble() * 0.34;
                double tipX = feather % 2 == 0 ? -0.160 + detail.nextDouble() * 0.08
                        : 0.080 + detail.nextDouble() * 0.025;
                ridge(mesh, rootX, y, tipX, tipY, 0.004 + detail.nextDouble() * 0.009,
                        side, shadow, vein);
            }
            double[][] spine = {{0.015,0.19}, {0.046,0.42}, {0.025,0.68}, {0.055,0.94},
                    {0.035,1.19}, {0.074,1.47}, {0.045,1.71}, {0.124,1.97}, {0.168,2.21}, {0.203,2.34}};
            for (int i = 0; i < spine.length - 1; i++) {
                clippedVein(mesh, spine[i][0], spine[i][1], spine[i+1][0], spine[i+1][1],
                        side, 0.0028, highlight);
            }
        }
    }

    private static void clippedVein(List<Face> mesh, double ax, double ay, double bx, double by,
                                     int side, double width, int color) {
        double dx = bx - ax, dy = by - ay;
        for (int i = 0; i < ENHANCED_BLADE.length - 1; i++) {
            double[] low = ENHANCED_BLADE[i], high = ENHANCED_BLADE[i+1];
            double h = high[0] - low[0], ls = (high[1] - low[1]) / h, rs = (high[2] - low[2]) / h;
            double[] t = {0, 1};
            if (!clip(t, ay-low[0], dy) || !clip(t, high[0]-ay, -dy)
                    || !clip(t, ax-low[1]-ls*(ay-low[0])-0.024, dx-ls*dy)
                    || !clip(t, low[2]+rs*(ay-low[0])-ax-0.024, rs*dy-dx)) continue;
            line(mesh, ax+dx*t[0], ay+dy*t[0], ax+dx*t[1], ay+dy*t[1],
                    side * (FACE_Z+0.0014), width, color, side);
        }
    }

    private static void ridge(List<Face> mesh, double x, double y, double tipX, double tipY,
                               double width, int side, int dark, int light) {
        double z = side * (FACE_Z + 0.002);
        double[] left = p(x-width, y, z), right = p(x+width, y+0.016, z);
        double[] crest = p(x+(tipX-x)*0.30, y+(tipY-y)*0.31, side * (FACE_Z+width));
        double[] tip = p(tipX, tipY, side * (FACE_Z+0.003));
        triangle(mesh, light, left, crest, tip, side);
        triangle(mesh, dark, crest, right, tip, side);
        triangle(mesh, dark, left, right, crest, side);
    }

    private static boolean clip(double[] t, double value, double delta) {
        if (Math.abs(delta) < 1e-12) return value >= 0;
        double bound = -value / delta;
        if (delta > 0) t[0] = Math.max(t[0], bound);
        else t[1] = Math.min(t[1], bound);
        return t[0] < t[1];
    }

    private static void line(List<Face> mesh, double ax, double ay, double bx, double by,
                             double z, double width, int color, int side) {
        double length = Math.hypot(bx - ax, by - ay);
        if (length < 0.00001) return;
        double dx = -(by - ay) * width / length, dy = (bx - ax) * width / length;
        quad(mesh, color, p(ax + dx, ay + dy, z), p(ax - dx, ay - dy, z),
                p(bx - dx, by - dy, z), p(bx + dx, by + dy, z), side);
    }

    private static double[] gripAt(double[][] grip, double y) {
        for (int i = 0; i < grip.length - 1; i++) {
            if (y <= grip[i + 1][0]) {
                double f = (y - grip[i][0]) / (grip[i + 1][0] - grip[i][0]);
                return new double[]{y, grip[i][1] * (1 - f) + grip[i + 1][1] * f,
                        grip[i][2] * (1 - f) + grip[i + 1][2] * f};
            }
        }
        return grip[grip.length - 1];
    }

    private static void tube(List<Face> mesh, double[] a, double[] b, int color, int alternate) {
        for (int i = 0; i < 8; i++) {
            double u = i * Math.PI / 4, v = (i + 1) * Math.PI / 4;
            quad(mesh, i % 3 == 0 ? alternate : color,
                    p(a[1] + Math.cos(u) * a[2], a[0], Math.sin(u) * 0.04),
                    p(a[1] + Math.cos(v) * a[2], a[0], Math.sin(v) * 0.04),
                    p(b[1] + Math.cos(v) * b[2], b[0], Math.sin(v) * 0.04),
                    p(b[1] + Math.cos(u) * b[2], b[0], Math.sin(u) * 0.04), 1);
        }
    }

    private static void plaque(List<Face> mesh, double[][] outline, double depth, int body, int edge) {
        double cx = 0, cy = 0;
        for (double[] v : outline) { cx += v[0]; cy += v[1]; }
        cx /= outline.length;
        cy /= outline.length;
        for (int i = 0; i < outline.length; i++) {
            double[] a = outline[i], b = outline[(i + 1) % outline.length];
            for (int side : new int[]{-1, 1}) {
                double[] ai = p(cx + (a[0] - cx) * 0.82, cy + (a[1] - cy) * 0.82, side * depth);
                double[] bi = p(cx + (b[0] - cx) * 0.82, cy + (b[1] - cy) * 0.82, side * depth);
                triangle(mesh, body, p(cx, cy, side * depth), ai, bi, side);
                quad(mesh, edge, p(a[0], a[1], side * depth * 0.65),
                        p(b[0], b[1], side * depth * 0.65), bi, ai, side);
            }
            quad(mesh, body, p(a[0], a[1], -depth * 0.65), p(b[0], b[1], -depth * 0.65),
                    p(b[0], b[1], depth * 0.65), p(a[0], a[1], depth * 0.65), 1);
        }
    }

    private static void disc(List<Face> mesh, double x, double y, double z, double radius,
                             int color, double start, double end, int side) {
        int steps = Math.max(6, (int)Math.ceil((end - start) * 4));
        for (int i = 0; i < steps; i++) {
            double a = start + (end - start) * i / steps, b = start + (end - start) * (i + 1) / steps;
            triangle(mesh, color, p(x, y, z), p(x + Math.cos(a) * radius, y + Math.sin(a) * radius, z),
                    p(x + Math.cos(b) * radius, y + Math.sin(b) * radius, z), side);
        }
    }

    private static double[] p(double x, double y, double z) { return new double[]{x, y, z}; }

    private static void triangle(List<Face> mesh, int color, double[] a, double[] b, double[] c, int side) {
        quad(mesh, color, a, b, c, c, side);
    }

    private static void quad(List<Face> mesh, int color, double[] a, double[] b, double[] c, double[] d, int side) {
        if (side < 0) { double[] swap = b; b = d; d = swap; }
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        // A reversed triangle is a quad whose first triangle is degenerate. Its second
        // triangle still has area (also true at the blade tip), so retain that face.
        if (length < 1e-12) {
            vx = d[0] - a[0]; vy = d[1] - a[1]; vz = d[2] - a[2];
            nx = uy * vz - uz * vy; ny = uz * vx - ux * vz; nz = ux * vy - uy * vx;
            length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        }
        if (length < 1e-12) return;
        mesh.add(new Face(new double[][]{a, b, c, d}, color,
                (float)(nx / length), (float)(ny / length), (float)(nz / length)));
    }

    private static final class Face {
        final double[][] points;
        final int color;
        final float nx, ny, nz;
        Face(double[][] points, int color, float nx, float ny, float nz) {
            this.points = points;
            this.color = color;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }
}
