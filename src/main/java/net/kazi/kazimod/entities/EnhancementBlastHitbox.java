package net.kazi.kazimod.entities;

import java.util.Arrays;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

/** Animated main-blast mesh inside an immutable release transform. */
public final class EnhancementBlastHitbox {
    private final Vector3d origin;
    private final Vector3d forward;
    private final float yaw;
    private AxisAlignedBB bounds;
    private float age = Float.NaN;
    private int seed;
    private boolean active;
    private double[] vertices = new double[0];
    private double[] triangleBounds = new double[0];
    private double[] componentBounds = new double[0];
    private int[] starts = new int[0];
    private int[] ends = new int[0];
    private double[] crossings = new double[16];

    public EnhancementBlastHitbox(double x, double y, double z, float yaw) {
        this.origin = new Vector3d(x, y, z);
        this.yaw = yaw;
        double angle = Math.toRadians(yaw);
        this.forward = new Vector3d(-Math.sin(angle), 0, Math.cos(angle));
        this.bounds = new AxisAlignedBB(x, y, z, x, y, z);
    }

    public Vector3d getOrigin() { return this.origin; }
    public Vector3d getForward() { return this.forward; }
    public AxisAlignedBB getBounds() { return this.bounds; }
    public float getYaw() { return this.yaw; }

    /** The renderer and server evaluate the same surface at the same phase age and seed. */
    public void update(float effectAge, int seed) {
        if (this.age == effectAge && this.seed == seed) return;
        this.age = effectAge;
        this.seed = seed;
        this.active = EnhancementBlastShape.surfaceOpacity(effectAge) > 0.001F;
        if (!this.active) {
            this.bounds = new AxisAlignedBB(origin.x, origin.y, origin.z, origin.x, origin.y, origin.z);
            return;
        }

        EnhancementBlastShape.Mesh mesh = EnhancementBlastShape.generate(effectAge, seed);
        int triangles = mesh.vertexCount() / 4;
        if (this.vertices.length != triangles * 9) {
            this.vertices = new double[triangles * 9];
            this.triangleBounds = new double[triangles * 6];
        }
        int components = mesh.componentCount();
        if (this.starts.length != components) {
            this.starts = new int[components];
            this.ends = new int[components];
            this.componentBounds = new double[components * 6];
        }

        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (int component = 0; component < components; component++) {
            this.starts[component] = mesh.componentStart(component) / 4;
            this.ends[component] = mesh.componentEnd(component) / 4;
            double cMinX = Double.POSITIVE_INFINITY, cMinY = cMinX, cMinZ = cMinX;
            double cMaxX = Double.NEGATIVE_INFINITY, cMaxY = cMaxX, cMaxZ = cMaxX;
            for (int triangle = starts[component]; triangle < ends[component]; triangle++) {
                double tMinX = Double.POSITIVE_INFINITY, tMinY = tMinX, tMinZ = tMinX;
                double tMaxX = Double.NEGATIVE_INFINITY, tMaxY = tMaxX, tMaxZ = tMaxX;
                for (int corner = 0; corner < 3; corner++) {
                    int vertex = triangle * 4 + corner;
                    double localX = mesh.x(vertex, 1.0F);
                    double localZ = mesh.z(vertex, 1.0F);
                    double x = origin.x + localX * forward.z + localZ * forward.x;
                    double y = origin.y + mesh.y(vertex, 1.0F) + EnhancementBlastShape.RENDER_OFFSET_Y;
                    double z = origin.z - localX * forward.x + localZ * forward.z;
                    int index = triangle * 9 + corner * 3;
                    this.vertices[index] = x;
                    this.vertices[index + 1] = y;
                    this.vertices[index + 2] = z;
                    tMinX = Math.min(tMinX, x); tMaxX = Math.max(tMaxX, x);
                    tMinY = Math.min(tMinY, y); tMaxY = Math.max(tMaxY, y);
                    tMinZ = Math.min(tMinZ, z); tMaxZ = Math.max(tMaxZ, z);
                }
                putBounds(triangleBounds, triangle * 6, tMinX, tMinY, tMinZ, tMaxX, tMaxY, tMaxZ);
                cMinX = Math.min(cMinX, tMinX); cMaxX = Math.max(cMaxX, tMaxX);
                cMinY = Math.min(cMinY, tMinY); cMaxY = Math.max(cMaxY, tMaxY);
                cMinZ = Math.min(cMinZ, tMinZ); cMaxZ = Math.max(cMaxZ, tMaxZ);
            }
            putBounds(componentBounds, component * 6, cMinX, cMinY, cMinZ, cMaxX, cMaxY, cMaxZ);
            minX = Math.min(minX, cMinX); maxX = Math.max(maxX, cMaxX);
            minY = Math.min(minY, cMinY); maxY = Math.max(maxY, cMaxY);
            minZ = Math.min(minZ, cMinZ); maxZ = Math.max(maxZ, cMaxZ);
        }
        this.bounds = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Bounding boxes are filters only; damage uses the union of the closed mesh components. */
    public boolean intersects(AxisAlignedBB target) {
        if (!this.active || !this.bounds.intersects(target)) return false;
        double x = (target.minX + target.maxX) * 0.5D;
        double y = (target.minY + target.maxY) * 0.5D;
        double z = (target.minZ + target.maxZ) * 0.5D;
        double hx = (target.maxX - target.minX) * 0.5D;
        double hy = (target.maxY - target.minY) * 0.5D;
        double hz = (target.maxZ - target.minZ) * 0.5D;
        for (int component = 0; component < starts.length; component++) {
            if (!overlaps(componentBounds, component * 6, target)) continue;
            for (int triangle = starts[component]; triangle < ends[component]; triangle++) {
                if (overlaps(triangleBounds, triangle * 6, target)
                        && triangleIntersects(triangle * 9, x, y, z, hx, hy, hz)) return true;
            }
            // A target wholly inside a solid does not cross any surface triangle.
            if (contains(component, x, y, z)) return true;
        }
        return false;
    }

    /** Nearest point in the actual closed blast volumes, preserving gaps between eruptions. */
    public Vector3d closestPoint(Vector3d point) {
        if (!this.active || point == null) return null;
        Vector3d nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (int component = 0; component < starts.length; component++) {
            double boundsDistance = distanceToBounds(componentBounds, component * 6, point);
            if (boundsDistance == 0 && contains(component, point.x, point.y, point.z)) return point;
            if (boundsDistance > best) continue;
            for (int triangle = starts[component]; triangle < ends[component]; triangle++) {
                if (distanceToBounds(triangleBounds, triangle * 6, point) > best) continue;
                Vector3d candidate = closestTrianglePoint(triangle * 9, point);
                double distance = candidate.distanceToSqr(point);
                if (distance < best) {
                    best = distance;
                    nearest = candidate;
                }
            }
        }
        return nearest;
    }

    private Vector3d closestTrianglePoint(int i, Vector3d point) {
        Vector3d a = new Vector3d(vertices[i], vertices[i + 1], vertices[i + 2]);
        Vector3d b = new Vector3d(vertices[i + 3], vertices[i + 4], vertices[i + 5]);
        Vector3d c = new Vector3d(vertices[i + 6], vertices[i + 7], vertices[i + 8]);
        Vector3d ab = b.subtract(a), ac = c.subtract(a), ap = point.subtract(a);
        // A collapsed deployment triangle is still a line/point, never a NaN contact.
        if (ab.cross(ac).lengthSqr() < 1.0E-20D) {
            Vector3d first = closestSegmentPoint(a, b, point);
            Vector3d second = closestSegmentPoint(b, c, point);
            Vector3d third = closestSegmentPoint(c, a, point);
            if (second.distanceToSqr(point) < first.distanceToSqr(point)) first = second;
            return third.distanceToSqr(point) < first.distanceToSqr(point) ? third : first;
        }
        double d1 = ab.dot(ap), d2 = ac.dot(ap);
        if (d1 <= 0 && d2 <= 0) return a;
        Vector3d bp = point.subtract(b);
        double d3 = ab.dot(bp), d4 = ac.dot(bp);
        if (d3 >= 0 && d4 <= d3) return b;
        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0 && d1 >= 0 && d3 <= 0) return a.add(ab.scale(d1 / (d1 - d3)));
        Vector3d cp = point.subtract(c);
        double d5 = ab.dot(cp), d6 = ac.dot(cp);
        if (d6 >= 0 && d5 <= d6) return c;
        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0 && d2 >= 0 && d6 <= 0) return a.add(ac.scale(d2 / (d2 - d6)));
        double va = d3 * d6 - d5 * d4;
        if (va <= 0 && d4 - d3 >= 0 && d5 - d6 >= 0) {
            return b.add(c.subtract(b).scale((d4 - d3) / ((d4 - d3) + (d5 - d6))));
        }
        double inverse = 1.0D / (va + vb + vc);
        return a.add(ab.scale(vb * inverse)).add(ac.scale(vc * inverse));
    }

    private static Vector3d closestSegmentPoint(Vector3d start, Vector3d end, Vector3d point) {
        Vector3d edge = end.subtract(start);
        double lengthSquared = edge.lengthSqr();
        if (lengthSquared < 1.0E-20D) return start;
        double t = Math.max(0, Math.min(1, point.subtract(start).dot(edge) / lengthSquared));
        return start.add(edge.scale(t));
    }

    private static double distanceToBounds(double[] data, int i, Vector3d point) {
        double dx = Math.max(data[i] - point.x, Math.max(0, point.x - data[i + 3]));
        double dy = Math.max(data[i + 1] - point.y, Math.max(0, point.y - data[i + 4]));
        double dz = Math.max(data[i + 2] - point.z, Math.max(0, point.z - data[i + 5]));
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean triangleIntersects(int i, double x, double y, double z,
                                       double hx, double hy, double hz) {
        double ax = vertices[i] - x, ay = vertices[i + 1] - y, az = vertices[i + 2] - z;
        double bx = vertices[i + 3] - x, by = vertices[i + 4] - y, bz = vertices[i + 5] - z;
        double cx = vertices[i + 6] - x, cy = vertices[i + 7] - y, cz = vertices[i + 8] - z;
        double ex = bx - ax, ey = by - ay, ez = bz - az;
        double fx = cx - bx, fy = cy - by, fz = cz - bz;
        if (separated(ey * fz - ez * fy, ez * fx - ex * fz, ex * fy - ey * fx,
                ax, ay, az, bx, by, bz, cx, cy, cz, hx, hy, hz)) return false;
        for (int edge = 0; edge < 3; edge++) {
            double dx = edge == 0 ? ex : edge == 1 ? fx : ax - cx;
            double dy = edge == 0 ? ey : edge == 1 ? fy : ay - cy;
            double dz = edge == 0 ? ez : edge == 1 ? fz : az - cz;
            if (separated(0, -dz, dy, ax, ay, az, bx, by, bz, cx, cy, cz, hx, hy, hz)
                    || separated(dz, 0, -dx, ax, ay, az, bx, by, bz, cx, cy, cz, hx, hy, hz)
                    || separated(-dy, dx, 0, ax, ay, az, bx, by, bz, cx, cy, cz, hx, hy, hz)) {
                return false;
            }
        }
        return true;
    }

    private static boolean separated(double nx, double ny, double nz,
                                     double ax, double ay, double az,
                                     double bx, double by, double bz,
                                     double cx, double cy, double cz,
                                     double hx, double hy, double hz) {
        double a = nx * ax + ny * ay + nz * az;
        double b = nx * bx + ny * by + nz * bz;
        double c = nx * cx + ny * cy + nz * cz;
        double radius = hx * Math.abs(nx) + hy * Math.abs(ny) + hz * Math.abs(nz);
        return Math.min(a, Math.min(b, c)) > radius || Math.max(a, Math.max(b, c)) < -radius;
    }

    private boolean contains(int component, double x, double y, double z) {
        // Non-axis-aligned ray avoids the repeated axial seams of the generated mesh.
        final double ry = 0.3713906763541037D, rz = 0.6947465906068658D;
        int count = 0;
        for (int triangle = starts[component]; triangle < ends[component]; triangle++) {
            int i = triangle * 9;
            double ax = vertices[i], ay = vertices[i + 1], az = vertices[i + 2];
            double ex = vertices[i + 3] - ax, ey = vertices[i + 4] - ay, ez = vertices[i + 5] - az;
            double fx = vertices[i + 6] - ax, fy = vertices[i + 7] - ay, fz = vertices[i + 8] - az;
            double px = ry * fz - rz * fy, py = rz * fx - fz, pz = fy - ry * fx;
            double determinant = ex * px + ey * py + ez * pz;
            if (Math.abs(determinant) < 1.0E-10D) continue;
            double tx = x - ax, ty = y - ay, tz = z - az;
            double u = (tx * px + ty * py + tz * pz) / determinant;
            if (u < 0 || u > 1) continue;
            double qx = ty * ez - tz * ey, qy = tz * ex - tx * ez, qz = tx * ey - ty * ex;
            double v = (qx + ry * qy + rz * qz) / determinant;
            if (v < 0 || u + v > 1) continue;
            double distance = (fx * qx + fy * qy + fz * qz) / determinant;
            if (distance <= 1.0E-8D) continue;
            boolean duplicate = false;
            for (int crossing = 0; crossing < count; crossing++) {
                if (Math.abs(crossings[crossing] - distance) < 1.0E-7D) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                if (count == crossings.length) crossings = Arrays.copyOf(crossings, count * 2);
                crossings[count++] = distance;
            }
        }
        return (count & 1) != 0;
    }

    private static boolean overlaps(double[] data, int i, AxisAlignedBB box) {
        return data[i] <= box.maxX && data[i + 3] >= box.minX
                && data[i + 1] <= box.maxY && data[i + 4] >= box.minY
                && data[i + 2] <= box.maxZ && data[i + 5] >= box.minZ;
    }

    private static void putBounds(double[] data, int i, double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        data[i] = minX; data[i + 1] = minY; data[i + 2] = minZ;
        data[i + 3] = maxX; data[i + 4] = maxY; data[i + 5] = maxZ;
    }
}
