package net.kazi.kazimod.client.nagi;

/** A small spectral dragon head, with the neck at the origin and the muzzle along +Z. */
public final class InnerDragonsArmMesh {
    public interface Sink { void vertex(float x, float y, float z, int color, float alpha); }
    private static final float[] CX = {1, .72F, 0, -.72F, -1, -.72F, 0, .72F};
    private static final float[] CY = {0, .72F, 1, .72F, 0, -.72F, -1, -.72F};
    private static final float[][] SKULL = {
            {-.10F, 0, .075F, .075F}, {.08F, .015F, .16F, .135F},
            {.29F, .055F, .245F, .18F}, {.52F, .03F, .205F, .135F},
            {.79F, .005F, .14F, .075F}, {.96F, .005F, .115F, .065F}};
    private static final float[][] JAW = {
            {.20F, -.10F, .14F, .045F}, {.45F, -.13F, .18F, .045F},
            {.79F, -.13F, .13F, .035F}, {.94F, -.11F, .10F, .025F}};
    private static final float[][] HORN = {{.17F, .17F, .22F, .060F}, {.24F, .30F, .15F, .050F},
            {.29F, .42F, .015F, .030F}, {.32F, .50F, -.17F, 0}};
    private InnerDragonsArmMesh() { }

    public static void render(Sink out, float age, float attack, float opacity) {
        if (!Float.isFinite(age) || !Float.isFinite(attack) || !Float.isFinite(opacity) || opacity <= 0) return;
        opacity = Math.min(1, opacity);
        attack = Math.max(0, Math.min(1, attack));
        float breath = (float) Math.sin(age * .075F);
        float jawOpen = .018F + .012F * (breath + 1) + attack * .10F;
        loft(out, SKULL, 0, .49F * opacity);
        loft(out, JAW, jawOpen, .45F * opacity);
        for (int side = -1; side <= 1; side += 2) {
            // Swept horns, cheek spines and a bright raised brow distinguish the silhouette.
            horn(out, side, opacity);
            spike(out, side * .20F, -.015F, .28F, side * .36F, .07F, .08F,
                    .065F, 0xEAF4FA, .50F * opacity);
            spike(out, side * .17F, -.07F, .22F, side * .28F, -.02F, -.04F,
                    .035F, 0xF4FAFF, .40F * opacity);
            quad(out, side * .229F, .11F, .29F, side * .232F, .11F, .43F,
                    side * .201F, .065F, .51F, side * .222F, .061F, .33F,
                    0x90A9BA, .58F * opacity);
            quad(out, side * .236F, .098F, .32F, side * .235F, .099F, .418F,
                    side * .209F, .071F, .475F, side * .228F, .073F, .34F,
                    0xFFFFFF, .88F * opacity);
            spike(out, side * .205F, .15F, .29F, side * .175F, .16F, .55F,
                    .028F, 0xFFFFFF, .66F * opacity);
            for (int tooth = 0; tooth < 3; tooth++) {
                float z = .48F + tooth * .16F;
                float x = side * (.17F - tooth * .024F);
                spike(out, x, -.055F, z, x * .94F, -.135F, z + .015F,
                        .021F, 0xFFFFFF, .67F * opacity);
                spike(out, x * .93F, -.115F - jawOpen * (z - .2F), z + .06F,
                        x * .91F, -.066F - jawOpen * (z - .2F), z + .048F,
                        .017F, 0xEAF5FF, .54F * opacity);
            }
            // A short shoulder plume blends the head into the missing limb.
            spike(out, side * .06F, -.005F, -.05F,
                    side * (.075F + breath * .012F), .045F, -.26F,
                    .048F, 0xFFFFFF, .15F * opacity);
        }
        for (int ridge = 0; ridge < 3; ridge++) {
            float z = .12F + ridge * .14F;
            spike(out, 0, .17F, z, 0, .29F - ridge * .025F, z - .09F,
                    .052F - ridge * .006F, 0xF4FAFF, .55F * opacity);
        }
    }

    private static void loft(Sink out, float[][] rings, float opening, float alpha) {
        for (int section = 0; section < rings.length - 1; section++) {
            float[] a = rings[section], b = rings[section + 1];
            for (int face = 0; face < 8; face++) {
                int next = (face + 1) & 7;
                int color = face < 4 ? (face == 1 || face == 2 ? 0xFFFFFF : 0xE8F1F8) : 0xB9CEDD;
                quad(out, CX[face] * a[2], a[1] + CY[face] * a[3] - opening * (a[0] - .2F), a[0],
                        CX[face] * b[2], b[1] + CY[face] * b[3] - opening * (b[0] - .2F), b[0],
                        CX[next] * b[2], b[1] + CY[next] * b[3] - opening * (b[0] - .2F), b[0],
                        CX[next] * a[2], a[1] + CY[next] * a[3] - opening * (a[0] - .2F), a[0], color, alpha);
            }
        }
        float[] nose = rings[rings.length - 1];
        for (int face = 0; face < 8; face++) {
            int next = (face + 1) & 7;
            float y = nose[1] - opening * (nose[0] - .2F);
            quad(out, 0, y, nose[0], CX[face] * nose[2], y + CY[face] * nose[3], nose[0],
                    CX[next] * nose[2], y + CY[next] * nose[3], nose[0], 0, y, nose[0], 0xF0F6FB, alpha);
        }
    }

    private static void horn(Sink out, int side, float opacity) {
        for (int part = 0; part < HORN.length - 1; part++) {
            float[] a = HORN[part], b = HORN[part + 1];
            for (int face = 0; face < 4; face++) {
                double angle = face * Math.PI * .5, next = angle + Math.PI * .5;
                float ax = (float) Math.cos(angle), az = (float) Math.sin(angle);
                float bx = (float) Math.cos(next), bz = (float) Math.sin(next);
                quad(out, side * a[0] + ax * a[3], a[1], a[2] + az * a[3],
                        side * b[0] + ax * b[3], b[1], b[2] + az * b[3],
                        side * b[0] + bx * b[3], b[1], b[2] + bz * b[3],
                        side * a[0] + bx * a[3], a[1], a[2] + bz * a[3],
                        face < 2 ? 0xFFFFFF : 0xCFDFEB, .54F * opacity);
            }
        }
    }

    private static void spike(Sink out, float x, float y, float z, float tx, float ty, float tz,
                              float width, int color, float alpha) {
        quad(out, x - width, y, z, x, y, z - width, tx, ty, tz, tx, ty, tz, color, alpha);
        quad(out, x, y, z - width, x + width, y, z, tx, ty, tz, tx, ty, tz, color, alpha);
        quad(out, x + width, y, z, x, y, z + width, tx, ty, tz, tx, ty, tz, color, alpha);
        quad(out, x, y, z + width, x - width, y, z, tx, ty, tz, tx, ty, tz, color, alpha);
    }

    private static void quad(Sink out, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int color, float alpha) {
        out.vertex(ax, ay, az, color, alpha); out.vertex(bx, by, bz, color, alpha);
        out.vertex(cx, cy, cz, color, alpha); out.vertex(dx, dy, dz, color, alpha);
    }
}
