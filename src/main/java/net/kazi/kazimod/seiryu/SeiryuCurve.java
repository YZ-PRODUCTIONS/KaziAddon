package net.kazi.kazimod.seiryu;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.vector.Vector3d;

/** Shared cross-sections weld adjacent cuboids without adding geometry or changing UVs. */
public final class SeiryuCurve {
    private final Vector3d[] points, tangents;
    private final double[] distances, headings;
    private final Map<Double, Frame> samples = new HashMap<>();
    private final Frame uprightHead;
    private final double neckTurn, headYaw;
    private final double headBlendLength;
    private static final double MAX_HEAD_VERTICAL = Math.sin(Math.toRadians(80));

    public SeiryuCurve(Vector3d[] points, Vector3d initialSide) {
        this(points, initialSide, null, null);
    }

    public SeiryuCurve(Vector3d[] points, Vector3d initialSide, Vector3d look, Vector3d headRight) {
        this(points, advanceHeadings(points, null, Math.atan2(initialSide.z, initialSide.x)), look, headRight, Double.NaN);
    }

    public SeiryuCurve(Vector3d[] points, double[] headings, Vector3d look, Vector3d headRight, double previousTurn) {
        this(points,headings,look,headRight,previousTurn,1);
    }

    public SeiryuCurve(Vector3d[] points, double[] headings, Vector3d look, Vector3d headRight, double previousTurn, double scale) {
        headBlendLength=3*SeiryuSpine.length(0)*scale;
        this.points = points;
        this.headings = headings.clone();
        tangents = new Vector3d[points.length];
        distances = new double[points.length];
        for (int i = 0; i < points.length; i++) {
            tangents[i] = SeiryuSpine.unit(points[Math.min(i + 1, points.length - 1)]
                    .subtract(points[Math.max(0, i - 1)]), new Vector3d(0, 0, -1));
            if (i > 0) distances[i] = distances[i - 1] + SeiryuSpine.length(i - 1)*scale;
        }
        headYaw = headRight == null ? headings[0] : Math.atan2(headRight.z, headRight.x);
        uprightHead = look == null ? null : upright(points[0], headYaw, -look.y);
        neckTurn = unwrap(unwrap(headings[0], headYaw) - headYaw, previousTurn);
    }

    static double[] advanceHeadings(Vector3d[] points, double[] previous, double fallback) {
        double[] result = new double[points.length];
        for (int i = 0; i < points.length; i++) {
            Vector3d tangent = SeiryuSpine.unit(points[Math.min(i + 1, points.length - 1)]
                    .subtract(points[Math.max(0, i - 1)]), new Vector3d(0, 0, -1));
            double horizontal = Math.sqrt(tangent.x * tangent.x + tangent.z * tangent.z);
            double old = previous == null ? (i == 0 ? fallback : result[i - 1]) : previous[i];
            double target = horizontal < .08 ? old : unwrap(Math.atan2(tangent.x, -tangent.z), old);
            // Heading becomes undefined at a vertical tangent: hold it there and ease out of the pole.
            double weight = Math.max(0, Math.min(1, (horizontal - .08) / .32));
            weight = weight * weight * (3 - 2 * weight);
            double yaw = previous == null ? target : old + Math.max(-.18, Math.min(.18, (target - old) * .4 * weight));
            if (i > 0) {
                double neighbor = unwrap(result[i - 1], yaw);
                yaw = neighbor + Math.max(-.45, Math.min(.45, yaw - neighbor));
            }
            result[i] = unwrap(yaw, old);
        }
        return result;
    }

    public Vector3d firstSide() { return new Vector3d(Math.cos(headings[0]), 0, Math.sin(headings[0])); }
    public double neckTurn() { return neckTurn; }

    private static Frame upright(Vector3d point, double yaw, double vertical) {
        double y = Math.max(-MAX_HEAD_VERTICAL, Math.min(MAX_HEAD_VERTICAL, vertical));
        double horizontal = Math.sqrt(1 - y * y);
        return new Frame(point, new Vector3d(Math.sin(yaw) * horizontal, y, -Math.cos(yaw) * horizontal),
                new Vector3d(Math.cos(yaw), 0, Math.sin(yaw)));
    }

    private static double unwrap(double angle, double previous) {
        if (!Double.isFinite(previous)) return angle;
        return angle + Math.rint((previous - angle) / (2 * Math.PI)) * (2 * Math.PI);
    }

    public Frame sample(double distance) { return samples.computeIfAbsent(distance, this::calculate); }

    public Frame head(double distance) {
        if (uprightHead == null) return sample(distance);
        return new Frame(uprightHead.point.add(uprightHead.tangent.scale(distance)), uprightHead.tangent, uprightHead.side);
    }

    private Frame calculate(double d) {
        if (uprightHead == null || d >= headBlendLength) return body(d);
        if (d <= 0) return head(d);
        Frame body = body(d);
        double t = d / headBlendLength;
        t = t * t * (3 - 2 * t);
        double yaw = unwrap(Math.atan2(body.side.z, body.side.x), headYaw + neckTurn);
        double pitch = Math.asin(uprightHead.tangent.y) * (1 - t) + Math.asin(body.tangent.y) * t;
        // Only the cross-sections turn. The authoritative body path and hitbox centers stay unchanged.
        return upright(body.point, headYaw + (yaw - headYaw) * t, Math.sin(pitch));
    }

    private Frame body(double d) {
        int last = points.length - 1;
        if (d <= 0) return upright(points[0].add(tangents[0].scale(d)), headings[0], tangents[0].y);
        if (d >= distances[last])
            return upright(points[last].add(tangents[last].scale(d - distances[last])), headings[last], tangents[last].y);
        int i = 0;
        while (d > distances[i + 1]) i++;
        double span = distances[i + 1] - distances[i], t = (d - distances[i]) / span;
        double t2 = t * t, t3 = t2 * t;
        Vector3d point = points[i].scale(2 * t3 - 3 * t2 + 1)
                .add(points[i + 1].scale(-2 * t3 + 3 * t2))
                .add(tangents[i].scale((t3 - 2 * t2 + t) * span))
                .add(tangents[i + 1].scale((t3 - t2) * span));
        Vector3d tangent = SeiryuSpine.unit(tangents[i].scale(1 - t).add(tangents[i + 1].scale(t)), tangents[i]);
        double yaw = headings[i] + (unwrap(headings[i + 1], headings[i]) - headings[i]) * t;
        return upright(point, yaw, tangent.y);
    }

    public static final class Frame {
        public final Vector3d point, tangent, side, up;
        Frame(Vector3d point, Vector3d tangent, Vector3d side) {
            this.point = point; this.tangent = tangent; this.side = side; this.up = side.cross(tangent).normalize();
        }
        public Vector3d position(double x, double y) { return point.add(side.scale(x)).subtract(up.scale(y)); }
        public Vector3d normal(double x, double y, double z) {
            return side.scale(x).subtract(up.scale(y)).add(tangent.scale(z));
        }
    }
}
