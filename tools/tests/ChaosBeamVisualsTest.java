import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterPattern;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ChaosBeamVisuals;
import net.kazi.kazimod.renderers.entities.asriel.ChaosCannonVisuals;

/** Executes the same cannon and beam entry point used by the renderer, without Minecraft. */
public final class ChaosBeamVisualsTest {
    private static final int LIFETIME = ChaosBusterPattern.BEAM_LIFETIME;
    private static int samples;
    private static long vertices;
    private static final int[] peaks = new int[3];

    public static void main(String[] args) {
        invalidAndExpired();
        for (boolean enhanced : new boolean[]{false, true}) {
            double baseRadius = enhanced ? ChaosBusterPattern.BLASTER_BEAM_RADIUS : ChaosBusterPattern.BEAM_RADIUS;
            for (float length : new float[]{.05F, .5F, 8F, 48F, 96F}) {
                for (float age = 0; age < LIFETIME; age += .25F) {
                    float radius = (float) (baseRadius * ChaosBusterPattern.beamRadiusScale(age, LIFETIME));
                    int previous = Integer.MAX_VALUE;
                    for (int detail = 0; detail < 3; detail++) {
                        Probe p = probe(age, LIFETIME, length, radius, baseRadius, enhanced, detail);
                        require(p.count % 4 == 0, "Every face must be a complete quad");
                        require(p.count > 0, "An active beam must keep visible geometry until its lifetime ends");
                        require(p.count < previous, "Lower detail must reduce the total cannon and beam cost");
                        previous = p.count;
                        peaks[detail] = Math.max(peaks[detail], p.count / 4);
                        if (age <= LIFETIME - 6) {
                            require(p.whiteOrigin && p.whiteEnd, "The white-hot beam must run from muzzle to its exact endpoint");
                            require(p.spectral, "Colored energy must remain visible alongside the white core");
                        }
                        samples++;
                    }
                }
            }
        }
        require(peaks[0] <= 1600 && peaks[1] <= 1000 && peaks[2] <= 550,
                "LOD budgets include cannon, forks, bursts and beam together");
        motionAndVariants();
        rainbowEnergyAlongBeam();
        doubledBeamAndUnscaledCannon();
        System.out.println("Chaos beam visual checks: " + samples + " animated radius/length/variant/LOD samples, "
                + vertices + " finite bounded vertices; peak quads " + peaks[0] + "/" + peaks[1] + "/" + peaks[2] + ".");
    }

    private static void invalidAndExpired() {
        float[][] invalid = {{-1, LIFETIME, 48, 3}, {LIFETIME, LIFETIME, 48, 3}, {100, LIFETIME, 48, 3}, {0, 0, 48, 3},
                {0, LIFETIME, 0, 3}, {0, LIFETIME, 48, 0}, {Float.NaN, LIFETIME, 48, 3}, {0, Float.NaN, 48, 3},
                {0, LIFETIME, Float.POSITIVE_INFINITY, 3}, {0, LIFETIME, 48, Float.NaN}};
        for (float[] input : invalid) {
            Probe p = probe(input[0], input[1], input[2], input[3], 4, false, 0);
            require(p.count == 0, "Invalid, zero-size and expired beams must emit neither cannon nor effects");
        }
        // Very small clipped beams still have a bounded cannon and a short blast, rather than NaN tangents.
        probe(3, LIFETIME, .002F, .002F, .002, true, 0);
    }

    private static void motionAndVariants() {
        Probe first = probe(2, LIFETIME, 48, 3.2F, 3.2, false, 0);
        Probe later = probe(3, LIFETIME, 48, 3.2F, 3.2, false, 0);
        Probe ex = probe(2, LIFETIME, 48, 3.2F, 3.2, true, 0);
        require(first.fingerprint != later.fingerprint, "Pulses and ejecta must animate between ticks");
        require(first.fingerprint != ex.fingerprint && ex.count > first.count,
                "Blaster must have a distinct, denser violent silhouette than Buster");
        require(first.maxRadial > 3.2 * 1.5, "Detonation fronts must visibly expand beyond the narrow core");
        Probe late = probe(LIFETIME - .5F, LIFETIME, 48, (float) (3.2 * ChaosBusterPattern.beamRadiusScale(LIFETIME - .5F, LIFETIME)), 3.2, false, 0);
        require(late.maxBeamRadial < first.maxBeamRadial * .15,
                "Late beam geometry must contract with the authoritative supplied damage radius");
        require(late.alphaTotal < first.alphaTotal * .2,
                "The tail must fade rather than leave bright shell fragments at removal");
    }

    private static void doubledBeamAndUnscaledCannon() {
        require(ChaosBusterPattern.BEAM_RANGE == 96D, "Both finishing beams must have ninety-six blocks of maximum range");
        require(ChaosBeamVisuals.VISUAL_RADIUS_SCALE == 2F, "Only the beam's cosmetic radius is doubled");
        for (boolean enhanced : new boolean[]{false, true}) {
            float damageRadius = (float) (enhanced ? ChaosBusterPattern.BLASTER_BEAM_RADIUS : ChaosBusterPattern.BEAM_RADIUS);
            for (int detail = 0; detail < 3; detail++) {
                Probe large = probe(3, LIFETIME, 96, damageRadius, damageRadius, enhanced, detail);
                // Half the input compensates for the new visual multiplier and reproduces the previous beam width.
                Probe previousWidth = probe(3, LIFETIME, 96, damageRadius / 2, damageRadius, enhanced, detail);
                require(large.maxBeamRadial > damageRadius * 2,
                        "The middle of the beam must visibly exceed its unchanged damage radius at every detail level");
                require(Math.abs(large.maxBeamRadial / previousWidth.maxBeamRadial - 2) < .025,
                        "The beam body must be twice its previous width, independently of the longer range");
                require(large.whiteEnd, "The enlarged beam core must reach the exact ninety-six-block endpoint");
                for (float age : new float[]{.25F, 3, 12, 26, 49, 58, LIFETIME - .5F}) {
                    float opacity = AsrielVisualGeometry.clamp((age + .5F) / 2)
                            * AsrielVisualGeometry.clamp((LIFETIME - age) / 6);
                    List<double[]> cannon = new ArrayList<>();
                    ChaosCannonVisuals.cannon((x,y,z,r,g,b,a) -> cannon.add(new double[]{x,y,z,r,g,b,a}),
                            age, 1, opacity, enhanced, detail, true);
                    int[] seen = {0};
                    float radius = damageRadius * (float) ChaosBusterPattern.beamRadiusScale(age, LIFETIME);
                    AsrielVisualGeometry.rainbowBeam((x,y,z,r,g,b,a) -> {
                        int index = seen[0]++;
                        if (index < cannon.size()) {
                            double[] expected = cannon.get(index), actual = {x,y,z,r,g,b,a};
                            for (int value = 0; value < actual.length; value++)
                                require(actual[value] == expected[value],
                                        "Beam enlargement must preserve every cannon coordinate, tint and alpha: "
                                                + actual[value] + " != " + expected[value]);
                        }
                    }, age, LIFETIME, 96, radius, enhanced, detail);
                    require(seen[0] > cannon.size(), "The unchanged cannon must be followed by the longer beam geometry");
                }
            }
        }
    }

    /** Color and motion must be present along the attack, not only hidden in its endpoint burst. */
    private static void rainbowEnergyAlongBeam() {
        for (boolean enhanced : new boolean[]{false, true}) {
            float radius = (float) (enhanced ? ChaosBusterPattern.BLASTER_BEAM_RADIUS : ChaosBusterPattern.BEAM_RADIUS);
            for (int detail = 0; detail < 3; detail++) {
                double[] minimumWidth = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
                double[] maximumWidth = new double[3];
                double[] minimumEnergy = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
                double[] maximumEnergy = new double[3];
                double[] maximumRedistribution = new double[3];
                double[][] previousProfile = new double[3][];
                long[] priorFingerprints = new long[3];
                for (float age : new float[]{1.5F, 2.5F, 3.5F, 5.5F, 7.5F, 9.5F, 26, 40, 49.5F}) {
                    Probe p = probe(age, LIFETIME, 96, radius, radius, enhanced, detail);
                    int combinedHues = 0;
                    double colorEnergy = 0, allEnergy = 0;
                    for (int region = 0; region < 3; region++) {
                        combinedHues |= p.bodyHueMask[region];
                        colorEnergy += p.saturatedBodyEnergy[region];
                        allEnergy += p.bodyEnergy[region];
                        require(Integer.bitCount(p.bodyHueMask[region]) >= 2,
                                "Every third of the beam needs visible saturated color, including low detail");
                        require(p.bodyFingerprint[region] != priorFingerprints[region],
                                "Energy must animate along every third of the beam, not just at its ends");
                        priorFingerprints[region] = p.bodyFingerprint[region];
                        minimumWidth[region] = Math.min(minimumWidth[region], p.bodyWidth[region]);
                        maximumWidth[region] = Math.max(maximumWidth[region], p.bodyWidth[region]);
                        minimumEnergy[region] = Math.min(minimumEnergy[region], p.bodyEnergy[region]);
                        maximumEnergy[region] = Math.max(maximumEnergy[region], p.bodyEnergy[region]);
                        if (previousProfile[region] != null) {
                            double difference = 0, combined = 0;
                            for (int section = 0; section < p.bodyLightProfile[region].length; section++) {
                                double current = p.bodyLightProfile[region][section], prior = previousProfile[region][section];
                                difference += Math.abs(current - prior);
                                combined += current + prior;
                            }
                            maximumRedistribution[region] = Math.max(maximumRedistribution[region], 2 * difference / combined);
                        }
                        previousProfile[region] = p.bodyLightProfile[region];
                    }
                    require(Integer.bitCount(combinedHues) >= 4,
                            "At least four strong rainbow hue families must appear together along each beam");
                    require(colorEnergy > allEnergy * .35,
                            "Saturated rainbow surfaces must carry a substantial share of the visible beam energy");
                }
                for (int region = 0; region < 3; region++) {
                    require(maximumWidth[region] > minimumWidth[region] * 1.08
                                    || maximumEnergy[region] > minimumEnergy[region] * 1.20
                                    || maximumRedistribution[region] > .15,
                            "Traveling eruptions must change local light distribution, width or intensity throughout the beam"
                                    + " (enhanced=" + enhanced + ", detail=" + detail + ", third=" + region
                                    + ", width=" + minimumWidth[region] + ".." + maximumWidth[region]
                                    + ", energy=" + minimumEnergy[region] + ".." + maximumEnergy[region]
                                    + ", local redistribution=" + maximumRedistribution[region] + ")");
                }
                Probe expired = probe(LIFETIME, LIFETIME, 96, radius, radius, enhanced, detail);
                require(expired.count == 0, "No colored eruptions may survive beam expiry");
            }
        }
    }

    private static int hueFamily(float r, float g, float b) {
        float maximum = Math.max(r, Math.max(g, b)), minimum = Math.min(r, Math.min(g, b));
        float span = maximum - minimum;
        if (span < .65F || maximum < .75F) return -1;
        double hue = maximum == r ? (g - b) / span : maximum == g ? 2 + (b - r) / span : 4 + (r - g) / span;
        if (hue < 0) hue += 6;
        return Math.min(5, (int) hue);
    }

    private static Probe probe(float age, float duration, float length, float radius, double baseRadius,
                               boolean enhanced, int detail) {
        Probe p = new Probe(length, ChaosBeamVisuals.cullingRadius(baseRadius));
        AsrielVisualGeometry.rainbowBeam(p, age, duration, length, radius, enhanced, detail);
        return p;
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    private static final class Probe implements AsrielVisualGeometry.Sink {
        final double length, padding;
        int count;
        long fingerprint = 17;
        boolean whiteOrigin, whiteEnd, spectral;
        double maxRadial, maxBeamRadial, alphaTotal;
        final int[] bodyHueMask = new int[3];
        final long[] bodyFingerprint = {17, 17, 17};
        final double[] bodyWidth = new double[3], bodyEnergy = new double[3], saturatedBodyEnergy = new double[3];
        final double[][] bodyLightProfile = new double[3][8];
        Probe(double length, double padding) { this.length = length; this.padding = padding; }
        @Override public void vertex(double x, double y, double z, float r, float g, float b, float a) {
            require(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                    && Float.isFinite(r) && Float.isFinite(g) && Float.isFinite(b) && Float.isFinite(a), "All mesh data must be finite");
            require(r >= 0 && r <= 1 && g >= 0 && g <= 1 && b >= 0 && b <= 1 && a >= 0 && a <= 1,
                    "Every tint and alpha must be bounded");
            double dz = z < 0 ? z : z > length ? z - length : 0;
            require(Math.sqrt(x * x + y * y + dz * dz) <= padding + 1e-6,
                    "Endpoint-centered culling must contain the complete mesh in every orientation");
            if (r > .9F && g > .9F && b > .9F && a > .02F) {
                if (Math.abs(z) < 1e-6) whiteOrigin = true;
                if (Math.abs(z - length) < 1e-6) whiteEnd = true;
            }
            if (a > .03F && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > .4F) spectral = true;
            double radial = Math.hypot(x, y);
            maxRadial = Math.max(maxRadial, radial);
            if (z > length * .2 && z < length * .8) maxBeamRadial = Math.max(maxBeamRadial, radial);
            if (z > length * .1 && z < length * .9 && a > .08F) {
                double position = (z / length - .1) / .8 * 3;
                int region = Math.min(2, (int) position);
                bodyWidth[region] = Math.max(bodyWidth[region], radial);
                bodyEnergy[region] += a;
                bodyLightProfile[region][Math.min(7, (int) ((position - region) * 8))] += a;
                int hue = hueFamily(r, g, b);
                if (hue >= 0) {
                    bodyHueMask[region] |= 1 << hue;
                    saturatedBodyEnergy[region] += a;
                }
                bodyFingerprint[region] = bodyFingerprint[region] * 31 + Double.doubleToLongBits(x);
                bodyFingerprint[region] = bodyFingerprint[region] * 31 + Double.doubleToLongBits(y);
                bodyFingerprint[region] = bodyFingerprint[region] * 31 + Double.doubleToLongBits(z);
            }
            fingerprint = fingerprint * 31 + Double.doubleToLongBits(x);
            fingerprint = fingerprint * 31 + Double.doubleToLongBits(y);
            fingerprint = fingerprint * 31 + Double.doubleToLongBits(z);
            count++; vertices++; alphaTotal += a;
        }
    }
}
