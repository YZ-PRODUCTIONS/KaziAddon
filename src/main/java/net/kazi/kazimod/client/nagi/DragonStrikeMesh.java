package net.kazi.kazimod.client.nagi;

import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;

/** One low-poly, independently flying dragon per entity, with kind-specific silhouettes. */
public final class DragonStrikeMesh {
    public interface Sink { void vertex(double x, double y, double z, int rgb, float alpha); }
    private DragonStrikeMesh() {}

    public static void render(Sink out, DragonKind kind, double age, float attack, int detail) {
        if (kind == null || !Double.isFinite(age) || age < 0 || !Float.isFinite(attack)) return;
        float alpha = (float) Math.min(1, age / 8);
        if (alpha <= 0) return;
        int segments = detail == 0 ? 12 : detail == 1 ? 8 : 5;
        int sides = detail == 0 ? 7 : 5;
        for (int segment = 0; segment < segments; segment++) {
            double t0 = segment / (double) segments, t1 = (segment + 1D) / segments;
            double[] p0 = center(t0, age, kind.ordinal()), p1 = center(t1, age, kind.ordinal());
            double r0 = .025 + .31 * t0, r1 = .025 + .31 * t1;
            for (int side = 0; side < sides; side++) {
                double a = side * Math.PI * 2 / sides, b = (side + 1) * Math.PI * 2 / sides;
                int color = shade(kind.color, .6 + .4 * (Math.sin(a) + 1) * .5);
                ringVertex(out, p0, a, r0, color, alpha * .92F);
                ringVertex(out, p0, b, r0, color, alpha * .92F);
                ringVertex(out, p1, b, r1, color, alpha * .92F);
                ringVertex(out, p1, a, r1, color, alpha * .92F);
            }
        }
        Sink local = (x,y,z,c,a) -> out.vertex(x, y + .55, z + .25, c, a * alpha);
        head(local, kind, age, Math.max(0, Math.min(1, attack)));
        if (kind == DragonKind.COBRA) {
            // Flared hood behind the single-eyed head.
            for (int side : new int[]{-1,1}) {
                spike(local, side * .32, -.25, -.45, side * 1.0, .65, -.75, .35, kind.color, .95F);
                box(local, side < 0 ? -.85 : .65, -.1, -.5, side < 0 ? -.65 : .85,
                        .35, -.45, 0xD7E4A7, .95F);
            }
        }
        if (kind == DragonKind.ICE_CRYSTAL || kind == DragonKind.CORPSE_FLAME) {
            for (int i = 0; i < 5; i++) {
                double z = -2.6 + i * .48;
                double height = kind == DragonKind.ICE_CRYSTAL ? .65 : .55 + .18 * Math.sin(age * .3 + i);
                spike(local, 0, .1, z, .12 * Math.sin(i), height, z - .28, .19,
                        kind == DragonKind.ICE_CRYSTAL ? 0xDAF5FF : 0xFFDD75, .85F);
            }
        }
        if (kind == DragonKind.ANGEL) {
            for (int side : new int[]{-1,1}) for (int feather = 0; feather < 5; feather++) {
                double flap = Math.sin(age * .14) * .3;
                spike(local, side * .2, .05, -.6 - feather * .16,
                        side * (2.4 - feather * .17), .8 + flap - feather * .1,
                        -1.0 - feather * .30, .18, 0xFFF4EB, .9F);
            }
        }
    }

    private static double[] center(double t, double age, int variant) {
        double wave = Math.sin(t * Math.PI * 2 - age * .18 + variant) * .32 * (1 - t);
        return new double[]{wave, .55 + Math.cos(t * Math.PI * 2 - age * .13) * .18 * (1 - t),
                -3.4 + t * 3.5};
    }

    private static void head(Sink out, DragonKind kind, double age, float attack) {
        int color = kind.color;
        box(out, -.42, -.14, -.45, .42, .38, .22, color, .96F);
        box(out, -.31, -.06, .15, .31, .16, .85, shade(color, .9), .96F);
        double jaw = -.24 - .28 * attack - .025 * Math.sin(age * .2);
        box(out, -.30, jaw - .13, -.05, .30, jaw, .82, shade(color, .73), .95F);
        box(out, -.27, jaw + .015, .12, .27, jaw + .025, .77, 0x382B45, .9F);
        if (kind == DragonKind.COBRA) {
            box(out, -.095, .38, -.05, .095, .40, .15, 0xFF9944, 1);
        }
        for (int side : new int[]{-1,1}) {
            if (kind != DragonKind.BLIND && kind != DragonKind.COBRA) {
                box(out, side < 0 ? -.435 : .415, .1, -.02, side < 0 ? -.415 : .435, .22, .23, 0xFFBA55, 1);
                if (kind == DragonKind.FOUR_EYED)
                    box(out, side < 0 ? -.435 : .415, .24, -.29, side < 0 ? -.415 : .435, .34, -.08, 0xEB758D, 1);
            }
            if (kind != DragonKind.SPEARHEAD)
                spike(out, side * .3, .32, -.24, side * .55, .95, -.8, .16,
                        kind == DragonKind.CORPSE_FLAME ? 0xFFE08B : shade(color, 1.15), .98F);
            spike(out, side * .38, .05, -.35, side * .80, .25, -.68, .17, color, .95F);
            for (int tooth = 0; tooth < 3; tooth++) {
                double z = .23 + tooth * .24;
                spike(out, side * .24, -.055, z, side * .20, -.25 - attack * .08, z + .06, .065, 0xFFF4CC, 1);
            }
        }
        if (kind == DragonKind.SPEARHEAD)
            spike(out, 0, .27, -.02, 0, .33, 1.7, .28, 0xFFF0A0, .98F);
        else
            spike(out, 0, .34, -.15, 0, .68, -.55, .18, shade(color, 1.1), .98F);
    }

    private static void ringVertex(Sink out, double[] p, double angle, double radius, int color, float alpha) {
        out.vertex(p[0] + Math.cos(angle) * radius, p[1] + Math.sin(angle) * radius, p[2], color, alpha);
    }
    private static int shade(int color, double amount) {
        return Math.min(255, (int) ((color >> 16 & 255) * amount)) << 16
                | Math.min(255, (int) ((color >> 8 & 255) * amount)) << 8
                | Math.min(255, (int) ((color & 255) * amount));
    }
    private static void spike(Sink out, double x, double y, double z, double tx, double ty, double tz,
                              double radius, int color, float alpha) {
        for (int side = 0; side < 4; side++) {
            double a = side * Math.PI / 2, b = (side + 1) * Math.PI / 2;
            out.vertex(x + Math.cos(a) * radius, y, z + Math.sin(a) * radius, color, alpha);
            out.vertex(x + Math.cos(b) * radius, y, z + Math.sin(b) * radius, color, alpha);
            out.vertex(tx, ty, tz, color, alpha);
            out.vertex(tx, ty, tz, color, alpha);
        }
    }
    private static void box(Sink out, double x0, double y0, double z0, double x1, double y1, double z1,
                            int color, float alpha) {
        double[][] points = {{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},
                {x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] faces = {{0,1,2,3},{4,7,6,5},{0,3,7,4},{1,5,6,2},{0,4,5,1},{3,2,6,7}};
        for (int[] face : faces) for (int vertex : face) {
            double[] p = points[vertex];
            out.vertex(p[0], p[1], p[2], color, alpha);
        }
    }
}
