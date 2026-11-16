package net.kazi.kazimod.blood;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Deterministic, bounded meshes shared by world and first-person blood effects. */
final class BloodEffects {
    private static final double TAU = Math.PI * 2;
    private static final Vector3d[] NOVA = {
            new Vector3d(-.82, .26, -.16), new Vector3d(.62, .48, .22),
            new Vector3d(-.38, -.54, .12), new Vector3d(.93, -.31, -.28),
            new Vector3d(.06, .85, -.24), new Vector3d(.22, -.03, .55)
    };

    static void convergence(IVertexBuilder out, Matrix4f m, Vector3d center, Vector3d dir,
                            float age, float duration, float alpha) {
        float gather = BloodMesh.smooth(age / 7), squeeze = BloodMesh.smooth((age - 7) / (duration - 9));
        double radius = (.19 - .125 * squeeze) * gather;
        Vector3d u = BloodMesh.side(dir), v = dir.cross(u);
        BloodMesh.drop(out, m, center, dir, radius, radius * (.82 + .18 * Math.sin(age * .5)), .2F, alpha);
        for (int i = 0; i < 6; i++) {
            double a = i * TAU / 6 + age * .08;
            double t = (age * .08 + i / 6.0) % 1;
            double reach = (.52 - .25 * squeeze) * (1 - t);
            Vector3d axis = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
            Vector3d from = center.add(axis.scale(reach)).subtract(dir.scale(.05 + .12 * (1 - t)));
            Vector3d control = center.add(u.scale(Math.cos(a + .8) * reach * .65))
                    .add(v.scale(Math.sin(a + .8) * reach * .65));
            BloodMesh.filament(out, m, from, control, center, .014 * (1 - t), alpha * .8F, .65F);
            BloodMesh.drop(out, m, from, center.subtract(from), .022 * (1 - t), .06 * (1 - t), 1, alpha);
        }
        if (squeeze > .6) {
            BloodMesh.airArc(out, m, center, dir, radius + .035, .006,
                    alpha * (squeeze - .6F), age * .3, Math.PI * 1.2);
        }
    }

    static void piercing(IVertexBuilder out, Matrix4f m, Vector3d start, Vector3d end,
                         Vector3d direction, float age, float stopped, boolean impact) {
        Vector3d delta = end.subtract(start);
        double length = delta.length();
        if (length < .01 || delta.dot(direction) <= .01) {
            if (impact) splash(out, m, end, direction, stopped, 1);
            return;
        }
        Vector3d dir = delta.normalize(), u = BloodMesh.side(dir), v = dir.cross(u);
        float fade = impact ? 1 - BloodMesh.smooth(stopped / 9) : 1;
        double retract = impact ? BloodMesh.smooth(stopped / 8) : 0;
        Vector3d root = start.add(delta.scale(retract));
        // A dense liquid center with a red edge, not an additive laser or a rigid cylinder.
        BloodMesh.tube(out, m, root, end, .047 * fade, .033 * fade, fade, .34F);
        int segments = MathHelper.clamp((int)Math.ceil(length * 3), 8, 100);
        for (int strand = 0; strand < 3; strand++) {
            Vector3d previous = root;
            for (int j = 1; j <= segments; j++) {
                double t = retract + (1 - retract) * j / segments;
                double angle = length * t * 2.5 - age * 2.8 + strand * TAU / 3;
                double offset = (.036 + .01 * Math.sin(t * length * 4 - age * 2)) * fade;
                Vector3d next = start.add(delta.scale(t)).add(u.scale(Math.cos(angle) * offset))
                        .add(v.scale(Math.sin(angle) * offset));
                BloodMesh.tube(out, m, previous, next, .014 * fade, .011 * fade, fade,
                        strand == 0 ? 1.13F : .83F);
                previous = next;
            }
        }
        if (!impact) {
            BloodMesh.drop(out, m, end.subtract(dir.scale(.12)), dir, .062, .24, .8F, 1);
            for (int i = 0; i < 2; i++) {
                double t = ((age * 1.2 + i * 3.4) % 7.5) + .2;
                if (t > length) continue;
                double radius = .09 + t * .055;
                BloodMesh.airArc(out, m, start.add(dir.scale(t)), dir, radius, .014,
                        (float)(.32 * (1 - t / 8)), i * 2.7 + age * .1, Math.PI * 1.45);
            }
            for (int i = 0; i < 7; i++) {
                double t = ((age * .37 + i * .147) % 1) * length;
                double angle = i * 2.4 + age * .09;
                Vector3d at = start.add(dir.scale(t)).add(u.scale(Math.cos(angle) * .09))
                        .add(v.scale(Math.sin(angle) * .09));
                BloodMesh.tube(out, m, at.subtract(dir.scale(.22)), at, .013, 0, .6F, .7F);
            }
        }
        if (age < 5) release(out, m, start, direction, age, 1);
        if (impact) splash(out, m, end, direction, stopped, 1);
    }

    static void release(IVertexBuilder out, Matrix4f m, Vector3d c, Vector3d dir, float age, float alpha) {
        float fade = (1 - BloodMesh.smooth(age / 5)) * alpha;
        Vector3d u = BloodMesh.side(dir), v = dir.cross(u);
        for (int i = 0; i < 5; i++) {
            double angle = i * TAU / 5;
            Vector3d radial = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            Vector3d end = c.add(radial.scale(.07 + age * .015)).add(dir.scale(.14 + age * .06));
            BloodMesh.filament(out, m, c, end.add(radial.scale(.025)), end, .012 * fade, fade, .9F);
        }
        BloodMesh.airArc(out, m, c.add(dir.scale(age * .22)), dir, .12 + age * .07,
                .02, fade * .4F, age * .15, Math.PI * 1.7);
    }

    static void supernova(IVertexBuilder out, Matrix4f m, Vector3d c, Vector3d dir,
                          float age, int phase, float phaseAge) {
        Vector3d u = BloodMesh.side(dir), v = dir.cross(u);
        float form = BloodMesh.smooth(age / 5);
        float frozenAge = phase == 2 ? age - phaseAge : age;
        for (int i = 0; i < NOVA.length; i++) {
            Vector3d offset = NOVA[i];
            double quiver = Math.sin(frozenAge * .22 + i * 2.1) * .025;
            Vector3d orb = c.add(u.scale(offset.x * form)).add(v.scale(offset.y * form + quiver))
                    .add(dir.scale(offset.z * form));
            double radius = (.105 + (i % 3) * .012) * form;
            if (phase < 2) {
                double stretch = phase == 1 ? 1.6 : 1 + Math.sin(age * .6 + i) * .06;
                BloodMesh.drop(out, m, orb, dir.scale(-1), radius, radius * stretch, phase == 1 ? 1 : .1F, 1);
                if (phase == 1) {
                    Vector3d tail = orb.subtract(dir.scale(.65 + i * .07));
                    BloodMesh.filament(out, m, orb, tail.add(v.scale(.04)), tail, radius * .27, .85F, .7F);
                    BloodMesh.drop(out, m, tail, dir, .022, .075, 1, .55F);
                } else if (age < 6) {
                    BloodMesh.filament(out, m, c.subtract(dir.scale(.9)), orb.subtract(dir.scale(.45)), orb,
                            .015 * (1 - form), .75F, .6F);
                }
            } else {
                novaBurst(out, m, c, orb, i, radius, phaseAge);
            }
        }
    }

    private static void novaBurst(IVertexBuilder out, Matrix4f m, Vector3d center, Vector3d orb,
                                  int seed, double radius, float age) {
        if (age < 1.4) BloodMesh.orb(out, m, orb, radius * (1 + age * .4), 1 - age / 1.4F);
        float fade = 1 - BloodMesh.smooth((age - 2) / 8);
        if (fade < .005) return;
        // Spherical buckshot from each compressed drop. One visual system, no extra damage entities.
        for (int i = 0; i < 18; i++) {
            double y = 1 - 2 * (i + .5) / 18, angle = i * 2.399963 + seed * .79;
            double radial = Math.sqrt(1 - y * y);
            Vector3d d = new Vector3d(Math.cos(angle) * radial, y, Math.sin(angle) * radial);
            double maximum = Math.max(.5, 4.8 - orb.subtract(center).length());
            double travel = Math.min(maximum, age * (1.05 + (i % 4) * .12));
            double tail = Math.max(0, travel - Math.max(.22, 1.35 - age * .08));
            Vector3d tip = orb.add(d.scale(travel)), back = orb.add(d.scale(tail));
            double width = (.018 + (i % 3) * .005) * fade;
            BloodMesh.tube(out, m, back, tip, width, .001, fade, i % 4 == 0 ? 1.05F : .76F);
            if (i % 3 == 0) BloodMesh.drop(out, m, tip, d.scale(-1), width * 1.35, .07 * fade, 1, fade);
        }
    }

    static void splash(IVertexBuilder out, Matrix4f m, Vector3d c, Vector3d dir, float age, float scale) {
        float fade = (1 - BloodMesh.smooth(age / 10)) * scale;
        if (fade < .005) return;
        Vector3d u = BloodMesh.side(dir), v = dir.cross(u);
        for (int i = 0; i < 9; i++) {
            double angle = i * TAU / 9;
            Vector3d radial = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            double spread = (.12 + Math.min(age, 5) * .11) * (1 + i % 3 * .2);
            Vector3d tip = c.add(radial.scale(spread)).subtract(dir.scale(.07 + age * .04));
            tip = tip.add(0, -Math.max(0, age - 3) * .025, 0);
            BloodMesh.filament(out, m, c.add(radial.scale(spread * .25)),
                    tip.add(radial.scale(-.12)), tip, .035 * fade, fade, .65F);
            BloodMesh.drop(out, m, tip, radial, .024 * fade, .075 * fade, 1, fade);
        }
    }

    private BloodEffects() { }
}
