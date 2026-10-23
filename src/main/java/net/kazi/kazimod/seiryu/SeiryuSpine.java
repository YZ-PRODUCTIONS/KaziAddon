package net.kazi.kazimod.seiryu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

/** Distance-sampled head history with bounded curvature and angular velocity. */
public final class SeiryuSpine {
    public static final int SEGMENTS = 24;
    public static final double MODEL_SCALE = 3.75;
    public static final double UNIT = MODEL_SCALE / 16;
    public static final double HEIGHT = MODEL_SCALE * (1.501 - 13.5 / 16);
    public static final double MAX_BEND = .30;
    private static final double HISTORY_LENGTH = 160;
    private final List<Vector3d> trail = new ArrayList<>();
    private Vector3d[] points;
    private Vector3d heading, previousHead;
    private double phase;
    private final double scale;

    public SeiryuSpine(){this(1);}
    public SeiryuSpine(double scale){this.scale=scale;}

    public static double length(int segment) { return (segment < 10 ? 25 : 22) * UNIT; }
    public static double radius(int segment) { return (segment < 10 ? 7.5 : 7) * UNIT; }

    public Vector3d[] update(Vector3d head, Vector3d forward) {
        Vector3d[] result=updateNormalized(head.scale(1/scale),forward);
        for(int i=0;i<result.length;i++)result[i]=result[i].scale(scale);
        return result;
    }

    private Vector3d[] updateNormalized(Vector3d head, Vector3d forward) {
        Vector3d direction = unit(forward, new Vector3d(0, 0, 1));
        boolean reset = previousHead == null || previousHead.distanceToSqr(head) > 1024;
        heading = reset ? direction : turn(heading, direction, .12);
        Vector3d neck = head.add(0, HEIGHT, 0).subtract(heading.scale(15 * UNIT));
        if (reset) {
            trail.clear();
            for (int i = 0; i <= 320; i++) trail.add(neck.subtract(heading.scale(i * .5)));
            points = null;
        } else {
            // Keep a moving tip and fixed-distance knots; idle updates do not consume history.
            Vector3d fixed = trail.get(1);
            double distance = fixed.distanceTo(neck);
            if (distance >= .5) {
                int steps = (int)Math.floor(distance / .5);
                for (int i = 1; i <= steps; i++)
                    trail.add(1, fixed.add(neck.subtract(fixed).scale(i * .5 / distance)));
            }
            trail.set(0, neck);
            double total = 0;
            for (int i = 1; i < trail.size(); i++) {
                total += trail.get(i).distanceTo(trail.get(i - 1));
                if (total > HISTORY_LENGTH) { trail.subList(i + 1, trail.size()).clear(); break; }
            }
        }
        previousHead = head;
        phase += .075;
        Vector3d[] next = new Vector3d[SEGMENTS + 1];
        next[0] = neck;
        Vector3d previousDirection = heading.scale(-1);
        double distance = 0;
        for (int i = 1; i < next.length; i++) {
            distance += length(i - 1);
            Vector3d target = sample(distance).scale(.5)
                    .add(sample(Math.max(0, distance - 3)).scale(.25))
                    .add(sample(distance + 3).scale(.25));
            double envelope = Math.min(1, distance / 18);
            // World-space waves avoid the old side/up sign flip during vertical flight.
            target = target.add(0, Math.sin(phase - distance * .075) * 1.4 * envelope, 0);
            Vector3d wanted = unit(target.subtract(next[i - 1]), previousDirection);
            if (points != null)
                wanted = turn(unit(points[i].subtract(points[i - 1]), previousDirection), wanted, .085);
            wanted = turn(previousDirection, wanted, MAX_BEND);
            next[i] = next[i - 1].add(wanted.scale(length(i - 1)));
            previousDirection = wanted;
        }
        points = next;
        return next.clone();
    }

    static Vector3d unit(Vector3d value, Vector3d fallback) {
        return value.lengthSqr() < 1.0e-10 ? fallback : value.normalize();
    }

    static Vector3d turn(Vector3d from, Vector3d to, double maxAngle) {
        double dot = Math.max(-1, Math.min(1, from.dot(to)));
        double angle = Math.acos(dot);
        if (angle <= maxAngle) return to;
        Vector3d tangent = to.subtract(from.scale(dot));
        if (tangent.lengthSqr() < 1.0e-10)
            tangent = from.cross(Math.abs(from.y) < .9 ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0));
        return from.scale(Math.cos(maxAngle)).add(tangent.normalize().scale(Math.sin(maxAngle)));
    }

    private Vector3d sample(double distance) {
        for (int i = 1; i < trail.size(); i++) {
            Vector3d a = trail.get(i - 1), b = trail.get(i);
            double span = a.distanceTo(b);
            if (span > 0 && distance <= span) return a.add(b.subtract(a).scale(distance / span));
            distance -= span;
        }
        int n = trail.size();
        return trail.get(n - 1).add(unit(trail.get(n - 1).subtract(trail.get(n - 2)), heading.scale(-1)).scale(distance));
    }

    public static AxisAlignedBB bounds(Vector3d[] points, int index) {
        return bounds(points,index,1);
    }
    public static AxisAlignedBB bounds(Vector3d[] points, int index, double scale) {
        return new AxisAlignedBB(points[index], points[index + 1]).inflate(radius(index)*scale + .2);
    }
}
