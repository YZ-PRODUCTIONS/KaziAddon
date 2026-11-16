package net.kazi.kazimod.abilities.NagiAwakening;

/** Three-block guard contact with finite beam volumes, including their flat end caps. */
public final class ImagineBreakerNegationGeometry {
    private ImagineBreakerNegationGeometry() {}

    public static boolean withinRadius(double x, double y, double z, double radius) {
        return Double.isFinite(radius) && radius >= 0
                && x * x + y * y + z * z <= radius * radius;
    }

    public static double[] closestCylinderPoint(double px, double py, double pz,
            double sx, double sy, double sz, double ex, double ey, double ez, double radius) {
        if (!Double.isFinite(px) || !Double.isFinite(py) || !Double.isFinite(pz)
                || !Double.isFinite(sx) || !Double.isFinite(sy) || !Double.isFinite(sz)
                || !Double.isFinite(ex) || !Double.isFinite(ey) || !Double.isFinite(ez)
                || !Double.isFinite(radius) || radius < 0) return null;
        double dx = ex - sx, dy = ey - sy, dz = ez - sz;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(length) || length < 1.0E-9) return null;
        dx /= length; dy /= length; dz /= length;
        double along = (px - sx) * dx + (py - sy) * dy + (pz - sz) * dz;
        double rx = px - sx - dx * along, ry = py - sy - dy * along, rz = pz - sz - dz * along;
        double radial = Math.sqrt(rx * rx + ry * ry + rz * rz);
        double scale = radial > radius ? radius / radial : 1;
        double clamped = Math.max(0, Math.min(length, along));
        return new double[] {sx + dx * clamped + rx * scale,
                sy + dy * clamped + ry * scale, sz + dz * clamped + rz * scale};
    }
}
