import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamPattern;
import net.kazi.kazimod.renderers.entities.asriel.ChaosBeamAttackVisuals;

/** Combat boundaries and the production mesh throughout charge, expansion, clipping and fade. */
public final class ChaosBeamAttackTest {
    private static long checks;
    private static final int[] peaks = new int[3];
    public static void main(String[] args) {
        require(ChaosBeamPattern.CHARGE_TICKS == 40 && ChaosBeamPattern.FIRE_TICKS == 240, "Two-second charge and twelve-second firing");
        int pulses = 0;
        for (int age = -10; age < 260; age++) if (ChaosBeamPattern.damageTick(age)) pulses++;
        require(pulses == 12 && pulses * ChaosBeamPattern.DAMAGE == 240, "Twelve pulses deal 240 base damage per target");
        for (int age = 0; age < 240; age++) {
            for (double length : new double[]{.1, 5, 48, 100, 150}) {
                for (double along : new double[]{0, length * .5, length}) {
                    double radius = ChaosBeamPattern.radiusAt(along, age);
                    require(ChaosBeamPattern.contains(along, radius*radius, 0, length, age), "Targets on the cone boundary are inside");
                    require(!ChaosBeamPattern.contains(along, (radius+.01)*(radius+.01), 0, length, age), "Targets outside the widening cone are excluded");
                }
                require(!ChaosBeamPattern.contains(length+.01, 0, .5, length, age), "Walls clip the damage length");
                require(!ChaosBeamPattern.contains(-.01, 0, .5, length, age), "Do not hit behind the caster");
            }
        }
        require(ChaosBeamPattern.CORE_RADIUS == 8 && ChaosBeamPattern.RANGE == 150, "Beam reaches 150 blocks with the same muzzle radius");
        require(ChaosBeamPattern.endRadius(0) == 32 && ChaosBeamPattern.endRadius(240) == 80, "Doubled beam widens throughout all twelve seconds");
        for (int age = 0; age < 240; age++)
            require(ChaosBeamPattern.endRadius(age + 1) > ChaosBeamPattern.endRadius(age), "Beam keeps gradually widening until firing ends");
        require(ChaosBeamPattern.endRadius(120) == 56 && ChaosBeamPattern.endRadius(150) < 66,
                "Expansion remains gradual instead of reaching maximum size early");
        require(!ChaosBeamPattern.contains(5, 0, .5, 150, 240), "Expired beam cannot damage");
        require(!ChaosBeamPattern.contains(Double.NaN, 0, .5, 150, 5), "Reject invalid hit coordinates");
        for (boolean firing : new boolean[]{false, true}) {
            for (float age = 0; age <= (firing ? 240 : 45); age += 2.5F) {
                for (double length : new double[]{0, .05, 12, 48, 100, 150}) {
                    int previous = Integer.MAX_VALUE;
                    for (int detail = 0; detail < 3; detail++) {
                        final int[] vertices = {0};
                        final double[] energy = {0};
                        ChaosBeamAttackVisuals.render((x,y,z,r,g,b,a) -> {
                            require(Double.isFinite(x+y+z) && Float.isFinite(r+g+b+a), "Finite geometry");
                            require(Math.abs(x) <= ChaosBeamAttackVisuals.cullingRadius() && Math.abs(y) <= ChaosBeamAttackVisuals.cullingRadius(), "Culling encloses every vertex");
                            require(z >= -10 && z <= Math.max(10,length), "Effects stay within clipped length and enlarged core bounds");
                            require(r>=0&&r<=1&&g>=0&&g<=1&&b>=0&&b<=1&&a>=0&&a<=1, "Bounded colors and opacity");
                            vertices[0]++; energy[0] += a;
                        },age,firing,length,detail);
                        require(vertices[0]%4 == 0, "Complete quads");
                        require(vertices[0] <= previous, "Lower detail never increases geometry");
                        previous = vertices[0]; peaks[detail]=Math.max(peaks[detail],vertices[0]/4);
                        if (firing && age == 240) require(vertices[0] == 0, "No lingering effect after firing");
                        if (age > 0 && age < (firing ? 239 : 40)) require(energy[0] > 0, "Visible charge and beam");
                    }
                }
            }
        }
        require(peaks[0] <= 1200 && peaks[1] <= 650 && peaks[2] <= 220, "Bounded low-end rendering cost");
        require(ChaosBeamPattern.fade(239) < .02F && ChaosBeamPattern.fade(240) == 0, "Smooth final fade");
        System.out.println("Chaos Beam: " + checks + " checks passed; peak quads " + peaks[0]+"/"+peaks[1]+"/"+peaks[2]+".");
    }
    private static void require(boolean pass, String message) { checks++; if (!pass) throw new AssertionError(message); }
}
