package net.kazi.kazimod.renderers.entities.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamPattern;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Two merging hand lights and a broad color-cycling cone, matching the Chaos Beam reference. */
public final class ChaosBeamAttackVisuals {
    private static final double TAU = Math.PI * 2D;
    private ChaosBeamAttackVisuals() { }

    public static double cullingRadius() { return ChaosBeamPattern.MAX_RADIUS * 1.12D + ChaosBeamPattern.CORE_RADIUS; }

    public static void render(Sink out, float age, boolean firing, double length, int detail) {
        if (!Float.isFinite(age) || !Double.isFinite(length) || age < 0
                || age >= (firing ? ChaosBeamPattern.FIRE_TICKS : ChaosBeamPattern.CHARGE_TICKS + 5)) return;
        detail = Math.max(0, Math.min(2, detail));
        int sides = detail == 0 ? 24 : detail == 1 ? 16 : 10;
        if (!firing) {
            float merge = ChaosBeamPattern.smooth((age - 12F) / 23F);
            double spread = 1.7D * (1D - merge), y = .55D * (1D - merge);
            float alpha = ChaosBeamPattern.smooth(age / 7F);
            double radius = .30D + .47D * merge;
            for (int side = -1; side <= 1; side += 2) {
                orb(out, side * spread, y, 0, radius, age + side * 2F, alpha * (1 - merge * .4F), sides);
                if (detail < 2) ring(out, side * spread, y, 0, radius * 1.5D,
                        radius * .09D, color(age, side * .05F), alpha * .4F, sides);
            }
            if (merge > 0) orb(out, 0, 0, .1, .45D + merge * (ChaosBeamPattern.CORE_RADIUS - .45D), age, merge * .85F, sides);
            return;
        }
        float fade = ChaosBeamPattern.fade(age);
        if (fade <= 0) return;
        double pulse = .5D + .5D * Math.sin(age * .85D);
        orb(out, 0, 0, 0, ChaosBeamPattern.CORE_RADIUS * (1D + .20D * pulse), age, fade, sides);
        // Concentric luminous rims expand out of the orb every few ticks.
        for (int i = 0; i < (detail == 2 ? 1 : 3); i++) {
            double t = fract(age / 9D + i / 3D);
            ring(out, 0, 0, -.02D - i * .025D, ChaosBeamPattern.CORE_RADIUS * (1.05D + t * .8D),
                    .24D + .36D * (1D - t), color(age, -.12F * i), fade * (float) (1D - t) * .55F, sides);
        }
        length = Math.max(0D, Math.min(ChaosBeamPattern.RANGE, length));
        if (length <= .001D) return;
        int sections = detail == 0 ? 10 : detail == 1 ? 7 : 4;
        int layers = detail == 2 ? 2 : 3;
        for (int layer = 0; layer < layers; layer++) {
            double scale = layer == 0 ? .98D : layer == 1 ? .78D : .48D;
            float alpha = fade * (layer == 0 ? .12F : layer == 1 ? .17F : .23F);
            for (int j = 0; j < sections; j++) {
                double z0 = length * j / sections, z1 = length * (j + 1D) / sections;
                double r0 = ChaosBeamPattern.radiusAt(z0, age) * scale;
                double r1 = ChaosBeamPattern.radiusAt(z1, age) * scale;
                float[] c0 = color(age, (float) (z0 / ChaosBeamPattern.RANGE * .16D + layer * .045D));
                float[] c1 = color(age, (float) (z1 / ChaosBeamPattern.RANGE * .16D + layer * .045D));
                for (int side = 0; side < sides; side++) {
                    double a = TAU * side / sides, b = TAU * (side + 1D) / sides;
                    float edge = alpha * (float) (.82D + .18D * Math.cos(a * 3D + age * .12D));
                    vertex(out, Math.cos(a) * r0, Math.sin(a) * r0, z0, c0, edge);
                    vertex(out, Math.cos(b) * r0, Math.sin(b) * r0, z0, c0, edge);
                    vertex(out, Math.cos(b) * r1, Math.sin(b) * r1, z1, c1, edge);
                    vertex(out, Math.cos(a) * r1, Math.sin(a) * r1, z1, c1, edge);
                }
            }
        }
        // Traveling bands make the widening light feel projected rather than a stationary shell.
        for (int i = 0; i < (detail == 2 ? 2 : 4); i++) {
            double t = fract(age / 14D + i * .25D), z = t * ChaosBeamPattern.RANGE;
            if (z > length) continue;
            double radius = ChaosBeamPattern.radiusAt(z, age);
            ring(out, 0, 0, z, radius, .12D + radius * .014D,
                    color(age, .14F), fade * .22F * (float) Math.sin(Math.PI * t), sides);
        }
    }

    private static void orb(Sink out, double x, double y, double z, double radius, float age, float alpha, int sides) {
        int rows = sides >= 24 ? 8 : sides >= 16 ? 6 : 4;
        float[] color = color(age, 0);
        for (int row = 0; row < rows; row++) {
            double a = Math.PI * row / rows, b = Math.PI * (row + 1D) / rows;
            for (int side = 0; side < sides; side++) {
                double c = TAU * side / sides, d = TAU * (side + 1D) / sides;
                float shine = (float) (.7D + .3D * Math.sin(a));
                float[] tint = {Math.min(1F, color[0] + .14F * shine), Math.min(1F, color[1] + .14F * shine), Math.min(1F, color[2] + .14F * shine)};
                vertex(out, x + Math.sin(a)*Math.cos(c)*radius, y + Math.cos(a)*radius, z + Math.sin(a)*Math.sin(c)*radius, tint, alpha * .8F);
                vertex(out, x + Math.sin(a)*Math.cos(d)*radius, y + Math.cos(a)*radius, z + Math.sin(a)*Math.sin(d)*radius, tint, alpha * .8F);
                vertex(out, x + Math.sin(b)*Math.cos(d)*radius, y + Math.cos(b)*radius, z + Math.sin(b)*Math.sin(d)*radius, tint, alpha * .8F);
                vertex(out, x + Math.sin(b)*Math.cos(c)*radius, y + Math.cos(b)*radius, z + Math.sin(b)*Math.sin(c)*radius, tint, alpha * .8F);
            }
        }
        // A compact sawtooth corona gives the pulsing core the reference's energetic outline.
        for (int side = 0; side < sides; side++) {
            double a = TAU * side / sides + age * .035D, b = a + TAU / sides;
            double outer = radius * (1.19D + .09D * Math.sin(age * .7D + side * 2.3D));
            vertex(out, x + Math.cos(a)*radius, y + Math.sin(a)*radius, z, color, alpha * .3F);
            vertex(out, x + Math.cos((a+b)*.5D)*outer, y + Math.sin((a+b)*.5D)*outer, z, color, alpha * .08F);
            vertex(out, x + Math.cos(b)*radius, y + Math.sin(b)*radius, z, color, alpha * .3F);
            vertex(out, x + Math.cos((a+b)*.5D)*radius*.9D, y + Math.sin((a+b)*.5D)*radius*.9D, z, color, alpha * .3F);
        }
    }

    private static void ring(Sink out, double x, double y, double z, double radius, double width, float[] color, float alpha, int sides) {
        for (int i = 0; i < sides; i++) {
            double a = TAU * i / sides, b = TAU * (i + 1D) / sides;
            vertex(out, x + Math.cos(a)*radius, y + Math.sin(a)*radius, z, color, alpha);
            vertex(out, x + Math.cos(b)*radius, y + Math.sin(b)*radius, z, color, alpha);
            vertex(out, x + Math.cos(b)*(radius-width), y + Math.sin(b)*(radius-width), z, color, alpha);
            vertex(out, x + Math.cos(a)*(radius-width), y + Math.sin(a)*(radius-width), z, color, alpha);
        }
    }

    private static float[] color(float age, float offset) {
        double hue = fract(age / 28D + offset);
        double sector = hue * 6D, f = sector - Math.floor(sector);
        float high = 1F, low = .12F, up = (float) (low + (high-low)*f), down = (float) (high - (high-low)*f);
        switch ((int) sector) {
            case 0: return new float[]{high,up,low}; case 1: return new float[]{down,high,low};
            case 2: return new float[]{low,high,up}; case 3: return new float[]{low,down,high};
            case 4: return new float[]{up,low,high}; default: return new float[]{high,low,down};
        }
    }
    private static double fract(double value) { return value - Math.floor(value); }
    private static void vertex(Sink out, double x, double y, double z, float[] c, float alpha) {
        out.vertex(x,y,z,c[0],c[1],c[2],Math.max(0F,Math.min(1F,alpha)));
    }
}
