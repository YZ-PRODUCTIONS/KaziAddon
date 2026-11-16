package net.kazi.kazimod.models.abilities;

import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.abilities.NagiAwakening.DragonVfxPhase;

/** Short-lived, untextured attack geometry. Local +Z follows the attack (or points up for cages/slams). */
public final class DragonAttackMesh {
    @FunctionalInterface
    public interface Sink { void vertex(float x, float y, float z, int color, float alpha); }

    private static final float PI = (float) Math.PI, TAU = PI * 2;
    private static final int WHITE = 0xF6FFFF, CYAN = 0x74E8FF, RED = 0xF52C65, DARK = 0x171528;
    private static final int JADE = 0x2AB98A, LIME = 0xC4F96A, VIOLET = 0x9D5CFF, LILAC = 0xEBC7FF;
    private static final int SOUL = 0x53EBDC, EMBER = 0xFF793D, ICE = 0x80C9F8, BLUE = 0x2D62AB;
    private static final int GOLD = 0xFFD45E, IVORY = 0xFFF6D8, ROSE = 0xFF88BE;

    private DragonAttackMesh() { }

    public static float radius(DragonKind kind, DragonVfxPhase phase, float length) {
        if (!Float.isFinite(length)) return 10;
        return phase == DragonVfxPhase.CHARGE || phase == DragonVfxPhase.BIND || phase == DragonVfxPhase.SLAM
                ? 10 : Math.max(0, Math.min(64, length)) + 10;
    }

    public static void render(Sink surface, Sink glow, DragonKind kind, DragonVfxPhase phase,
                              int detail, float age, float life, float length, int seed) {
        if (surface == null || glow == null || kind == null || phase == null || !Float.isFinite(age)
                || !Float.isFinite(life) || !Float.isFinite(length) || age < 0 || life <= 0 || age >= life) return;
        detail = Math.max(0, Math.min(2, detail));
        length = Math.max(0, Math.min(64, length));
        float t = age / life;
        float opacity = smooth(age / 1.2F) * (1 - smooth((t - .55F) / .45F));
        if (opacity < .0001F) return;
        if (phase == DragonVfxPhase.CHARGE) {
            charge(surface, glow, kind, detail, age, t, seed);
            return;
        }
        if (phase == DragonVfxPhase.BIND) {
            bind(surface, glow, detail, age, opacity);
            return;
        }
        if (phase == DragonVfxPhase.SLAM) {
            slam(surface, glow, detail, t, opacity, seed);
            return;
        }
        boolean special = phase == DragonVfxPhase.SPECIAL;
        switch (kind) {
            case FIRST: first(surface, glow, detail, t, length, opacity, special, seed); break;
            case BLIND: jaws(surface, glow, detail, t, length, opacity, special); break;
            case COBRA: venom(surface, glow, detail, age, t, length, opacity, special, seed); break;
            case FOUR_EYED: mind(surface, glow, detail, age, t, length, opacity, special); break;
            case CORPSE_FLAME: soul(surface, glow, detail, age, t, length, opacity, special, seed); break;
            case ICE_CRYSTAL: ice(surface, glow, detail, age, t, length, opacity, special, seed); break;
            case SPEARHEAD: thunder(surface, glow, detail, age, t, length, opacity, special, seed); break;
            case ANGEL: angel(surface, glow, detail, t, length, opacity, special); break;
            default: break;
        }
    }

    private static void first(Sink body, Sink glow, int d, float t, float length, float a, boolean special, int seed) {
        int n = count(d, 40, 28, 16);
        if (special) {
            float radius = .5F + 7.5F * easeOut(t);
            for (int plane = 0; plane < 3; plane++) {
                Sink axes = plane(glow, plane);
                arc(axes, radius, .07F * (1 - t) + .025F, 0, 0, TAU, n, CYAN, a * .65F, true);
                arc(axes, radius * .87F, .025F, 0, .12F, TAU, n, WHITE, a * .65F, true);
            }
            for (int i = 0; i < count(d, 14, 10, 6); i++) {
                float angle = TAU * i / count(d, 14, 10, 6) + hash(seed + i) * .12F;
                float r = radius * (.55F + hash(seed + i * 17) * .35F);
                float x = cos(angle) * r, y = sin(angle) * r;
                line(glow, x * .55F, y * .55F, 0, x, y, .15F, .023F, WHITE, a * .8F);
                line(glow, x, y, .15F, x * 1.07F - y * .13F, y * 1.07F + x * .13F, .1F,
                        .016F, CYAN, a * .65F);
            }
        } else {
            float flight = smooth(t / .35F), z = length * flight;
            line(glow, 0, 0, Math.max(0, z - 2), 0, 0, z, .045F, WHITE, a * .85F);
            for (int i = 0; i < 3; i++) {
                float r = .2F + .6F * t + i * .12F;
                arc(glow, r, .035F, Math.max(0, z - i * .32F), i * .3F, TAU, n / 2, CYAN, a * (1 - i * .2F), true);
            }
            if (t > .2F) {
                star(glow, 0, 0, length, .8F + t * .5F, .04F, 6, WHITE, a * smooth((t - .2F) * 5));
                arc(body, .8F + t, .12F, length, 0, TAU, n / 2, CYAN, a * .14F, true);
            }
        }
    }

    private static void jaws(Sink body, Sink glow, int d, float t, float length, float a, boolean special) {
        float s = special ? 1.65F : 1;
        float close = smooth(t / .5F), gap = s * (1.05F - .72F * close);
        int teeth = count(d, 7, 5, 3);
        for (int side = -1; side <= 1; side += 2) {
            float y = side * gap;
            for (int i = 0; i < teeth; i++) {
                float x = (i / (float) (teeth - 1) - .5F) * s * 2;
                float height = s * (.46F + .25F * (1 - Math.abs(x / s)));
                fang(body, x, y, length, s * .18F, -side * height, DARK, a * .72F);
                fang(glow, x, y, length + .045F, s * .075F, -side * height * .95F, RED, a * .8F);
            }
            for (int j = 0; j < count(d, 22, 16, 10); j++) {
                float u = j / (float) count(d, 22, 16, 10), v = (j + 1F) / count(d, 22, 16, 10);
                float x = (u - .5F) * s * 2.9F, xx = (v - .5F) * s * 2.9F;
                line(body, x, y + side * .35F * sin(PI * u), length - .1F,
                        xx, y + side * .35F * sin(PI * v), length - .1F, .14F * s, DARK, a * .5F);
            }
        }
        int claws = special ? 5 : 3, steps = count(d, 18, 12, 8);
        float rake = smooth((t - .2F) / .4F);
        for (int c = 0; c < claws; c++) {
            for (int j = 0; j < steps; j++) {
                float u = j / (float) steps, v = (j + 1F) / steps;
                float x = (c - (claws - 1) * .5F) * .32F * s;
                float angle = -.8F + u * 2.1F * rake, angle2 = -.8F + v * 2.1F * rake;
                line(glow, x + sin(angle) * s * .6F, cos(angle) * s * 1.45F - s * .6F, length + .16F,
                        x + sin(angle2) * s * .6F, cos(angle2) * s * 1.45F - s * .6F, length + .16F,
                        .035F * s * sin(PI * u), c % 2 == 0 ? RED : 0xFFB1CD, a);
            }
        }
    }

    private static void venom(Sink body, Sink glow, int d, float age, float t, float length, float a,
                              boolean special, int seed) {
        int steps = count(d, 28, 18, 12), streams = special ? 5 : 2;
        float end = length * smooth(t / .28F), spread = special ? 3.4F : .3F;
        for (int strand = 0; strand < streams; strand++) {
            float phase = strand * TAU / streams + age * .09F;
            for (int j = 0; j < steps; j++) {
                float u = j / (float) steps, v = (j + 1F) / steps;
                float r = .12F + spread * u, rr = .12F + spread * v;
                float angle = phase + u * 5, angle2 = phase + v * 5;
                float width = (.045F + u * (special ? .18F : .065F)) * sin(PI * u);
                line(body, cos(angle) * r, sin(angle) * r, end * u, cos(angle2) * rr, sin(angle2) * rr,
                        end * v, width * 2.2F, JADE, a * .35F);
                line(glow, cos(angle) * r, sin(angle) * r, end * u, cos(angle2) * rr, sin(angle2) * rr,
                        end * v, width * .55F, LIME, a * .7F);
            }
        }
        for (int i = 0; i < count(d, special ? 22 : 9, special ? 14 : 6, special ? 8 : 4); i++) {
            float u = fract(hash(seed + i * 43) + t * .9F);
            float angle = hash(seed + i * 71) * TAU + age * .03F;
            float r = (.2F + spread * u) * (.7F + hash(seed + i * 19) * .3F);
            crystal(body, cos(angle) * r, sin(angle) * r - t * t * .65F, end * u,
                    .1F + .12F * u, .22F + .25F * u, JADE, a * sin(PI * u) * .5F);
            crystal(glow, cos(angle) * r, sin(angle) * r - t * t * .65F, end * u,
                    .04F, .15F, LIME, a * sin(PI * u) * .75F);
        }
        arc(glow, .35F + t * (special ? 2.2F : .6F), .045F, length, age * -.02F, TAU,
                count(d, 28, 20, 12), JADE, a * .6F, true);
    }

    private static void mind(Sink body, Sink glow, int d, float age, float t, float length, float a, boolean special) {
        float reach = length * smooth(t / .18F), r = special ? .45F : .18F;
        line(body, 0, 0, 0, 0, 0, reach, r * 1.35F, VIOLET, a * .26F);
        line(glow, 0, 0, 0, 0, 0, reach, r * .34F, LILAC, a * .8F);
        line(glow, 0, 0, 0, 0, 0, reach, r * .12F, WHITE, a);
        helix(glow, count(d, 32, 22, 14), 0, reach, r * 1.4F, 0, age * -.12F, TAU * 2,
                .035F, VIOLET, a * .7F);
        helix(glow, count(d, 32, 22, 14), 0, reach, r * 1.4F, 0, age * -.12F + PI, TAU * 2,
                .023F, LILAC, a * .8F);
        float spread = special ? .9F : .5F;
        for (int i = 0; i < 4; i++) {
            float angle = i * PI / 2 + age * .013F;
            eye(body, glow, cos(angle) * spread, sin(angle) * spread, .1F,
                    special ? .48F : .27F, angle, a, d);
        }
        if (special) {
            for (int i = 0; i < 3; i++) arc(glow, .85F + i * .22F + t * .5F, .025F,
                    reach, age * .05F + i * .6F, TAU, count(d, 32, 20, 12), i == 1 ? WHITE : VIOLET, a * .65F, true);
            star(glow, 0, 0, reach, 1.2F + t, .035F, 4, LILAC, a);
        }
    }

    private static void soul(Sink body, Sink glow, int d, float age, float t, float length, float a,
                             boolean special, int seed) {
        int strands = special ? count(d, 7, 5, 3) : 3, steps = count(d, 24, 16, 10);
        float end = length * smooth(t / .3F), spread = special ? 2.7F : .4F;
        for (int strand = 0; strand < strands; strand++) {
            float axis = strand * TAU / strands;
            for (int j = 0; j < steps; j++) {
                float u = j / (float) steps, v = (j + 1F) / steps;
                float flicker = sin(u * 12 - age * .14F + strand), next = sin(v * 12 - age * .14F + strand);
                float rr = spread * u * (.5F + .4F * sin(PI * u));
                float ss = spread * v * (.5F + .4F * sin(PI * v));
                float x = cos(axis) * rr + flicker * .15F, y = sin(axis) * rr + u * u * .5F;
                float xx = cos(axis) * ss + next * .15F, yy = sin(axis) * ss + v * v * .5F;
                float width = sin(PI * u) * (.08F + (special ? .35F : .12F) * u);
                line(body, x, y, end * u, xx, yy, end * v, width * 1.6F, 0x184B59, a * .32F);
                line(glow, x, y, end * u, xx, yy, end * v, width,
                        strand % 3 == 0 ? EMBER : SOUL, a * .8F);
                if (j % 3 == 0) flame(glow, x, y, end * u, width * 1.4F,
                        .25F + u * .6F, axis + age * .025F, strand % 3 == 0 ? IVORY : WHITE, a * .65F);
            }
        }
        for (int i = 0; i < count(d, 12, 8, 5); i++) {
            float cycle = fract(hash(seed + i * 73) + t * .75F);
            float angle = hash(seed + i * 31) * TAU;
            float rr = (1 - cycle) * (special ? 2 : .6F);
            flame(glow, cos(angle) * rr, sin(angle) * rr + cycle, length - cycle * Math.min(2, length),
                    .1F, .3F + cycle * .45F, angle, i % 3 == 0 ? EMBER : SOUL, a * sin(PI * cycle));
        }
    }

    private static void ice(Sink body, Sink glow, int d, float age, float t, float length, float a,
                            boolean special, int seed) {
        float grow = smooth(t / .28F);
        if (special) {
            int pillars = count(d, 8, 6, 4);
            for (int i = 0; i < pillars; i++) {
                float angle = TAU * i / pillars;
                float r = 1.25F + .1F * sin(age * .05F + i);
                crystal(body, cos(angle) * r, sin(angle) * r, -.25F, .28F * grow, 1.85F * grow,
                        i % 2 == 0 ? ICE : BLUE, a * .55F);
                crystal(glow, cos(angle) * r, sin(angle) * r, -.25F, .055F * grow, 1.95F * grow,
                        WHITE, a * .65F);
            }
            for (int band = -1; band <= 1; band++) {
                arc(glow, 1.22F * grow, .032F, band * .85F, age * .015F * (band == 0 ? -1 : 1),
                        TAU, count(d, 32, 24, 16), CYAN, a * .65F, false);
            }
            for (int i = 0; i < count(d, 12, 8, 5); i++) {
                float angle = hash(seed + i * 137) * TAU;
                float r = 1.6F + t * .7F;
                crystal(body, cos(angle) * r, sin(angle) * r, -.6F + hash(seed + i * 7) * 1.4F + t,
                        .08F, .24F, ICE, a * .45F);
            }
        } else {
            float front = length * smooth(t / .3F);
            for (int i = 0; i < count(d, 7, 5, 3); i++) {
                float z = Math.max(0, front - i * .32F);
                crystal(body, sin(i * 2.4F + age * .015F) * .16F, cos(i * 2.4F) * .16F, z,
                        .13F * (1 - i * .07F), .45F, ICE, a * (1 - i * .1F));
            }
            for (int i = 0; i < 6; i++) {
                float angle = i * PI / 3;
                line(glow, 0, 0, length, cos(angle) * (.4F + t), sin(angle) * (.4F + t), length,
                        .025F, WHITE, a * .8F);
                float x = cos(angle) * (.25F + t * .5F), y = sin(angle) * (.25F + t * .5F);
                line(glow, x, y, length, x * 1.2F - y * .3F, y * 1.2F + x * .3F, length, .018F, CYAN, a);
                line(glow, x, y, length, x * 1.2F + y * .3F, y * 1.2F - x * .3F, length, .018F, CYAN, a);
            }
        }
    }

    private static void thunder(Sink body, Sink glow, int d, float age, float t, float length, float a,
                                boolean special, int seed) {
        float end = length * smooth(t / .15F), power = special ? 1 : .55F;
        int steps = count(d, 26, 18, 12), change = (int) (age / 2);
        for (int strand = 0; strand < (special ? 3 : 1); strand++) {
            float px = 0, py = 0, pz = 0;
            for (int i = 1; i <= steps; i++) {
                float u = i / (float) steps, radius = sin(PI * u) * .6F * power;
                float x = (hash(seed + change * 7 + i * 97 + strand * 137) - .5F) * radius;
                float y = (hash(seed + change * 13 + i * 71 + strand * 97) - .5F) * radius;
                float z = end * u;
                line(glow, px, py, pz, x, y, z, .065F * power, GOLD, a * .65F);
                line(glow, px, py, pz, x, y, z, .022F * power, WHITE, a);
                if (special && i % 5 == 0) {
                    float angle = hash(seed + i * 31 + strand) * TAU;
                    float xx = x + cos(angle) * 1.5F, yy = y + sin(angle) * 1.5F;
                    line(glow, x, y, z, (x + xx) * .5F - .15F, (y + yy) * .5F + .15F,
                            z - .3F, .025F, GOLD, a * .8F);
                    line(glow, (x + xx) * .5F - .15F, (y + yy) * .5F + .15F, z - .3F,
                            xx, yy, z - .6F, .012F, IVORY, a * .6F);
                }
                px = x; py = y; pz = z;
            }
        }
        crystal(body, 0, 0, end - .2F, .3F * power, .9F * power, GOLD, a * .65F);
        crystal(glow, 0, 0, end - .15F, .12F * power, 1.1F * power, WHITE, a);
        star(glow, 0, 0, length, power * (1 + t), .035F, special ? 8 : 4, GOLD, a);
        if (special) arc(glow, .9F + t, .045F, length, age * .1F, TAU,
                count(d, 24, 16, 12), WHITE, a * .65F, true);
    }

    private static void angel(Sink body, Sink glow, int d, float t, float length, float a, boolean special) {
        float spread = (special ? 1.9F : 1.15F) * (.65F + .35F * smooth(t / .3F));
        wings(body, glow, d, length, spread, a);
        helix(glow, count(d, 20, 14, 10), 0, length * smooth(t / .28F), .18F, .1F,
                t * TAU, TAU, .03F, ROSE, a * .8F);
        star(glow, 0, 0, length, .55F + t * .5F, .035F, 4, IVORY, a);
    }

    private static void bind(Sink body, Sink glow, int d, float age, float a) {
        float appear = smooth(age / 5);
        for (int i = 0; i < 3; i++) {
            helix(body, count(d, 32, 22, 14), -1.2F, 1.2F, .75F * appear, 0, age * .045F + i * TAU / 3,
                    TAU * 1.3F, .10F, ROSE, a * .28F);
            helix(glow, count(d, 32, 22, 14), -1.2F, 1.2F, .79F * appear, 0, age * .045F + i * TAU / 3,
                    TAU * 1.3F, .022F, IVORY, a * .8F);
        }
        arc(glow, 1.05F * appear, .035F, 1.25F, age * .045F, TAU, count(d, 32, 24, 16), ROSE, a * .8F, true);
        for (int side = -1; side <= 1; side += 2) {
            int count = count(d, 7, 5, 3);
            for (int i = 0; i < count; i++) {
                float u = i / (float) count;
                feather(body, glow, side * (.7F + u * 1.2F) * appear, .05F,
                        .8F - u * .6F, side * (.35F + u * .3F), 0, -.55F - u * .4F,
                        .16F, a * .75F);
            }
        }
    }

    private static void slam(Sink body, Sink glow, int d, float t, float a, int seed) {
        float radius = .5F + 3 * easeOut(t);
        arc(glow, radius, .09F * (1 - t), .06F, 0, TAU, count(d, 40, 28, 18), IVORY, a * .85F, false);
        arc(body, radius * .85F, .28F, .04F, .12F, TAU, count(d, 40, 28, 18), ROSE, a * .25F, true);
        star(glow, 0, 0, .08F, radius * .8F, .045F, 8, ROSE, a);
        int feathers = count(d, 14, 10, 6);
        for (int i = 0; i < feathers; i++) {
            float angle = i * TAU / feathers;
            float r = (.5F + t * 2.3F), height = sin(PI * t) * (1 + hash(seed + i * 19));
            feather(body, glow, cos(angle) * r, sin(angle) * r, .1F,
                    cos(angle) * .35F, sin(angle) * .35F, height, .18F * (1 - t), a);
        }
    }

    private static void charge(Sink body, Sink glow, DragonKind kind, int d, float age, float t, int seed) {
        float a = smooth(age / 2) * (1 - smooth((t - .9F) / .1F));
        float r = 1.15F - t * .65F;
        int n = count(d, 28, 20, 12);
        switch (kind) {
            case FIRST:
                for (int i = 0; i < 3; i++) arc(glow, r + i * .13F, .027F, i * .18F,
                        age * .04F * (i % 2 == 0 ? 1 : -1), TAU, n, i == 1 ? WHITE : CYAN, a, true);
                break;
            case BLIND:
                jaws(body, glow, d, t * .4F, .25F, a * .8F, false);
                break;
            case COBRA:
                helix(body, n, -.5F, .7F, r * .65F, -.25F, age * .15F, TAU * 1.6F, .11F, JADE, a * .45F);
                helix(glow, n, -.5F, .7F, r * .65F, -.25F, age * .15F + PI, TAU * 1.6F, .025F, LIME, a);
                break;
            case FOUR_EYED:
                for (int i = 0; i < 4; i++) {
                    float angle = i * PI / 2 + age * .025F;
                    eye(body, glow, cos(angle) * r, sin(angle) * r, 0, .34F, angle, a, d);
                }
                star(glow, 0, 0, 0, .15F + t * .25F, .035F, 4, WHITE, a);
                break;
            case CORPSE_FLAME:
                for (int i = 0; i < count(d, 10, 7, 5); i++) {
                    float u = fract(hash(seed + i * 53) + age * .03F), angle = i * 2.4F + age * .04F;
                    flame(glow, cos(angle) * r * (1 - u), sin(angle) * r * (1 - u), -.5F + u,
                            .14F, .6F, angle, i % 3 == 0 ? EMBER : SOUL, a * sin(PI * u));
                }
                break;
            case ICE_CRYSTAL:
                for (int i = 0; i < 6; i++) {
                    float angle = i * PI / 3 + age * .035F;
                    crystal(body, cos(angle) * r, sin(angle) * r, 0, .14F, .45F, ICE, a * .7F);
                    crystal(glow, cos(angle) * r, sin(angle) * r, 0, .035F, .5F, WHITE, a);
                }
                break;
            case SPEARHEAD:
                for (int i = 0; i < 6; i++) {
                    float angle = i * PI / 3 + age * -.025F;
                    float x = cos(angle) * r, y = sin(angle) * r;
                    line(glow, x, y, -.3F, x * .45F - y * .15F, y * .45F + x * .15F, .1F, .035F, GOLD, a);
                    line(glow, x * .45F - y * .15F, y * .45F + x * .15F, .1F, 0, 0, .4F, .018F, WHITE, a);
                }
                crystal(glow, 0, 0, .2F, .11F + t * .08F, .4F, IVORY, a);
                break;
            case ANGEL:
                wings(body, glow, d, 0, .55F + t * .5F, a);
                arc(glow, .45F, .025F, .05F, age * .04F, TAU, n, ROSE, a * .7F, true);
                break;
            default: break;
        }
    }

    private static void wings(Sink body, Sink glow, int d, float z, float scale, float a) {
        int n = count(d, 8, 6, 4);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < n; i++) {
                float u = i / (float) n;
                float x = side * (.12F + u * 1.1F) * scale, y = (.18F + sin(u * PI) * .35F) * scale;
                feather(body, glow, x, y, z, side * (.4F + u * .4F) * scale,
                        (-.1F - u * .65F) * scale, -.08F * i, .15F * scale, a);
            }
        }
    }

    private static void eye(Sink body, Sink glow, float x, float y, float z, float scale, float angle, float a, int d) {
        int n = count(d, 20, 14, 10);
        for (int side = -1; side <= 1; side += 2) {
            for (int j = 0; j < n; j++) {
                float u = j / (float) n, v = (j + 1F) / n;
                float xx = (u * 2 - 1) * scale, yy = sin(u * PI) * scale * .43F * side;
                float xxx = (v * 2 - 1) * scale, yyy = sin(v * PI) * scale * .43F * side;
                line(glow, x + xx * cos(angle) - yy * sin(angle), y + xx * sin(angle) + yy * cos(angle), z,
                        x + xxx * cos(angle) - yyy * sin(angle), y + xxx * sin(angle) + yyy * cos(angle), z,
                        .018F, LILAC, a);
            }
        }
        crystal(body, x, y, z, scale * .19F, .04F, DARK, a * .8F);
        crystal(glow, x, y, z + .025F, scale * .085F, .08F, WHITE, a);
    }

    private static void helix(Sink out, int n, float z0, float z1, float radius, float growth,
                              float phase, float turns, float width, int color, float alpha) {
        for (int i = 0; i < n; i++) {
            float u = i / (float) n, v = (i + 1F) / n;
            float a = phase + turns * u, b = phase + turns * v;
            line(out, cos(a) * (radius + growth * u), sin(a) * (radius + growth * u), mix(z0, z1, u),
                    cos(b) * (radius + growth * v), sin(b) * (radius + growth * v), mix(z0, z1, v),
                    width * (.3F + .7F * sin(PI * u)), color, alpha);
        }
    }

    private static void feather(Sink body, Sink glow, float x, float y, float z, float dx, float dy, float dz,
                                float width, float a) {
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float px = length > .001F ? -dy / length * width : width;
        float py = length > .001F ? dx / length * width : 0;
        quad(body, x, y, z, x + dx * .4F + px, y + dy * .4F + py, z + dz * .4F,
                x + dx, y + dy, z + dz, x + dx * .4F - px, y + dy * .4F - py, z + dz * .4F,
                IVORY, a * .58F);
        line(glow, x, y, z + .015F, x + dx, y + dy, z + dz + .015F, width * .12F, ROSE, a * .8F);
    }

    private static void flame(Sink out, float x, float y, float z, float width, float height, float angle,
                              int color, float a) {
        float dx = cos(angle) * width, dy = sin(angle) * width;
        quad(out, x, y, z - height * .5F, x + dx, y + dy, z,
                x + dx * .45F, y + dy * .45F, z + height, x - dx, y - dy, z,
                color, a);
    }

    private static void fang(Sink out, float x, float y, float z, float width, float dy, int color, float alpha) {
        float tipZ = z + .08F;
        tri(out, x - width, y, z - width, x + width, y, z - width, x, y + dy, tipZ, color, alpha);
        tri(out, x + width, y, z - width, x + width, y, z + width, x, y + dy, tipZ, color, alpha * .7F);
        tri(out, x + width, y, z + width, x - width, y, z + width, x, y + dy, tipZ, color, alpha);
        tri(out, x - width, y, z + width, x - width, y, z - width, x, y + dy, tipZ, color, alpha * .7F);
    }

    private static void crystal(Sink out, float x, float y, float z, float radius, float height, int color, float alpha) {
        for (int i = 0; i < 4; i++) {
            float a = i * PI / 2, b = (i + 1) * PI / 2;
            float xx = x + cos(a) * radius, yy = y + sin(a) * radius;
            float xxx = x + cos(b) * radius, yyy = y + sin(b) * radius;
            tri(out, xx, yy, z, xxx, yyy, z, x, y, z + height, color, alpha * (i % 2 == 0 ? 1 : .65F));
            tri(out, xxx, yyy, z, xx, yy, z, x, y, z - height * .65F, color, alpha * .55F);
        }
    }

    private static void star(Sink out, float x, float y, float z, float size, float width,
                             int points, int color, float a) {
        for (int i = 0; i < points; i++) {
            float angle = TAU * i / points;
            line(out, x + cos(angle) * .12F, y + sin(angle) * .12F, z,
                    x + cos(angle) * size, y + sin(angle) * size, z, width, color, a);
        }
    }

    private static void arc(Sink out, float radius, float width, float z, float start, float span,
                            int segments, int color, float a, boolean broken) {
        if (radius <= 0 || width <= 0) return;
        for (int i = 0; i < segments; i++) {
            if (broken && i % 7 == 0) continue;
            float angle = start + span * i / segments, next = start + span * (i + 1F) / segments;
            quad(out, cos(angle) * (radius - width), sin(angle) * (radius - width), z,
                    cos(next) * (radius - width), sin(next) * (radius - width), z,
                    cos(next) * (radius + width), sin(next) * (radius + width), z,
                    cos(angle) * (radius + width), sin(angle) * (radius + width), z, color, a);
        }
    }

    /** Two crossed ribbons keep thin lines visible from any camera direction. */
    private static void line(Sink out, float x0, float y0, float z0, float x1, float y1, float z1,
                             float width, int color, float alpha) {
        if (width <= .00001F || alpha <= .00001F) return;
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < .00001F) return;
        dx /= len; dy /= len; dz /= len;
        float px = -dy, py = dx, pz = 0, planar = (float) Math.sqrt(px * px + py * py);
        if (planar < .001F) { px = 1; py = 0; } else { px /= planar; py /= planar; }
        float qx = -dz * py, qy = dz * px, qz = dx * py - dy * px;
        for (int i = 0; i < 2; i++) {
            float xx = (i == 0 ? px : qx) * width, yy = (i == 0 ? py : qy) * width,
                    zz = (i == 0 ? pz : qz) * width;
            quad(out, x0 - xx, y0 - yy, z0 - zz, x0 + xx, y0 + yy, z0 + zz,
                    x1 + xx, y1 + yy, z1 + zz, x1 - xx, y1 - yy, z1 - zz, color, alpha);
        }
    }

    private static Sink plane(Sink out, int plane) {
        if (plane == 1) return (x, y, z, c, a) -> out.vertex(x, z, y, c, a);
        if (plane == 2) return (x, y, z, c, a) -> out.vertex(z, x, y, c, a);
        return out;
    }

    private static void tri(Sink out, float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, int color, float alpha) {
        quad(out, ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz, color, alpha);
    }

    private static void quad(Sink out, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int color, float alpha) {
        alpha = clamp(alpha);
        if (alpha < .00001F) return;
        out.vertex(ax, ay, az, color, alpha); out.vertex(bx, by, bz, color, alpha);
        out.vertex(cx, cy, cz, color, alpha); out.vertex(dx, dy, dz, color, alpha);
    }

    private static int count(int detail, int high, int medium, int low) { return detail == 0 ? high : detail == 1 ? medium : low; }
    private static float mix(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float smooth(float value) { float t = clamp(value); return t * t * (3 - 2 * t); }
    private static float easeOut(float value) { float t = 1 - clamp(value); return 1 - t * t * t; }
    private static float sin(float angle) { return (float) Math.sin(angle); }
    private static float cos(float angle) { return (float) Math.cos(angle); }
    private static float fract(float value) { return value - (float) Math.floor(value); }
    private static float hash(int seed) {
        int v = seed * 0x45d9f3b; v = (v ^ v >>> 16) * 0x45d9f3b; v ^= v >>> 16;
        return (v & 0x7fffffff) / 2147483647F;
    }
}
