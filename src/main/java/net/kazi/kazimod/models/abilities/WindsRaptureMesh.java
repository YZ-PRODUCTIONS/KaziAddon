package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.models.abilities.EaVfxMesh.Sink;

/**
 * Crimson wind coiling around Ea, then tearing forwards as an open, annular beam.
 * Local +Z is the cast direction. Release geometry stays inside a two-block radius
 * and leaves a clear central bore; there are no discs, caps or axial core meshes.
 */
public final class WindsRaptureMesh {
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2;
    private static final int CRIMSON = 0xF31640;
    private static final int MAGENTA = 0xFF3399;
    private static final int PINK = 0xFF9ACF;
    private static final int WHITE = 0xFFF2F7;

    private WindsRaptureMesh() { }

    public static void charge(Sink body, Sink glow, float age, float progress, float opacity) {
        if (!Float.isFinite(age) || !Float.isFinite(progress) || !Float.isFinite(opacity)) return;
        float p = clamp(progress);
        float alpha = clamp(opacity) * smooth(p / .09F);
        if (alpha <= .001F) return;
        EaVfxMesh.weapon(body, glow, 1, age, p, alpha);

        float power = smooth(p);
        float spin = age * (.075F + power * .085F);
        // Loose red wind is drawn into the three turning barrels. Tapered ends and
        // feathered edges prevent these curved sheets from reading as rigid rings.
        for (int strand = 0; strand < 6; strand++) {
            float phase = strand * TAU / 6;
            for (int j = 0; j < 36; j++) {
                float u = j / 36F, v = (j + 1) / 36F;
                float a = phase + u * 7.6F - spin;
                float b = phase + v * 7.6F - spin;
                float r = .30F + (1 - u) * (1.4F + power * .85F);
                float rr = .30F + (1 - v) * (1.4F + power * .85F);
                float z = -.35F + u * 3.75F + sin(a * .7F) * .22F;
                float zz = -.35F + v * 3.75F + sin(b * .7F) * .22F;
                float taper = arch(u), nextTaper = arch(v);
                float pulse = .50F + .50F * sin(age * .24F - u * 11 + strand);
                feather(glow, a, b, r, rr, z, zz,
                        (.06F + .10F * power) * taper,
                        (.06F + .10F * power) * nextTaper,
                        strand % 3 == 0 ? MAGENTA : CRIMSON,
                        alpha * (.25F + power * .40F) * (.65F + pulse * .35F));
                if (strand % 2 == 0) ribbon(glow, a + .012F, b + .012F,
                        r - .015F, rr - .015F, z, zz,
                        .012F * taper, .012F * nextTaper,
                        PINK, alpha * power * pulse * .72F,
                        alpha * power * pulse * .72F);
            }
        }

        // The larger three-dimensional curls echo the reference's loose whirling
        // loops, while the weapon remains readable in the open middle.
        for (int curl = 0; curl < 3; curl++) {
            float activation = smooth((p - .10F - curl * .14F) / .28F);
            float phase = curl * TAU / 3 - spin * .42F;
            for (int j = 0; j < 48; j++) {
                float u = j / 48F, v = (j + 1) / 48F;
                float a = phase + u * TAU * .86F, b = phase + v * TAU * .86F;
                float r = 1.25F + power * .65F + .20F * sin(u * TAU + age * .07F);
                float rr = 1.25F + power * .65F + .20F * sin(v * TAU + age * .07F);
                float z = 1.3F + .95F * sin(a + curl * .8F);
                float zz = 1.3F + .95F * sin(b + curl * .8F);
                float taper = arch(u), nextTaper = arch(v);
                feather(glow, a, b, r, rr, z, zz,
                        .10F * taper, .10F * nextTaper,
                        curl == 1 ? MAGENTA : CRIMSON, alpha * activation * .48F);
                ribbon(glow, a, b, r, rr, z, zz,
                        .009F * taper, .009F * nextTaper,
                        WHITE, alpha * activation * taper * .65F,
                        alpha * activation * nextTaper * .65F);
            }
        }
        // Final compression is a small open crown at the muzzle, not a solid flash.
        float ignition = smooth((p - .68F) / .32F);
        if (ignition > .001F) front(glow, 2.96F, .77F, .065F,
                spin, .035F, 3.4F, alpha * ignition * .76F, 48);
    }

    public static void release(Sink glow, float age, float length, float opacity) {
        if (!Float.isFinite(age) || !Float.isFinite(length) || !Float.isFinite(opacity)) return;
        float alpha = clamp(opacity);
        float reach = Math.max(0, Math.min(40, length));
        if (alpha <= .001F || reach <= .001F) return;
        float opening = .45F + .55F * smooth(age / 3F);
        float shown = alpha * opening;
        int steps = Math.max(12, (int) Math.ceil(reach * 2));

        // Four scarlet sheets rotate around an empty bore. Their widths breathe,
        // and their broad transparent edges blend into the world instead of
        // producing the appearance of a solid laser tube.
        for (int strand = 0; strand < 4; strand++) {
            float phase = strand * TAU / 4 - age * .23F;
            for (int j = 0; j < steps; j++) {
                float z = reach * j / steps, zz = reach * (j + 1) / steps;
                float a = phase + z * .66F + .11F * sin(z * 1.1F - age * .12F);
                float b = phase + zz * .66F + .11F * sin(zz * 1.1F - age * .12F);
                float r = shellRadius(z, age), rr = shellRadius(zz, age);
                float pulse = .60F + .40F * sin(z * 1.05F - age * .53F + strand);
                float width = (.12F + pulse * .09F) * endTaper(z, reach);
                float nextWidth = (.12F + pulse * .09F) * endTaper(zz, reach);
                feather(glow, a, b, r, rr, z, zz, width, nextWidth,
                        strand % 2 == 0 ? CRIMSON : MAGENTA, shown * (.28F + pulse * .16F));
                ribbon(glow, a + width * .32F, b + nextWidth * .32F,
                        r - .015F, rr - .015F, z, zz,
                        .018F * endTaper(z, reach), .018F * endTaper(zz, reach),
                        strand == 1 ? WHITE : PINK,
                        shown * (.48F + pulse * .23F), shown * (.48F + pulse * .23F));
            }
        }

        // Fine opposite-moving strands create turbulent shear without filling in
        // the central tunnel. All surfaces remain outside its .85-block bore.
        for (int strand = 0; strand < 2; strand++) {
            float phase = strand * PI + age * .18F;
            for (int j = 0; j < steps; j++) {
                float z = reach * j / steps, zz = reach * (j + 1) / steps;
                float a = phase - z * .86F, b = phase - zz * .86F;
                float r = 1.05F + .10F * sin(z * .6F - age * .2F);
                float rr = 1.05F + .10F * sin(zz * .6F - age * .2F);
                feather(glow, a, b, r, rr, z, zz,
                        .040F * endTaper(z, reach), .040F * endTaper(zz, reach),
                        MAGENTA, shown * .39F);
                ribbon(glow, a, b, r, rr, z, zz, .011F, .011F,
                        WHITE, shown * .58F, shown * .58F);
            }
        }

        // Successive hollow pressure fronts travel down the complete 40-block
        // beam at a constant speed. The foremost crown follows its growing end.
        for (int burst = 0; burst < 7; burst++) {
            float z = positiveMod(age * 3.4F - burst * 6.15F, 43.05F);
            if (z > reach) continue;
            float edge = smooth(z / 1.25F) * (.45F + .55F * smooth((reach - z) / .8F));
            float radius = 1.42F + .12F * sin(age * .15F + burst);
            front(glow, z, radius, .10F, -age * .09F + burst * .72F,
                    .16F, reach, shown * edge * .75F, 56);
            // A red wake behind each hot front supplies depth and directional motion.
            float wakeZ = Math.max(0, z - .38F);
            annulus(glow, wakeZ, radius + .05F, radius + .23F,
                    age * .1F + burst, .05F, reach, CRIMSON,
                    shown * edge * .12F, 0, 40);
        }
        front(glow, reach, 1.50F + .07F * sin(age * .7F), .115F,
                age * .13F, .11F, reach, shown * .94F, 64);

        // Thin, short wind streaks peel around the outside; these too are bounded
        // by the gameplay radius and terminate before the beam's end.
        for (int streak = 0; streak < 15; streak++) {
            float z = positiveMod(streak * 2.87F + age * 3.9F, 43.05F);
            if (z >= reach) continue;
            float span = Math.min(1.1F + (streak % 3) * .36F, reach - z);
            float phase = streak * 2.39996F - age * .14F;
            for (int j = 0; j < 4; j++) {
                float u = j * .25F, v = (j + 1) * .25F;
                float a = phase + u * .42F, b = phase + v * .42F;
                float r = 1.78F + .13F * u, rr = 1.78F + .13F * v;
                ribbon(glow, a, b, r, rr, z + span * u, z + span * v,
                        .018F * arch(u), .018F * arch(v),
                        streak % 3 == 0 ? WHITE : PINK,
                        shown * arch(u) * .58F, shown * arch(v) * .58F);
            }
        }
    }

    private static void front(Sink out, float z, float radius, float width, float phase,
                              float warp, float length, float alpha, int steps) {
        // Radially graded annuli have no central triangles. Three torn brighter
        // crescents move around each front rather than fixed segmented geometry.
        annulus(out, z, radius - width * 2.4F, radius - width * .45F,
                phase, warp, length, CRIMSON, 0, alpha * .22F, steps);
        annulus(out, z, radius - width * .45F, radius,
                phase, warp, length, MAGENTA, alpha * .27F, alpha * .82F, steps);
        annulus(out, z, radius, radius + width * .34F,
                phase, warp, length, WHITE, alpha * .92F, alpha * .48F, steps);
        annulus(out, z, radius + width * .34F, radius + width * 2.2F,
                phase, warp, length, MAGENTA, alpha * .33F, 0, steps);
    }

    private static void annulus(Sink out, float z, float inner, float outer, float phase,
                                float warp, float length, int color, float innerAlpha,
                                float outerAlpha, int steps) {
        for (int j = 0; j < steps; j++) {
            float a = j * TAU / steps + phase, b = (j + 1) * TAU / steps + phase;
            float pulse = .35F + .65F * smooth((sin(a * 3 - phase * 2.3F) + 1) * .5F);
            float nextPulse = .35F + .65F * smooth((sin(b * 3 - phase * 2.3F) + 1) * .5F);
            float r = .030F * sin(a * 5 + phase), rr = .030F * sin(b * 5 + phase);
            float za = Math.max(0, Math.min(length, z + sin(a * 2 + phase) * warp));
            float zb = Math.max(0, Math.min(length, z + sin(b * 2 + phase) * warp));
            polar(out, a, inner + r, za, color, innerAlpha * pulse);
            polar(out, b, inner + rr, zb, color, innerAlpha * nextPulse);
            polar(out, b, outer + rr, zb, color, outerAlpha * nextPulse);
            polar(out, a, outer + r, za, color, outerAlpha * pulse);
        }
    }

    private static void feather(Sink out, float a, float b, float r, float rr,
                                float z, float zz, float width, float nextWidth,
                                int color, float alpha) {
        // Alpha gradients across each ribbon hide polygon boundaries without textures.
        polar(out, a - width, r, z, color, 0);
        polar(out, b - nextWidth, rr, zz, color, 0);
        polar(out, b, rr, zz, color, alpha);
        polar(out, a, r, z, color, alpha);
        polar(out, a, r, z, color, alpha);
        polar(out, b, rr, zz, color, alpha);
        polar(out, b + nextWidth, rr, zz, color, 0);
        polar(out, a + width, r, z, color, 0);
    }

    private static void ribbon(Sink out, float a, float b, float r, float rr,
                               float z, float zz, float width, float nextWidth,
                               int color, float alpha, float nextAlpha) {
        polar(out, a - width, r, z, color, alpha);
        polar(out, b - nextWidth, rr, zz, color, nextAlpha);
        polar(out, b + nextWidth, rr, zz, color, nextAlpha);
        polar(out, a + width, r, z, color, alpha);
    }

    private static void polar(Sink out, float angle, float radius, float z, int color, float alpha) {
        out.vertex(cos(angle) * radius, sin(angle) * radius, z, color, alpha);
    }

    private static float shellRadius(float z, float age) {
        return 1.42F + .15F * sin(z * .73F - age * .31F);
    }

    private static float endTaper(float z, float length) {
        return .28F + .72F * smooth(Math.min(z, length - z) / .85F);
    }

    private static float positiveMod(float value, float period) {
        return value - (float) Math.floor(value / period) * period;
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float arch(float value) { return Math.max(0, sin(PI * value)); }
    private static float smooth(float value) { value = clamp(value); return value * value * (3 - 2 * value); }
    private static float sin(float value) { return (float) Math.sin(value); }
    private static float cos(float value) { return (float) Math.cos(value); }
}
