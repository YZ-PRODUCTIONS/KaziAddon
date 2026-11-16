import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningMath;

public final class YagiAwakeningMathTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(!YagiAwakeningMath.enhanced(20, 20), "Full health uses the normal move");
        check(!YagiAwakeningMath.enhanced(10, 20), "Exactly 50 percent remains normal");
        check(YagiAwakeningMath.enhanced(9.99F, 20), "Strictly below 50 percent enhances");
        check(YagiAwakeningMath.enhanced(99, 200), "Threshold scales with maximum health");
        check(!YagiAwakeningMath.enhanced(101, 200), "Above threshold remains normal");
        check(!YagiAwakeningMath.enhanced(0, 0), "Invalid maximum health cannot enhance");
        check(!YagiAwakeningMath.enhanced(Float.NaN, 20), "NaN health is rejected");
        check(!YagiAwakeningMath.enhanced(20, Float.POSITIVE_INFINITY), "Infinite maximum is rejected");
        check(YagiAwakeningMath.usesChaosBeam(2, 20), "Exactly 10 percent selects Chaos Beam");
        check(YagiAwakeningMath.usesChaosBeam(Math.nextDown(2F), 20), "Below 10 percent selects Chaos Beam");
        check(!YagiAwakeningMath.usesChaosBeam(Math.nextUp(2F), 20), "Above 10 percent selects Shocker Breaker");
        check(!YagiAwakeningMath.usesChaosBeam(9, 20), "The general low-health form does not select Chaos Beam");
        check(YagiAwakeningMath.usesChaosBeam(20, 200), "Chaos Beam scales with maximum health");
        check(!YagiAwakeningMath.usesChaosBeam(15, 100) && YagiAwakeningMath.usesChaosBeam(15, 150),
                "Changing maximum health changes the selected form even at the same current HP");
        check(!YagiAwakeningMath.usesChaosBeam(Math.nextUp(2.5F), 25), "A percentage above ten must not round down");
        check(!YagiAwakeningMath.usesChaosBeam(2.7F, 27), "Rounding the HP cutoff up must not grant the beam early");
        check(YagiAwakeningMath.insideBeam(0, 0, 24, 0, 0, 1, 48, 2.5, .3), "Forward beam hits its center");
        check(!YagiAwakeningMath.insideBeam(0, 0, -1, 0, 0, 1, 48, 2.5, .3), "No targets behind the origin");
        check(!YagiAwakeningMath.insideBeam(0, 0, 48.1, 0, 0, 1, 48, 2.5, .3), "No targets beyond the clipped end");
        check(YagiAwakeningMath.insideBeam(2.79, 0, 20, 0, 0, 1, 48, 2.5, .3), "Target width intersects beam edge");
        check(!YagiAwakeningMath.insideBeam(2.81, 0, 20, 0, 0, 1, 48, 2.5, .3), "Targets beyond edge are excluded");
        check(YagiAwakeningMath.insideBeam(0, 20, 1, 0, 1, 0, 48, 2.5, .3), "Vertical beams work");
        double d = Math.sqrt(.5);
        check(YagiAwakeningMath.insideBeam(20, 0, 20, d, 0, d, 48, 2.5, .3), "Diagonal beams work");
        check(!YagiAwakeningMath.insideBeam(20, 0, -20, d, 0, d, 48, 2.5, .3), "Off-axis diagonal targets miss");
        check(!YagiAwakeningMath.insideBeam(0, 0, 15, 0, 0, 1, 10, 2.5, .3), "Clipping at a wall shortens damage reach");
        check(!YagiAwakeningMath.insideBeam(Double.NaN, 0, 10, 0, 0, 1, 48, 2.5, .3), "Invalid geometry is rejected");
        System.out.println("Yagi awakening: " + checks + " health-threshold and directional-hitbox checks passed.");
    }
}
