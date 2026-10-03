package net.kazi.kazimod.entities;

import java.util.ArrayList;
import java.util.List;

/** Shared, engine-independent release timing and beam/box collision geometry. */
public final class EaRuptureShape {
    public static final int CHARGE_TICKS = 180;
    public static final int RELEASE_TICKS = 220;
    public static final int FADE_TICKS = 40;
    // ea_cast.ogg has a baked-in fade from 20 to 22 seconds after charge starts.
    public static final int AUDIO_FADE_START_TICKS = CHARGE_TICKS + RELEASE_TICKS;
    public static final int CANCEL_TICKS = 10;
    public static final int DEPLOY_TICKS = 12;
    public static final double MAX_RANGE = 146;
    public static final double MAX_RADIUS = 8;

    private EaRuptureShape() { }

    public static double length(float age, double range) {
        return Math.max(0, range) * Math.min(1, Math.max(0, age) / (double)DEPLOY_TICKS);
    }

    public static double radius(float age) {
        return MAX_RADIUS * Math.min(1, Math.max(0, age) / 10.0);
    }

    public static boolean damaging(float age) {
        return age > 0 && age < RELEASE_TICKS;
    }

    /** Intersects a finite circular cylinder, including its flat caps, with a box. */
    public static boolean intersects(double ox, double oy, double oz, double dx, double dy, double dz,
                                     double length, double radius, double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        if (length <= 0 || radius <= 0 || !Double.isFinite(length) || !Double.isFinite(radius)) return false;
        double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(magnitude) || magnitude < 1.0e-9) return false;
        double[] origin = {ox, oy, oz};
        double[] direction = {dx / magnitude, dy / magnitude, dz / magnitude};
        double[] low = {minX, minY, minZ};
        double[] high = {maxX, maxY, maxZ};
        double near = 0, far = 0;
        for (int i = 0; i < 3; i++) {
            if (!Double.isFinite(origin[i]) || !Double.isFinite(low[i]) || !Double.isFinite(high[i])
                    || low[i] > high[i]) return false;
            near += (direction[i] >= 0 ? low[i] - origin[i] : high[i] - origin[i]) * direction[i];
            far += (direction[i] >= 0 ? high[i] - origin[i] : low[i] - origin[i]) * direction[i];
        }
        if (far < 0 || near > length) return false;

        // A segment through the box is already inside the cylinder. Otherwise
        // the minimum radial distance lies on an edge of the clipped box.
        double entry = 0, exit = length;
        boolean axisIntersects = true;
        for (int i = 0; i < 3; i++) {
            if (direction[i] == 0) {
                if (origin[i] < low[i] || origin[i] > high[i]) axisIntersects = false;
            } else {
                double first = (low[i] - origin[i]) / direction[i];
                double second = (high[i] - origin[i]) / direction[i];
                entry = Math.max(entry, Math.min(first, second));
                exit = Math.min(exit, Math.max(first, second));
            }
        }
        if (axisIntersects && entry <= exit) return true;

        // Intersect the box with the two cap half-spaces. Every resulting
        // vertex is either an original corner or a box-edge/cap intersection.
        double[][] corners = new double[8][3];
        double[] axial = new double[8];
        List<double[]> radialVertices = new ArrayList<>(20);
        for (int corner = 0; corner < 8; corner++) {
            for (int i = 0; i < 3; i++) {
                corners[corner][i] = ((corner & (1 << i)) == 0 ? low[i] : high[i]) - origin[i];
                axial[corner] += corners[corner][i] * direction[i];
            }
            if (axial[corner] >= 0 && axial[corner] <= length) {
                addRadialVertex(radialVertices, corners[corner], axial[corner], direction);
            }
        }
        for (int corner = 0; corner < 8; corner++) {
            for (int axis = 0; axis < 3; axis++) {
                if ((corner & (1 << axis)) != 0) continue;
                int other = corner | (1 << axis);
                double delta = axial[other] - axial[corner];
                if (delta == 0) continue;
                for (int cap = 0; cap < 2; cap++) {
                    double plane = cap == 0 ? 0 : length;
                    double t = (plane - axial[corner]) / delta;
                    if (t < 0 || t > 1) continue;
                    double[] point = corners[corner].clone();
                    point[axis] += t * (corners[other][axis] - corners[corner][axis]);
                    addRadialVertex(radialVertices, point, plane, direction);
                }
            }
        }

        // Test all vertex chords. They include every boundary edge and remain
        // inside this convex clipped box, so they introduce no extra volume.
        // If a minimum lies inside a face parallel to the beam, sliding along
        // the beam reaches an edge with the same radial distance. Other face
        // interiors can attain a minimum only on the already-tested beam axis.
        double limit = radius * radius;
        for (int first = 0; first < radialVertices.size(); first++) {
            double[] a = radialVertices.get(first);
            double vertexDistance = a[0] * a[0] + a[1] * a[1] + a[2] * a[2];
            if (vertexDistance <= limit) return true;
            for (int second = first + 1; second < radialVertices.size(); second++) {
                double[] b = radialVertices.get(second);
                double x = b[0] - a[0], y = b[1] - a[1], z = b[2] - a[2];
                double span = x * x + y * y + z * z;
                if (span == 0) continue;
                double t = Math.max(0, Math.min(1, -(a[0] * x + a[1] * y + a[2] * z) / span));
                x = a[0] + t * x;
                y = a[1] + t * y;
                z = a[2] + t * z;
                if (x * x + y * y + z * z <= limit) return true;
            }
        }
        return false;
    }

    private static void addRadialVertex(List<double[]> vertices, double[] point,
                                        double axial, double[] direction) {
        vertices.add(new double[]{point[0] - axial * direction[0],
                point[1] - axial * direction[1], point[2] - axial * direction[2]});
    }
}
