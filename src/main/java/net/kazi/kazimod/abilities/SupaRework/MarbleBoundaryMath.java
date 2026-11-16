package net.kazi.kazimod.abilities.SupaRework;

/** Segment/sphere exit clipping. Entry is always permitted; fast moves cannot cross the entire dome. */
public final class MarbleBoundaryMath {
    private MarbleBoundaryMath() { }
    public static boolean teleportExits(double startDistanceSquared, double endDistanceSquared, double radius) {
        return radius > 0 && startDistanceSquared < radius*radius && endDistanceSquared >= radius*radius;
    }
    public static double fraction(double x, double y, double z, double dx, double dy, double dz,
                                  double radius, double padding) {
        double a = dx*dx + dy*dy + dz*dz;
        if (a < 1.0E-14 || radius <= padding) return 1;
        double distance2 = x*x + y*y + z*z;
        double inner = radius - padding;
        double b = x*dx + y*dy + z*dz;
        double c = distance2 - inner*inner;
        // Already straddling the wall: allow inward movement, never outward movement.
        if (distance2 < radius*radius && c > 0 && b >= 0) return 0;
        double discriminant = b*b - a*c;
        if (discriminant <= 0) return 1;
        double root = Math.sqrt(discriminant);
        double enter = (-b-root)/a, exit = (-b+root)/a;
        if (exit < 0 || exit > 1 || distance2 >= radius*radius && enter < 0) return 1;
        return Math.max(0, exit - 1.0E-5 / Math.sqrt(a));
    }
}
