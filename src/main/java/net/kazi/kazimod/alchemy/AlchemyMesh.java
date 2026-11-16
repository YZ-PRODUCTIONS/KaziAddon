package net.kazi.kazimod.alchemy;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Closed, textured masonry and depth-tested, circleless transmutation filaments. */
public final class AlchemyMesh extends RenderState {
    public static final RenderType STONE = RenderType.create("alchemy_masonry",
            DefaultVertexFormats.NEW_ENTITY, 7, 262144, false, true,
            RenderType.State.builder().setTextureState(new TextureState(AtlasTexture.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setAlphaState(NO_ALPHA)
                    .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setShadeModelState(SMOOTH_SHADE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOutputState(MAIN_TARGET).createCompositeState(false));
    public static final RenderType ENERGY = RenderType.create("alchemy_blue_white_filaments",
            DefaultVertexFormats.POSITION_COLOR, 7, 262144, false, false,
            RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                    .setShadeModelState(SMOOTH_SHADE).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).setOutputState(MAIN_TARGET).createCompositeState(false));

    static final class Budget {
        int stone, energy, probes;
        Budget(int stone, int energy, int probes) { this.stone = stone; this.energy = energy; this.probes = probes; }
    }

    static final class SurfaceCache {
        private final Map<Long, Sample> samples = new HashMap<>();
        private long pillarContact = Long.MIN_VALUE, pillarProbeTick = Long.MIN_VALUE;
        private Vector3d pillarAnchor;

        Vector3d pillarAnchor(AlchemyField f, Vector3d victim, long contact, Budget budget) {
            if (pillarContact != contact) {
                pillarContact = contact; pillarAnchor = null; pillarProbeTick = Long.MIN_VALUE;
            }
            if (pillarAnchor != null) return pillarAnchor;
            long now = f.level.getGameTime();
            if (pillarProbeTick == now || budget.probes <= 0) return null;
            pillarProbeTick = now;
            if (!f.level.hasChunkAt(new BlockPos(victim))) return null;
            budget.probes--;
            RayTraceResult hit = f.level.clip(new RayTraceContext(victim.add(0, .25, 0),
                    victim.add(0, -4, 0), RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE, f));
            if (hit.getType() != RayTraceResult.Type.MISS) {
                // Keep a world-space base: this field follows the airborne victim after contact+20.
                pillarAnchor = hit.getLocation().add(0, .018, 0);
            }
            return pillarAnchor;
        }

        double height(AlchemyField f, Vector3d origin, double x, double y, double z, Budget budget) {
            double wx = origin.x + x, wz = origin.z + z, wy = origin.y + y;
            long key = new BlockPos(MathHelper.floor(wx * 2), MathHelper.floor(wy), MathHelper.floor(wz * 2)).asLong();
            Sample sample = samples.get(key);
            long now = f.level.getGameTime();
            if (sample != null && now - sample.tick < 10) return sample.y - origin.y;
            if (budget.probes-- <= 0) return sample == null ? Double.NaN : sample.y - origin.y;
            if (!f.level.hasChunkAt(new BlockPos(wx, wy, wz))) return Double.NaN;
            RayTraceResult hit = f.level.clip(new RayTraceContext(new Vector3d(wx, wy + 1.8, wz),
                    new Vector3d(wx, wy - 2.5, wz), RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE, f));
            double height = hit.getType() == RayTraceResult.Type.MISS ? Double.NaN : hit.getLocation().y + .028;
            if (samples.size() >= 384) samples.clear();
            samples.put(key, new Sample(height, now));
            return height - origin.y;
        }
    }

    private static final class Sample {
        final double y; final long tick;
        Sample(double y, long tick) { this.y = y; this.tick = tick; }
    }

    static void render(AlchemyField field, float partial, MatrixStack stack, IVertexBuilder stone,
                       IVertexBuilder energy, Budget budget, SurfaceCache surface, int detail, float distanceFade) {
        float age = field.age(partial);
        float alpha = (field.kind() == 3 ? 1 : smooth(age / 2)) * smooth((field.life() - age) / 10) * distanceFade;
        if (alpha <= .002F || !Float.isFinite(age)) return;
        Context c = new Context(field, partial, stack, stone, energy, budget, surface, detail, alpha);
        switch (field.kind()) {
            case 0: c.groundBurst(Vector3d.ZERO, 1.2, age, alpha); break;
            case 1: fangs(c); break;
            case 2: wall(c); break;
            case 3: prison(c); break;
            case 4: combination(c); break;
            case 5: grand(c); break;
            default: break;
        }
    }

    private static void fangs(Context c) {
        int[] nodes = c.f.nodes();
        int count = Math.min(32, nodes.length / 3);
        Vector3d previous = Vector3d.ZERO;
        for (int i = 0; i < count; i++) {
            Vector3d base = c.node(nodes, i);
            if (base == null) continue;
            float elapsed = c.age - (12 + i * 2);
            if (elapsed > -9 && elapsed < 10) {
                float pulse = smooth((elapsed + 9) / 6) * (1 - smooth((elapsed - 3) / 7));
                c.groundBolt(previous, base, 73 + i * 31, .026, pulse * c.alpha);
                c.groundBurst(base, .85, elapsed + 8, pulse * c.alpha);
            }
            if (elapsed >= 0) {
                float rise = smooth(elapsed / 3);
                float sink = smooth((c.age - (c.f.life() - 12)) / 12);
                Vector3d foot = base.add(0, -.22 - sink * 2.8, 0);
                c.spike(foot, .65, 2.6 * rise, c.forward.scale(.62), i * 19, c.alpha);
                if (c.detail > 0) {
                    Vector3d side = c.right.scale(i % 2 == 0 ? .62 : -.62);
                    c.spike(foot.add(side), .29, 1.1 * rise, c.forward.scale(.28), i * 19 + 1, c.alpha);
                }
                c.chips(base, elapsed, 120 + i, c.detail > 1 ? 5 : 2, .8);
                if (elapsed < 10) c.bolt(base.add(0, .05, 0), foot.add(c.forward.scale(.62)).add(0, 2.6 * rise, 0),
                        42 + i, .019, (1 - smooth(elapsed / 10)) * c.alpha, false);
            }
            previous = base;
        }
    }

    private static void wall(Context c) {
        double halfWidth = MathHelper.clamp(c.f.size(), .5F, 4F);
        boolean launched = c.f.phase() == 1;
        float phaseAge = launched ? c.f.phaseAge(c.partial) : 0;
        float split = launched ? smooth(phaseAge / 5) : 0;
        float lifetime = launched ? 1 - smooth((phaseAge - 19) / 6) : 1;
        float alpha = c.alpha * lifetime;
        int columns = 5, rows = 3;
        double cellWidth = halfWidth * 2 / columns;
        for (int column = 0; column < columns; column++) {
            float rise = launched ? 1 : smooth((c.age - 8 - Math.abs(column - 2) * .3F) / 3.4F);
            double sink = launched ? 0 : smooth((c.age - c.f.life() + 10) / 10) * 3.6;
            for (int row = 0; row < rows; row++) {
                int seed = column * 37 + row * 89;
                double x = -halfWidth + cellWidth * (column + .5);
                double y = (row + .5) * 3.5 / rows - (1 - rise) * 3.6 - sink;
                double z = split * (noise(seed) - .5) * .22;
                double gap = .016 + split * .14;
                if (rise > 0) c.box(c.facing(x, y, z), cellWidth - gap, 3.5 / rows - gap, .8 - gap,
                        -c.heading + split * (noise(seed + 4) - .5) * .14, seed, alpha);
                if (c.age >= 8 && (c.age < 20 || launched)) {
                    Vector3d a = c.facing(x - cellWidth * .42, y - .45, -.41 - split * .05);
                    Vector3d b = c.facing(x + cellWidth * .35, y + .45, -.41 - split * .05);
                    c.bolt(a, b, seed + 90, .016, alpha * (launched ? .8F : 1 - smooth((c.age - 12) / 8)), false);
                }
                if (launched && c.detail > 0 && (row + column) % 2 == 0) {
                    Vector3d head = c.facing(x, y, -.15);
                    c.bolt(head, head.subtract(c.forward.scale(1.2 + split)), seed, .018,
                            alpha * .65F, false);
                }
            }
        }
        if (!launched && c.age < 22) {
            c.groundBolt(c.facing(-halfWidth, .03, -.48), c.facing(halfWidth, .03, -.48),
                    301, .03, c.alpha * (1 - smooth((c.age - 14) / 8)));
            c.chips(Vector3d.ZERO, c.age - 10, 403, c.detail > 0 ? 10 : 4, halfWidth);
        }
    }

    private static void prison(Context c) {
        double radius = MathHelper.clamp(c.f.size(), 1, 3);
        float released = smooth((c.age - 60) / 11);
        float rise = smooth((c.age - 14) / 4);
        int ribs = 8;
        for (int i = 0; i < ribs; i++) {
            double a = i * Math.PI * 2 / ribs;
            Vector3d radial = new Vector3d(Math.cos(a), 0, Math.sin(a));
            Vector3d foot = radial.scale(radius);
            if (c.age < 22 || released > 0) {
                float pulse = c.age < 14 ? .45F + .55F * smooth((c.age - 6) / 8) : 1 - smooth((c.age - 16) / 6);
                c.groundBolt(radial.scale(radius * .45), radial.scale(radius * 1.3), 510 + i * 31, .022, pulse * c.alpha);
            }
            if (rise <= 0) continue;
            double down = (1 - rise) * 3.1 + released * 3.4;
            Vector3d p0 = foot.add(radial.scale(released * .5)).add(0, -down, 0);
            Vector3d p1 = radial.scale(radius * .98 + released * .6).add(0, 1.15 - down, 0);
            Vector3d p2 = radial.scale(radius * .77 + released * .8).add(0, 2.5 - down, 0);
            Vector3d p3 = radial.scale(radius * .18 + released * 1.1).add(0, 3 - down, 0);
            c.beamStone(p0, p1, .30, .24, i, c.alpha);
            c.beamStone(p1, p2, .24, .19, i + 10, c.alpha);
            c.beamStone(p2, p3, .19, .14, i + 20, c.alpha);
            c.box(p0.add(0, .12, 0), .62, .26, .62, a, i + 30, c.alpha);
            if (c.age < 24) {
                c.bolt(p0.add(radial.scale(.31)), p2.add(radial.scale(.22)), i * 31, .018,
                        c.alpha * (1 - smooth((c.age - 18) / 6)), true);
                c.chips(foot, c.age - 14, i * 41, c.detail > 1 ? 3 : 1, .55);
            }
        }
    }

    private static void combination(Context c) {
        LivingEntity owner = c.f.owner();
        if (c.f.size() > 1.5F) {
            c.groundBurst(Vector3d.ZERO, 2.8, c.age, c.alpha * (1 - smooth(c.age / 13)));
            c.chips(Vector3d.ZERO, c.age, 700, c.detail > 0 ? 12 : 5, 1.4);
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * .4 + .17;
                float rise = smooth(c.age / 3) * (1 - smooth((c.age - 6) / 10));
                c.spike(new Vector3d(Math.cos(a) * .85, -.15, Math.sin(a) * .85), .30,
                        rise * .8, new Vector3d(Math.cos(a) * .25, 0, Math.sin(a) * .25), i, c.alpha);
            }
            return;
        }
        if (owner == null || AlchemyFeatures.action(owner) != 4) return;
        CompoundNBT state = AlchemyFeatures.state(owner);
        long contact = state.getLong("ComboContact");
        if (contact <= 0) return;
        float time = owner.level.getGameTime() - contact + c.partial;
        LivingEntity target = c.f.target();
        if (target != null && time >= 20 && time < 32) combinationPillar(c, target, contact, time - 20);
        double height = target == null ? 1 : Math.min(2.8, target.getBbHeight() * .57);
        Vector3d at = new Vector3d(0, height, 0);
        for (int hit = 0; hit < 4; hit++) {
            float elapsed = time - hit * 10;
            if (elapsed < 0 || elapsed > 7) continue;
            float fade = 1 - smooth(elapsed / 7);
            float grow = smooth(elapsed / 3);
            double tilt = hit == 1 ? -.2 : hit == 3 ? 1.4 : hit == 0 ? .45 : -.65;
            Vector3d across = c.right.scale(Math.cos(tilt)).add(0, Math.sin(tilt), 0);
            Vector3d up = c.right.scale(-Math.sin(tilt)).add(0, Math.cos(tilt), 0);
            Vector3d last = null;
            int steps = c.detail > 0 ? 12 : 7;
            for (int i = 0; i <= steps; i++) {
                double a = -.9 + i * 1.8 / steps;
                Vector3d point = at.add(across.scale(Math.sin(a) * (1.15 + grow * .5)))
                        .add(up.scale(Math.cos(a) * .25)).subtract(c.forward.scale(.35 + .1 * Math.cos(a)));
                if (last != null) c.light(last, point, .025 * Math.sin(i * Math.PI / (steps + 1)), fade * c.alpha);
                last = point;
            }
            for (int branch = 0; branch < (c.detail > 0 ? 5 : 3); branch++) {
                double a = branch * 2.39996 + hit;
                Vector3d end = at.add(c.right.scale(Math.cos(a) * (grow + .35)))
                        .add(0, Math.sin(a) * (.5 + grow * .6), 0);
                c.bolt(at, end, branch * 31 + hit * 101, .017, fade * c.alpha, false);
            }
        }
    }

    private static void combinationPillar(Context c, LivingEntity target, long contact, float elapsed) {
        Vector3d anchor = c.surface.pillarAnchor(c.f, target.getPosition(c.partial), contact, c.budget);
        if (anchor == null) return;
        Vector3d base = anchor.subtract(c.origin);
        float rise = smooth(elapsed / 2.5F), retract = smooth((elapsed - 8) / 4);
        float opacity = c.alpha * smooth(elapsed / .8F) * (1 - smooth((elapsed - 9) / 3));
        double lift = rise * (1 - retract), height = 1.8 * lift;
        // Three staggered slabs emerge from below the floor, then sink before the combo field ends.
        for (int tier = 0; tier < 3; tier++) {
            double y = (tier + .5) * .6 - (1 - lift) * 1.8;
            c.box(base.add(0, y, 0), 1.12 - tier * .06, .58, 1.0 - tier * .05,
                    -c.heading + (tier - 1) * .025, 820 + tier * 13, opacity);
        }
        c.box(base.add(0, -.06 - (1 - lift) * .2, 0), 1.35, .18, 1.22, -c.heading, 865, opacity);
        c.box(base.add(0, height - .09, 0), 1.18, .18, 1.06, -c.heading, 881, opacity);
        float charge = c.alpha * (1 - smooth(elapsed / 6));
        c.groundBurst(base, 1.35, elapsed + 1, charge);
        if (height > .08) for (int side = -1; side <= 1; side += 2) {
            Vector3d seam = base.add(c.facing(side * .36, 0, -.54));
            c.bolt(seam.add(0, .03, 0), seam.add(0, height, 0), 903 + side, .022, charge, c.detail > 0);
        }
        c.chips(base, elapsed * (14F / 12), 917, c.detail > 0 ? 4 : 2, .8);
    }

    private static void grand(Context c) {
        int[] nodes = c.f.nodes();
        int count = Math.min(72, nodes.length / 3);
        int stride = c.detail == 0 ? 2 : 1;
        for (int i = 0; i < count; i += stride) {
            Vector3d base = c.node(nodes, i);
            if (base == null) continue;
            double radius = Math.sqrt(base.x * base.x + base.z * base.z);
            Vector3d inward = new Vector3d(-base.x, 0, -base.z).normalize();
            float start = 60 + MathHelper.clamp((12 - (float)radius) / .42F, 0, 24);
            float elapsed = c.age - start;
            if (c.age < 60) {
                float charge = smooth((c.age - 8) / 52);
                float crawl = .55F + .45F * (float)Math.sin(c.age * .17 + i * 1.7);
                c.groundBolt(base.add(inward.scale(-.65)), base.add(inward.scale(1.4 + charge)),
                        i * 83, .014 + .012 * charge, c.alpha * charge * (.3F + .7F * crawl));
                if (c.detail > 0 && i % 3 == 0) c.box(base.add(0, -.13 + charge * .15, 0),
                        .65, .2, .5, noise(i) * 6, i, c.alpha * charge);
                continue;
            }
            if (elapsed < -5) continue;
            float electricity = smooth((elapsed + 5) / 3) * (1 - smooth((elapsed - 5) / 10));
            c.groundBolt(base.add(inward.scale(-1)), base.add(inward.scale(2.7)), i * 83,
                    .035, electricity * c.alpha);
            if (elapsed < 0) continue;
            float rise = smooth(elapsed / 4);
            double sink = smooth((c.age - 96) / 14) * 4.5;
            Vector3d p0 = base.add(0, -.20 - sink, 0);
            Vector3d p1 = p0.add(inward.scale(.23)).add(0, 1.15 * rise, 0);
            Vector3d p2 = p0.add(inward.scale(.85 * rise)).add(0, 2.65 * rise, 0);
            Vector3d p3 = p0.add(inward.scale(1.65 * rise)).add(0, 3.8 * rise, 0);
            c.beamStone(p0, p1, .65, .48, i * 3, c.alpha);
            c.beamStone(p1, p2, .48, .30, i * 3 + 1, c.alpha);
            c.beamStone(p2, p3, .30, .035, i * 3 + 2, c.alpha);
            if (c.detail > 0) c.spike(p0.add(cross(inward, new Vector3d(0, 1, 0)).scale(.65)),
                    .29, 1.35 * rise, inward.scale(.4), i * 53, c.alpha);
            c.chips(base, elapsed, i * 47, c.detail > 1 ? 4 : 1, 1.1);
            c.bolt(p0.add(inward.scale(-.66)), p2.add(inward.scale(-.32)), i * 83,
                    .024, electricity * c.alpha, c.detail > 0);
        }
        if (c.age >= 83 && c.age < 100) {
            float t = c.age - 83;
            c.groundBurst(Vector3d.ZERO, 3.1, t, c.alpha * (1 - smooth(t / 17)));
            c.chips(Vector3d.ZERO, t, 933, c.detail > 0 ? 10 : 4, 2.3);
        }
    }

    private static final class Context {
        final AlchemyField f;
        final float partial, age, alpha;
        final int detail, light;
        final double heading;
        final Vector3d origin, forward, right;
        final Matrix4f matrix;
        final Matrix3f normal;
        final IVertexBuilder stone, energy;
        final Budget budget;
        final SurfaceCache surface;
        final TextureAtlasSprite sprite;

        Context(AlchemyField f, float partial, MatrixStack stack, IVertexBuilder stone, IVertexBuilder energy,
                Budget budget, SurfaceCache surface, int detail, float alpha) {
            this.f = f; this.partial = partial; this.age = f.visualAge(partial); this.alpha = alpha;
            this.stone = stone; this.energy = energy; this.budget = budget; this.surface = surface; this.detail = detail;
            origin = f.getPosition(partial); matrix = stack.last().pose(); normal = stack.last().normal();
            heading = Math.toRadians(Float.isFinite(f.heading()) ? f.heading() : 0);
            forward = new Vector3d(-Math.sin(heading), 0, Math.cos(heading));
            right = new Vector3d(Math.cos(heading), 0, Math.sin(heading));
            BlockState material = f.material();
            if (material == null || material.getMaterial() != Material.STONE && material.getMaterial() != Material.SAND)
                material = Blocks.STONE.defaultBlockState();
            sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(material);
            light = WorldRenderer.getLightColor(f.level, new BlockPos(origin).above());
        }

        Vector3d facing(double x, double y, double z) { return right.scale(x).add(forward.scale(z)).add(0, y, 0); }

        Vector3d node(int[] nodes, int i) {
            int offset = i * 3;
            Vector3d at = new Vector3d(nodes[offset] + .5 - origin.x, nodes[offset + 1] + 1.028 - origin.y,
                    nodes[offset + 2] + .5 - origin.z);
            return at.lengthSqr() > 48 * 48 ? null : at;
        }

        void box(Vector3d center, double width, double height, double depth, double yaw, int seed, float opacity) {
            if (height <= .001 || opacity <= .002 || budget.stone < 24) return;
            Vector3d u = new Vector3d(Math.cos(yaw) * width * .5, 0, -Math.sin(yaw) * width * .5);
            Vector3d v = new Vector3d(0, height * .5, 0);
            Vector3d w = new Vector3d(Math.sin(yaw) * depth * .5, 0, Math.cos(yaw) * depth * .5);
            Vector3d[] p = {center.subtract(u).subtract(v).subtract(w), center.add(u).subtract(v).subtract(w),
                    center.add(u).add(v).subtract(w), center.subtract(u).add(v).subtract(w),
                    center.subtract(u).subtract(v).add(w), center.add(u).subtract(v).add(w),
                    center.add(u).add(v).add(w), center.subtract(u).add(v).add(w)};
            hull(p, seed, opacity);
        }

        void hull(Vector3d[] p, int seed, float opacity) {
            face(p[0], p[3], p[2], p[1], seed, opacity);
            face(p[5], p[6], p[7], p[4], seed + 1, opacity);
            face(p[4], p[7], p[3], p[0], seed + 2, opacity);
            face(p[1], p[2], p[6], p[5], seed + 3, opacity);
            face(p[3], p[7], p[6], p[2], seed + 4, opacity);
            face(p[4], p[0], p[1], p[5], seed + 5, opacity);
        }

        void beamStone(Vector3d a, Vector3d b, double root, double tip, int seed, float opacity) {
            Vector3d direction = b.subtract(a);
            if (direction.lengthSqr() < .0001) return;
            Vector3d u = cross(direction, new Vector3d(0, 1, 0));
            if (u.lengthSqr() < .001) u = new Vector3d(1, 0, 0);
            u = u.normalize();
            Vector3d v = cross(direction.normalize(), u).normalize();
            Vector3d[] p = {a.subtract(u.scale(root)).subtract(v.scale(root * .65)),
                    a.add(u.scale(root)).subtract(v.scale(root * .65)),
                    b.add(u.scale(tip)).subtract(v.scale(tip * .65)),
                    b.subtract(u.scale(tip)).subtract(v.scale(tip * .65)),
                    a.subtract(u.scale(root)).add(v.scale(root * .65)),
                    a.add(u.scale(root)).add(v.scale(root * .65)),
                    b.add(u.scale(tip)).add(v.scale(tip * .65)),
                    b.subtract(u.scale(tip)).add(v.scale(tip * .65))};
            hull(p, seed, opacity);
        }

        void spike(Vector3d base, double radius, double height, Vector3d lean, int seed, float opacity) {
            if (height < .01) return;
            int sides = detail > 0 ? 6 : 4;
            Vector3d[] lower = new Vector3d[sides];
            double rotation = noise(seed) * 2;
            for (int i = 0; i < sides; i++) {
                double a = i * Math.PI * 2 / sides + rotation;
                lower[i] = base.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            }
            for (int tier = 1; tier <= 3; tier++) {
                double t = tier == 1 ? .43 : tier == 2 ? .81 : 1;
                double r = radius * (tier == 1 ? .81 : tier == 2 ? .35 : .025);
                Vector3d[] upper = new Vector3d[sides];
                for (int i = 0; i < sides; i++) {
                    double a = i * Math.PI * 2 / sides + rotation + tier * .065;
                    upper[i] = base.add(lean.scale(t * t)).add(Math.cos(a) * r, height * t, Math.sin(a) * r);
                }
                for (int i = 0; i < sides; i++) face(lower[i], upper[i], upper[(i + 1) % sides],
                        lower[(i + 1) % sides], seed + tier * 7 + i, opacity);
                lower = upper;
            }
        }

        void face(Vector3d a, Vector3d b, Vector3d c, Vector3d d, int seed, float opacity) {
            if (budget.stone < 4 || opacity <= .002F) return;
            budget.stone -= 4;
            Vector3d n = cross(b.subtract(a), c.subtract(a)).normalize();
            float shade = (float)(.68 + .18 * Math.max(0, n.y) + .10 * Math.abs(n.z) + noise(seed) * .06);
            float u0 = sprite.getU(noise(seed + 4) * 3 + .05), v0 = sprite.getV(noise(seed + 7) * 3 + .05);
            float u1 = sprite.getU(15.95), v1 = sprite.getV(15.95);
            vertex(stone, matrix, normal, a, n, shade, shade, shade, opacity, u0, v1, light);
            vertex(stone, matrix, normal, b, n, shade, shade, shade, opacity, u0, v0, light);
            vertex(stone, matrix, normal, c, n, shade, shade, shade, opacity, u1, v0, light);
            vertex(stone, matrix, normal, d, n, shade, shade, shade, opacity, u1, v1, light);
        }

        void chips(Vector3d center, float elapsed, int seed, int count, double spread) {
            if (elapsed < 0 || elapsed > 14 || detail == 0 && elapsed > 8) return;
            float t = MathHelper.clamp(elapsed / 14, 0, 1), opacity = alpha * (1 - smooth((t - .55F) / .45F));
            for (int i = 0; i < count; i++) {
                double a = noise(seed + i * 29) * Math.PI * 2;
                double r = .18 + spread * (.3 + noise(seed + i * 11)) * t;
                double y = Math.sin(t * Math.PI) * (.35 + noise(seed + i * 13) * .7);
                double size = .10 + noise(seed + i * 37) * .16;
                box(center.add(Math.cos(a) * r, y + .06, Math.sin(a) * r), size, size * .65, size * .8,
                        a + t * 3, seed + i, opacity);
            }
        }

        void groundBurst(Vector3d center, double radius, float time, float opacity) {
            if (opacity <= .002F || time < 0) return;
            int count = detail > 1 ? 7 : detail > 0 ? 5 : 3;
            float crawl = smooth(time / 7);
            for (int i = 0; i < count; i++) {
                double a = i * 2.39996 + .2;
                Vector3d to = center.add(Math.cos(a) * radius * crawl, 0, Math.sin(a) * radius * crawl);
                groundBolt(center, to, i * 31 + 701, .025, opacity);
            }
        }

        void groundBolt(Vector3d a, Vector3d b, int seed, double width, float opacity) {
            if (opacity <= .002F || budget.energy < 16) return;
            int steps = detail > 0 ? 7 : 4;
            Vector3d delta = b.subtract(a), side = cross(delta, new Vector3d(0, 1, 0)).normalize();
            Vector3d previous = null;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Vector3d point = a.add(delta.scale(t)).add(side.scale((noise(seed + i * 31) - .5) * .42 * Math.sin(t * Math.PI)));
                double y = surface.height(f, origin, point.x, point.y, point.z, budget);
                if (!Double.isFinite(y)) { previous = null; continue; }
                point = new Vector3d(point.x, y, point.z);
                if (previous != null && Math.abs(point.y - previous.y) < 1.25) {
                    light(previous, point, width, opacity);
                    if (detail > 0 && i == 3) {
                        Vector3d branch = point.add(side.scale((seed % 2 == 0 ? 1 : -1) * .55));
                        double by = surface.height(f, origin, branch.x, branch.y, branch.z, budget);
                        if (Double.isFinite(by) && Math.abs(by - point.y) < .7)
                            light(point, new Vector3d(branch.x, by, branch.z), width * .55, opacity * .72F);
                    }
                }
                previous = point;
            }
        }

        void bolt(Vector3d a, Vector3d b, int seed, double width, float opacity, boolean branches) {
            if (opacity <= .002F || budget.energy < 16) return;
            Vector3d direction = b.subtract(a);
            Vector3d side = cross(direction, new Vector3d(0, 1, 0)).normalize();
            if (side.lengthSqr() < .1) side = new Vector3d(1, 0, 0);
            Vector3d other = cross(direction.normalize(), side);
            int steps = detail > 0 ? 6 : 4;
            float phase = age / 2.5F;
            int tick = MathHelper.floor(phase);
            float fraction = smooth(phase - tick);
            Vector3d last = a;
            for (int i = 1; i <= steps; i++) {
                double t = i / (double)steps, envelope = Math.sin(t * Math.PI);
                double n = MathHelper.lerp(fraction, noise(seed + i * 43 + tick * 17), noise(seed + i * 43 + (tick + 1) * 17)) - .5;
                Vector3d point = a.add(direction.scale(t)).add(side.scale(n * .28 * envelope))
                        .add(other.scale((noise(seed + i * 11) - .5) * .16 * envelope));
                light(last, point, width, opacity);
                if (branches && i == 3) {
                    Vector3d middle = point.add(side.scale(.18)).add(other.scale(.12));
                    light(point, middle, width * .6, opacity * .7F);
                    light(middle, middle.add(side.scale(.23)).add(direction.normalize().scale(.15)), width * .35, opacity * .4F);
                }
                last = point;
            }
        }

        void light(Vector3d a, Vector3d b, double width, float opacity) {
            if (budget.energy < 16 || width <= .0001 || opacity <= .002F) return;
            budget.energy -= 16;
            filament(energy, matrix, a, b, width, opacity);
        }
    }

    static void filament(IVertexBuilder out, Matrix4f matrix, Vector3d a, Vector3d b, double width, float alpha) {
        Vector3d direction = b.subtract(a).normalize();
        Vector3d side = cross(direction, new Vector3d(0, 1, 0)).normalize();
        if (side.lengthSqr() < .1) side = new Vector3d(1, 0, 0);
        Vector3d other = cross(direction, side).normalize();
        ribbon(out, matrix, a, b, side.scale(width * 2.5), .06F, .36F, 1, alpha * .22F);
        ribbon(out, matrix, a, b, other.scale(width * 2.5), .06F, .36F, 1, alpha * .18F);
        ribbon(out, matrix, a, b, side.scale(width * .45), .75F, .94F, 1, alpha);
        ribbon(out, matrix, a, b, other.scale(width * .45), .75F, .94F, 1, alpha * .8F);
    }

    private static void ribbon(IVertexBuilder out, Matrix4f matrix, Vector3d a, Vector3d b,
                               Vector3d width, float r, float g, float blue, float alpha) {
        colorVertex(out, matrix, a.subtract(width), r, g, blue, alpha);
        colorVertex(out, matrix, b.subtract(width), r, g, blue, alpha);
        colorVertex(out, matrix, b.add(width), r, g, blue, alpha);
        colorVertex(out, matrix, a.add(width), r, g, blue, alpha);
    }

    private static void colorVertex(IVertexBuilder out, Matrix4f matrix, Vector3d p, float r, float g, float b, float a) {
        out.vertex(matrix, (float)p.x, (float)p.y, (float)p.z).color(r, g, b, MathHelper.clamp(a, 0, 1)).endVertex();
    }

    static void vertex(IVertexBuilder out, Matrix4f matrix, Matrix3f normal, Vector3d p, Vector3d n,
                        float r, float g, float b, float a, float u, float v, int light) {
        out.vertex(matrix, (float)p.x, (float)p.y, (float)p.z).color(r, g, b, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, (float)n.x, (float)n.y, (float)n.z).endVertex();
    }

    static Vector3d cross(Vector3d a, Vector3d b) { return a.cross(b); }
    static float smooth(float t) { return AlchemyPose.smooth(t); }
    static double noise(int seed) { int x = seed * 0x45d9f3b; x ^= x >>> 16; x *= 0x45d9f3b; x ^= x >>> 16; return (x & 0x7fffffff) / 2147483647.0; }
    private AlchemyMesh() { super(null, null, null); }
}
