package net.kazi.kazimod.renderers.entities.asriel;

import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Hollow pastel beam heads and tapered ribbons following the projectile's real curved flight. */
public final class AngelOfDeathVisuals {
    private static final double SIZE_SCALE = 2.0D;
    public static final double PADDING = .75D * SIZE_SCALE;
    private static final double TAU = Math.PI * 2;
    private static final float[] WHITE = {1F, 1F, 1F};
    private static final float[][] COLORS = {
            {1F, .69F, .79F}, {1F, .85F, .64F}, {.97F, 1F, .66F},
            {.71F, 1F, .72F}, {.67F, .96F, 1F}, {.75F, .79F, 1F}, {.93F, .71F, 1F}};

    private AngelOfDeathVisuals() { }

    /** Trail is head-first in render-local coordinates; view is the camera in that same space. */
    public static void render(Sink out, double[][] trail, float age, int tint, int detail,
                              double viewX, double viewY, double viewZ) {
        if (trail == null || trail.length == 0 || !Float.isFinite(age) || age < 0
                || !Double.isFinite(viewX + viewY + viewZ)) return;
        for (double[] point : trail)
            if (point == null || point.length < 3 || !Double.isFinite(point[0] + point[1] + point[2])) return;
        detail = Math.max(0, Math.min(2, detail));
        float[] color = COLORS[Math.floorMod(tint, COLORS.length)];
        double[] view = unit(viewX - trail[0][0], viewY - trail[0][1], viewZ - trail[0][2], 0, 0, 1);
        double[] forward = {0, 0, 1};
        for (int i = 1; i < trail.length; i++) {
            double x = trail[0][0] - trail[i][0], y = trail[0][1] - trail[i][1], z = trail[0][2] - trail[i][2];
            if (x*x + y*y + z*z > .000001) { forward = unit(x, y, z, 0, 0, 1); break; }
        }
        double dot = forward[0]*view[0] + forward[1]*view[1] + forward[2]*view[2];
        double[] along = unit(forward[0] - dot*view[0], forward[1] - dot*view[1], forward[2] - dot*view[2],
                Math.abs(view[1]) < .9 ? 0 : 1, Math.abs(view[1]) < .9 ? 1 : 0, 0);
        double[] across = crossUnit(view, along);
        // Reproject the fallback too, so the head stays open even when looking straight along its flight.
        along = crossUnit(across, view);
        if (trail.length > 1) ribbons(out, trail, view, across, color, detail);
        head(out, trail[0], along, across, color, age, detail);
    }

    private static void ribbons(Sink out, double[][] trail, double[] view, double[] fallback,
                                 float[] color, int detail) {
        int steps = detail == 0 ? 24 : detail == 1 ? 12 : 6;
        double[][] points = new double[steps + 1][], sides = new double[steps + 1][];
        for (int i = 0; i <= steps; i++) points[i] = sample(trail, i * (trail.length - 1.0) / steps);
        for (int i = 0; i <= steps; i++) {
            double[] a = points[Math.max(0, i - 1)], b = points[Math.min(steps, i + 1)];
            double[] tangent = unit(a[0] - b[0], a[1] - b[1], a[2] - b[2], 0, 0, 1);
            double x = view[1]*tangent[2] - view[2]*tangent[1];
            double y = view[2]*tangent[0] - view[0]*tangent[2];
            double z = view[0]*tangent[1] - view[1]*tangent[0];
            sides[i] = unit(x, y, z, fallback[0], fallback[1], fallback[2]);
            if (i > 0 && sides[i][0]*sides[i-1][0] + sides[i][1]*sides[i-1][1] + sides[i][2]*sides[i-1][2] < 0)
                for (int axis = 0; axis < 3; axis++) sides[i][axis] *= -1;
        }
        for (int i = 0; i < steps; i++) {
            double t0 = i / (double) steps, t1 = (i + 1.0) / steps;
            double w0 = .075 * SIZE_SCALE * Math.pow(1 - t0, .65), w1 = .075 * SIZE_SCALE * Math.pow(1 - t1, .65);
            // Leave the oval's center empty. The trailing ribbon joins the rear half of its rim.
            double head0 = Math.min(1, t0 * steps / 1.5), head1 = Math.min(1, t1 * steps / 1.5);
            float a0 = (float) ((1 - t0) * head0), a1 = (float) ((1 - t1) * head1);
            if (detail == 0) strip(out, points[i], points[i+1], sides[i], sides[i+1], w0 * 3.0, w1 * 3.0,
                    color, a0 * .11F, a1 * .11F);
            strip(out, points[i], points[i+1], sides[i], sides[i+1], w0, w1, color, a0 * .86F, a1 * .86F);
            strip(out, points[i], points[i+1], sides[i], sides[i+1], w0 * .24, w1 * .24,
                    WHITE, a0 * .58F, a1 * .58F);
        }
    }

    private static void head(Sink out, double[] center, double[] along, double[] across,
                            float[] color, float age, int detail) {
        int sides = detail == 0 ? 18 : detail == 1 ? 14 : 10;
        double pulse = 1 + .035 * Math.sin(age * .9);
        ring(out, center, along, across, .34*pulse, .16*pulse, .052, color, .96F, sides);
        ring(out, center, along, across, .294*pulse, .118*pulse, .012, WHITE, .83F, sides);
        if (detail == 0)
            ring(out, center, along, across, .38*pulse, .20*pulse, .045, color, .13F, sides);
    }

    private static void ring(Sink out, double[] center, double[] along, double[] across,
                            double major, double minor, double width, float[] color, float alpha, int sides) {
        major *= SIZE_SCALE;
        minor *= SIZE_SCALE;
        width *= SIZE_SCALE;
        for (int i = 0; i < sides; i++) {
            double a = TAU*i/sides, b = TAU*(i+1)/sides;
            point(out, center, along, across, Math.cos(a)*major, Math.sin(a)*minor, color, alpha);
            point(out, center, along, across, Math.cos(b)*major, Math.sin(b)*minor, color, alpha);
            point(out, center, along, across, Math.cos(b)*(major-width), Math.sin(b)*(minor-width), color, alpha);
            point(out, center, along, across, Math.cos(a)*(major-width), Math.sin(a)*(minor-width), color, alpha);
        }
    }

    private static void strip(Sink out, double[] a, double[] b, double[] sa, double[] sb, double wa, double wb,
                               float[] color, float alphaA, float alphaB) {
        vertex(out, a, sa, -wa, color, alphaA); vertex(out, b, sb, -wb, color, alphaB);
        vertex(out, b, sb, wb, color, alphaB); vertex(out, a, sa, wa, color, alphaA);
    }

    private static void vertex(Sink out, double[] p, double[] side, double width, float[] color, float alpha) {
        out.vertex(p[0]+side[0]*width, p[1]+side[1]*width, p[2]+side[2]*width, color[0], color[1], color[2], alpha);
    }

    private static void point(Sink out, double[] center, double[] a, double[] b, double x, double y,
                              float[] color, float alpha) {
        out.vertex(center[0]+a[0]*x+b[0]*y, center[1]+a[1]*x+b[1]*y, center[2]+a[2]*x+b[2]*y,
                color[0], color[1], color[2], alpha);
    }

    /** Catmull-Rom interpolation removes tick-sized corners without inventing a straight replacement tail. */
    private static double[] sample(double[][] points, double position) {
        int i = Math.min(points.length - 2, (int) position);
        double t = position - i, t2 = t*t, t3 = t2*t;
        double[] a = points[Math.max(0,i-1)], b = points[i], c = points[i+1], d = points[Math.min(points.length-1,i+2)];
        double[] out = new double[3];
        for (int axis=0;axis<3;axis++) {
            double value = .5 * (2*b[axis] + (-a[axis]+c[axis])*t
                    + (2*a[axis]-5*b[axis]+4*c[axis]-d[axis])*t2 + (-a[axis]+3*b[axis]-3*c[axis]+d[axis])*t3);
            out[axis] = Math.max(Math.min(Math.min(a[axis],b[axis]),Math.min(c[axis],d[axis])),
                    Math.min(Math.max(Math.max(a[axis],b[axis]),Math.max(c[axis],d[axis])),value));
        }
        return out;
    }

    private static double[] crossUnit(double[] a, double[] b) {
        return unit(a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0], 1, 0, 0);
    }

    private static double[] unit(double x, double y, double z, double fx, double fy, double fz) {
        double length = Math.sqrt(x*x+y*y+z*z);
        return length < .000001 ? new double[]{fx,fy,fz} : new double[]{x/length,y/length,z/length};
    }
}
