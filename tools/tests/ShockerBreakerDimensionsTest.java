import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerDimensions;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ShockerBreakerVisuals;

/** Verifies that the visuals grow uniformly by 50% without changing gameplay dimensions. */
public final class ShockerBreakerDimensionsTest {
    private static int checks;

    public static void main(String[] args) {
        requiredDimensions();
        for (boolean enhanced : new boolean[]{false, true}) for (int detail = 0; detail < 3; detail++) {
            for (float age : new float[]{40, 44, 47.75F, 48, 53, 55.75F, 56, 65, 74.75F,
                    77.75F, 78, 82, 85.75F, 86, 97, 105.75F}) {
                Probe previous = new Probe(4.5), world = new Probe(ShockerBreakerDimensions.VISUAL_SCALE);
                float originalRadius = ShockerBreakerDimensions.originalRadius(enhanced);
                ShockerBreakerVisuals.render(previous, age, originalRadius, enhanced, detail);
                ShockerBreakerVisuals.render(world, age, originalRadius, enhanced, detail);
                equivalent(previous, world);
                check(world.vertices.size() <= 4800, "Resizing must retain the existing 1200-quad budget");
                check(world.maxHorizontal <= ShockerBreakerDimensions.visualRadius(enhanced) * 1.5 + .1,
                        "The complete impact must fit the renderer's horizontal culling volume");
                check(world.maxY <= ShockerBreakerDimensions.CULL_HEIGHT && world.minY >= -.9,
                        "The effect must remain inside its vertical culling volume");
                if (age >= ShockerBreakerVisualTiming.lifetime(enhanced)) check(world.vertices.isEmpty(),
                        "Neither variant may replay strikes after its single impact and fade");
                if (age >= 56 && age < 78) check(world.hasWhiteTop,
                        "The white lightning core must persist throughout the additional 1.5 seconds");
                if (age == 40) {
                    check(world.hasWhiteTop && world.maxY >= ShockerBreakerDimensions.VISUAL_HEIGHT
                                    && world.maxY <= ShockerBreakerDimensions.VISUAL_HEIGHT
                                    + .1 * ShockerBreakerDimensions.VISUAL_SCALE,
                            "Both variants must have a 162-block core with only filament thickness beyond it");
                    check(world.maxHorizontal > ShockerBreakerDimensions.damageRadius(enhanced),
                            "The full flare must remain broader than the damaging core");
                }
            }
        }
        for (int detail = 0; detail < 3; detail++) for (float age : new float[]{5, 13, 24, 33, 40}) {
            Probe previous = new Probe(4.5), world = new Probe(ShockerBreakerDimensions.VISUAL_SCALE);
            ShockerBreakerVisuals.chargeAura(previous, age, detail);
            ShockerBreakerVisuals.chargeAura(world, age, detail);
            equivalent(previous, world);
            check(world.maxHorizontal <= 20.25 && world.maxY <= 27 && world.minY >= -.675,
                    "The enlarged caster aura must fit its separate culling box");
            if (age == 40) check(world.vertices.isEmpty(), "Resizing the caster aura must not extend its lifetime");
        }
        System.out.println("Shocker Breaker size: " + checks + " production-geometry, culling and damage-dimension checks passed.");
    }

    private static void requiredDimensions() {
        near(ShockerBreakerDimensions.SCALE, 3, "Damage and warning scale must remain unchanged");
        near(ShockerBreakerDimensions.VISUAL_SCALE, 6.75, "Visual scale must be 50% larger than the previous scale of 4.5");
        near(ShockerBreakerDimensions.damageRadius(false), 9, "Normal damage radius must be 9 blocks");
        near(ShockerBreakerDimensions.damageRadius(true), 12, "EX damage radius must be 12 blocks");
        near(ShockerBreakerDimensions.DAMAGE_HEIGHT, 18, "Damage height must remain 18 blocks");
        near(ShockerBreakerDimensions.DAMAGE_BELOW, .9, "Below-ground damage allowance must remain unchanged");
        near(ShockerBreakerDimensions.VISUAL_HEIGHT, 162, "Visual height must grow from 108 to 162 blocks");
        near(ShockerBreakerDimensions.CULL_HEIGHT, 175.5, "Culling height must fit the enlarged visual mesh");
        near(ShockerBreakerDimensions.visualRadius(false), 20.25, "Normal visual radius must grow from 13.5 to 20.25 blocks");
        near(ShockerBreakerDimensions.visualRadius(true), 27, "EX visual radius must grow from 18 to 27 blocks");
        check(ShockerBreakerDimensions.CULL_HEIGHT > ShockerBreakerDimensions.VISUAL_HEIGHT,
                "Culling must retain space around the top of the lightning");
        for (boolean enhanced : new boolean[]{false, true}) {
            double original = ShockerBreakerDimensions.originalRadius(enhanced);
            double radius = ShockerBreakerDimensions.damageRadius(enhanced);
            near((radius * 2) / (original * 2), 3, "Damage diameter must retain its previous scale along both horizontal axes");
            near(ShockerBreakerDimensions.visualRadius(enhanced) / radius, 2.25,
                    "Each variant's visual radius must grow independently of the unchanged damage radius");
        }
    }

    private static void equivalent(Probe previous, Probe world) {
        check(previous.vertices.size() == world.vertices.size(), "Size must not add particles or geometry");
        for (int i = 0; i < previous.vertices.size(); i++) {
            double[] before = previous.vertices.get(i), after = world.vertices.get(i);
            for (int axis = 0; axis < 3; axis++) near(after[axis], before[axis] * 1.5,
                    "Every vertex must grow by 50% uniformly across horizontal and vertical axes");
            for (int value = 3; value < 7; value++) near(after[value], before[value],
                    "Size must preserve original colors and transparency");
        }
    }

    private static void near(double actual, double expected, String reason) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) <= 1e-7, reason);
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static final class Probe implements AsrielVisualGeometry.Sink {
        final double scale;
        final List<double[]> vertices = new ArrayList<>();
        double minY, maxY, maxHorizontal;
        boolean hasWhiteTop;
        Probe(double scale) { this.scale = scale; }
        @Override public void vertex(double x, double y, double z, float r, float g, float b, float a) {
            x *= scale; y *= scale; z *= scale;
            vertices.add(new double[]{x, y, z, r, g, b, a});
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            maxHorizontal = Math.max(maxHorizontal, Math.hypot(x, z));
            if (Math.abs(y - ShockerBreakerDimensions.VISUAL_HEIGHT) < 1e-7
                    && r > .9F && g > .9F && b > .9F && a > .1F) hasWhiteTop = true;
        }
    }
}
