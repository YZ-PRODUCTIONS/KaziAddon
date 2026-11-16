import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ShockerBreakerVisuals;

/** Checks production geometry at fractional render times; this is not an in-game screenshot test. */
public final class ShockerBreakerVisualsTest {
    private static int checks;
    private static final int[] peak = new int[3];

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        singleImpactSoundTiming();
        singleStrikeProfile();
        timeline();
        invalidInputs();
        chargeAura();
        singleColumnColorAndFade();
        explosiveMotionAndVariants();
        sustainedRainbowAcrossHeight();
        repeatability();
        System.out.println("Shocker Breaker: " + checks + " timing, bounds, translucency and geometry checks passed. Peak quads: "
                + peak[0] / 4 + "/" + peak[1] / 4 + "/" + peak[2] / 4 + ".");
    }

    private static void singleImpactSoundTiming() {
        for (int tick = -1; tick <= ShockerBreakerVisualTiming.LIFETIME + 2; tick++) {
            check(ShockerBreakerVisualTiming.pulseIndexAt(tick) == (tick == 0 ? 0 : -1),
                    "Each warned pillar has one impact sound cue and no old-location retriggers");
        }
        check(ShockerBreakerVisualTiming.pulse(0) > .999F, "The initial impact must flash at full strength");
        check(ShockerBreakerVisualTiming.columnStrength(0) > .999F, "The initial impact must create the full column");
        check(ShockerBreakerVisualTiming.pulse(1) < .8F, "The impact flash must immediately decay");
    }

    private static void singleStrikeProfile() {
        check(ShockerBreakerVisualTiming.CHARGE_TICKS == 40,
                "Lengthening the impact must preserve the two-second charge");
        check(ShockerBreakerVisualTiming.STRIKE_TICKS == 58 && ShockerBreakerVisualTiming.FADE_TICKS == 8,
                "The energized column gains twenty more ticks while retaining its eight-tick fade");
        for (boolean enhanced : new boolean[]{false, true}) {
            check(ShockerBreakerVisualTiming.lifetime(enhanced) == 106,
                    "Both variants warn for forty ticks, sustain lightning for fifty-eight and fade for eight");
            float previousPulse = 1, previousColumn = 1, previousFade = 1;
            float previousFlicker = 1;
            boolean flickerVaries = false;
            for (float time = 0; time <= 68; time += .125F) {
                float pulse = ShockerBreakerVisualTiming.pulse(time, enhanced);
                float column = ShockerBreakerVisualTiming.columnStrength(time, enhanced);
                float fade = ShockerBreakerVisualTiming.fade(time, enhanced);
                float flicker = ShockerBreakerVisualTiming.sustainedFlicker(time);
                check(Float.isFinite(pulse) && pulse >= 0 && pulse <= previousPulse,
                        "A pillar's single impact must only decay; it cannot flash again at its old position");
                check(Float.isFinite(column) && column >= 0 && column <= previousColumn,
                        "The sustained column envelope must not introduce another impact peak");
                check(Float.isFinite(fade) && fade >= 0 && fade <= previousFade,
                        "A pillar's afterglow must fade monotonically until removal");
                check(pulse == ShockerBreakerVisualTiming.pulse(time)
                        && column == ShockerBreakerVisualTiming.columnStrength(time)
                        && fade == ShockerBreakerVisualTiming.fade(time),
                        "Both variants and direct timing callers must share the same single-impact profile");
                if (time >= 8) check(pulse == 0, "The longer discharge must not extend or repeat its initial impact flash");
                if (time < 66) {
                    check(column >= .68F && fade > 0, "The column must remain visible throughout its sustained phase and fade");
                    check(Float.isFinite(flicker) && flicker >= .71999F && flicker <= 1,
                            "Sustained electrical flicker stays bright and bounded without another detonation");
                    if (time > 38 && time < 58 && Math.abs(flicker - previousFlicker) > .015F) flickerVaries = true;
                } else check(column == 0 && fade == 0 && flicker == 0,
                        "All column energy must end at sixty-six impact ticks");
                if (time <= 58) check(fade == 1, "The extra second must be sustained lightning, not a longer fade");
                previousPulse = pulse; previousColumn = column; previousFade = fade;
                previousFlicker = flicker;
            }
            check(flickerVaries, "The column must keep electrically flickering throughout the extra second");
            for (float invalid : new float[]{-1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
                check(ShockerBreakerVisualTiming.pulse(invalid, enhanced) == 0
                        && ShockerBreakerVisualTiming.columnStrength(invalid, enhanced) == 0
                        && ShockerBreakerVisualTiming.fade(invalid, enhanced) == 0
                        && ShockerBreakerVisualTiming.sustainedFlicker(invalid) == 0,
                        "Invalid ages must never create a flash or visible tail");
            }
            for (int detail = 0; detail < 3; detail++) {
                float radius = enhanced ? 4 : 3;
                Probe initial = render(40, radius, enhanced, detail, false);
                check(initial.whiteColumn > 0 && initial.coloredColumn > 0 && initial.maxY >= 23.9,
                        "Each fresh pillar still needs its full-height white and rainbow impact");
                for (float age : new float[]{78, 86, 90, 97.75F}) {
                    Probe sustained = render(age, radius, enhanced, detail, false);
                    check(sustained.columnEnergy > 0 && sustained.whiteColumn > 0 && sustained.maxY >= 23.9,
                            "Both variants retain their full-height bright column beyond the former expiry and through the extra second");
                }
                Probe start = render(99, radius, enhanced, detail, false);
                Probe end = render(105.75F, radius, enhanced, detail, false);
                check(start.columnEnergy > 0 && end.columnEnergy > 0 && start.maxY >= 23.9 && end.maxY >= 23.9,
                        "The full-height column must fade with its ground effects, not vanish at fade start");
                check(start.vertices > 0 && end.vertices > 0 && end.alphaSum < start.alphaSum * .4,
                        "The sustained discharge must visibly fade before expiry");
                for (float age : new float[]{106, 110, 115, 125.75F})
                    check(render(age, radius, enhanced, detail, false).vertices == 0,
                            "An expired pillar must never replay the former later cosmetic strikes");
            }
        }
    }

    private static void timeline() {
        boolean reducedMiddle = false, reducedLow = false;
        for (boolean enhanced : new boolean[]{false, true}) {
            float radius = enhanced ? 4 : 3;
            for (float age = 0; age <= ShockerBreakerVisualTiming.LIFETIME + 2; age += .25F) {
                int previous = Integer.MAX_VALUE;
                for (int detail = 0; detail < 3; detail++) {
                    Probe mesh = render(age, radius, enhanced, detail, false);
                    check(mesh.valid, "Finite, legal vertex data at age " + age + " detail " + detail);
                    check(mesh.vertices % 4 == 0, "Every material submission must contain complete quads");
                    check(mesh.vertices <= 4800, "One marker exceeds the bounded 1200-quad visual budget");
                    check(mesh.maxHorizontal <= radius * 1.5 + .1, "Strike geometry exceeds its culling footprint");
                    check(mesh.minY >= -.1 && mesh.maxY <= 24.1, "Strike geometry exceeds its vertical culling bounds");
                    check(mesh.vertices <= previous, "Reduced detail must not increase geometry count");
                    if (detail == 1 && mesh.vertices < previous) reducedMiddle = true;
                    if (detail == 2 && mesh.vertices < previous) reducedLow = true;
                    previous = mesh.vertices;
                    peak[detail] = Math.max(peak[detail], mesh.vertices);
                    if (age < ShockerBreakerVisualTiming.CHARGE_TICKS || age >= ShockerBreakerVisualTiming.lifetime(enhanced))
                        check(mesh.vertices == 0, "Target lightning is only visible between strike and final fade");
                    else check(mesh.vertices > 0, "Effect disappeared before its requested visual lifetime");
                }
            }
        }
        check(reducedMiddle && reducedLow, "Both lower detail settings must actually reduce the cost");
    }

    private static void invalidInputs() {
        for (float age : new float[]{-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.MAX_VALUE}) {
            check(render(age, 3, false, 0, false).vertices == 0, "Invalid or expired ages must not render");
            Probe aura = new Probe(false); ShockerBreakerVisuals.chargeAura(aura, age, 0);
            check(aura.vertices == 0, "Invalid or expired ages must not spawn a caster aura");
        }
        for (float radius : new float[]{0, -1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
            check(render(45, radius, false, 0, false).vertices == 0, "Invalid strike radius must not render");
    }

    private static void chargeAura() {
        boolean present = false;
        for (float age = 0; age <= ShockerBreakerVisualTiming.LIFETIME; age += .25F) {
            int previous = Integer.MAX_VALUE;
            for (int detail = 0; detail < 3; detail++) {
                Probe mesh = new Probe(false);
                ShockerBreakerVisuals.chargeAura(mesh, age, detail);
                check(mesh.valid && mesh.vertices % 4 == 0, "Caster arcs must contain valid complete quads");
                check(mesh.maxHorizontal <= 3 && mesh.minY >= -.1 && mesh.maxY <= 3.5,
                        "Caster arcs escaped the intended close-body aura");
                check(mesh.vertices <= 2000, "Caster aura exceeds its 500-quad budget");
                check(mesh.vertices <= previous, "Caster aura detail reduction must reduce or preserve cost");
                previous = mesh.vertices;
                if (age >= ShockerBreakerVisualTiming.CHARGE_TICKS)
                    check(mesh.vertices == 0, "Caster arcs must stop when the attack fires");
                if (age > 4 && age < 34 && mesh.vertices > 0) present = true;
            }
        }
        check(present, "The charge must visibly crackle around the caster");
    }

    private static void singleColumnColorAndFade() {
        for (float age = ShockerBreakerVisualTiming.CHARGE_TICKS + 1;
             age < ShockerBreakerVisualTiming.CHARGE_TICKS + ShockerBreakerVisualTiming.STRIKE_TICKS; age += .5F) {
            Probe mesh = render(age, 3, false, 2, false);
            check(mesh.whiteColumn > 0, "Every bright strike phase needs its white central core");
            check(mesh.maxY >= 23.9, "The central column must span the complete lightning height");
            check(mesh.coloredColumn > 0, "The column must retain a colored electrical sheath");
        }
        float fadeStart = ShockerBreakerVisualTiming.CHARGE_TICKS + ShockerBreakerVisualTiming.STRIKE_TICKS;
        Probe start = render(fadeStart + 1, 3, false, 0, false);
        Probe end = render(ShockerBreakerVisualTiming.LIFETIME - .25F, 3, false, 0, false);
        check(start.columnEnergy > 0 && end.columnEnergy > 0 && end.columnEnergy < start.columnEnergy * .4,
                "The full column must fade gradually with the remaining ground effects");
        check(start.maxY >= 23.9 && end.maxY >= 23.9, "The column must retain its height until the fade finishes");
        check(end.alphaSum < start.alphaSum * .4, "The final effect must visibly fade, not vanish abruptly at removal");
    }

    private static void repeatability() {
        for (boolean enhanced : new boolean[]{false, true}) {
            for (float age : new float[]{40.25F, 41, 46.5F, 51.25F, 64, 83.75F, 96.5F, 105.75F}) {
                for (int detail = 0; detail < 3; detail++) {
                    Probe a = render(age, 4, enhanced, detail, true), b = render(age, 4, enhanced, detail, true);
                    check(a.vertices == b.vertices, "Identical viewer clocks must yield the same mesh size");
                    for (int i = 0; i < a.samples.size(); i++) {
                        double[] av = a.samples.get(i), bv = b.samples.get(i);
                        for (int c = 0; c < av.length; c++)
                            check(Math.abs(av[c] - bv[c]) < 1e-6, "Identical visual clocks must not randomly change the effect");
                    }
                }
            }
        }
    }

    private static void sustainedRainbowAcrossHeight() {
        for (boolean enhanced : new boolean[]{false, true}) {
            for (int detail = 0; detail < 3; detail++) {
                Probe previous = null;
                for (float strikeAge : new float[]{9.25F, 14.75F, 21.25F, 28.75F, 36.25F, 44.25F, 50.75F, 57.25F}) {
                    Probe mesh = render(ShockerBreakerVisualTiming.CHARGE_TICKS + strikeAge,
                            enhanced ? 4 : 3, enhanced, detail, false);
                    int allHues = 0;
                    String sample = " (EX=" + enhanced + ", detail=" + detail + ", strikeAge=" + strikeAge + ")";
                    for (int region = 0; region < 3; region++) {
                        check(Integer.bitCount(mesh.heightHueMask[region]) >= 2,
                                "Every height region needs concurrent saturated rainbow hues" + sample + ", region=" + region);
                        check(mesh.saturatedHeightEnergy[region] > mesh.heightEnergy[region] * .20,
                                "The sustained rainbow must remain visible beside the white core" + sample + ", region=" + region);
                        allHues |= mesh.heightHueMask[region];
                        if (previous != null) {
                            check(mesh.heightMotion[region] != previous.heightMotion[region],
                                    "Electrical geometry must keep moving throughout the height after the initial impact"
                                            + sample + ", region=" + region);
                            check(mesh.heightColors[region] != previous.heightColors[region],
                                    "Sustained rainbow colors must animate in every height region" + sample + ", region=" + region);
                        }
                    }
                    check(Integer.bitCount(allHues) >= 4,
                            "A sustained pillar must display a broad rainbow simultaneously" + sample);
                    previous = mesh;
                }
            }
        }
    }

    private static int saturatedHue(float r, float g, float b) {
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double chroma = max - min;
        if (max < .75 || chroma < .65) return -1;
        double hue = max == r ? (g - b) / chroma : max == g ? 2 + (b - r) / chroma : 4 + (r - g) / chroma;
        if (hue < 0) hue += 6;
        return (int) Math.floor(hue + .5) % 6;
    }

    private static void explosiveMotionAndVariants() {
        for (int detail = 0; detail < 3; detail++) {
            Probe strike = render(40, 3, false, detail, true);
            Probe expanding = render(43, 3, false, detail, true);
            check(expanding.pressureFrontRadius > strike.pressureFrontRadius + .3,
                    "Each detonation's feathered pressure front must travel outward after its flash");
            check(strike.coreWidth > expanding.coreWidth * 1.25,
                    "The lightning trunk must contract visibly between violent peaks");
            Probe normal = render(42, 4, false, detail, true);
            Probe enhanced = render(42, 4, true, detail, true);
            check(enhanced.vertices > normal.vertices,
                    "EX must gain additional eruption geometry, not only a larger radius");
            check(enhanced.alphaSum > normal.alphaSum,
                    "EX must retain a stronger discharge while sharing the same finite bounds");
            double motion = 0;
            Probe next = render(42.5F, 4, true, detail, true);
            for (int i = 0; i < Math.min(next.samples.size(), enhanced.samples.size()); i++) {
                double[] a = next.samples.get(i), b = enhanced.samples.get(i);
                motion += Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
            }
            check(motion > 20, "Discharge forks, eruption sheets and sparks must animate between render samples");
        }
    }

    private static Probe render(float age, float radius, boolean enhanced, int detail, boolean retain) {
        Probe out = new Probe(retain);
        ShockerBreakerVisuals.render(out, age, radius, enhanced, detail);
        return out;
    }

    private static final class Probe implements AsrielVisualGeometry.Sink {
        int vertices, whiteColumn, coloredColumn;
        double maxHorizontal, minY, maxY, alphaSum, columnEnergy, groundEnergy, red, green, blue;
        double pressureFrontRadius, coreWidth;
        final int[] heightHueMask = new int[3];
        final double[] heightEnergy = new double[3], saturatedHeightEnergy = new double[3];
        final long[] heightMotion = {17, 17, 17}, heightColors = {17, 17, 17};
        boolean valid = true;
        final List<double[]> samples;
        Probe(boolean retain) { samples = retain ? new ArrayList<>() : null; }
        public void vertex(double x, double y, double z, float r, float g, float b, float a) {
            vertices++;
            valid &= Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                    && Float.isFinite(r) && Float.isFinite(g) && Float.isFinite(b) && Float.isFinite(a)
                    && r >= 0 && r <= 1.00001 && g >= 0 && g <= 1.00001
                    && b >= 0 && b <= 1.00001 && a >= 0 && a <= 1.00001;
            maxHorizontal = Math.max(maxHorizontal, Math.hypot(x, z));
            if (Math.abs(y - .04) < .000001) pressureFrontRadius = Math.max(pressureFrontRadius, Math.hypot(x, z));
            if (Math.abs(y - 24) < .000001 && a > .4 && r > .9 && g > .9 && b > .9)
                coreWidth = Math.max(coreWidth, Math.hypot(x, z));
            minY = Math.min(minY, y); maxY = Math.max(maxY, y); alphaSum += a;
            if (y > 6) columnEnergy += a;
            else groundEnergy += a;
            if (y > 6 && a > .05) {
                if (r > .9 && g > .9 && b > .9) whiteColumn++;
                else { coloredColumn++; red += r; green += g; blue += b; }
            }
            // Observe the body independently of the ground explosion and top cap.
            if (y >= 2 && y < 22 && a > .05) {
                int region = Math.min(2, (int) ((y - 2) * 3 / 20));
                heightEnergy[region] += a;
                int hue = saturatedHue(r, g, b);
                if (hue >= 0) {
                    heightHueMask[region] |= 1 << hue;
                    saturatedHeightEnergy[region] += a;
                }
                // Quantization ignores numerically tiny changes while allowing any geometry topology.
                heightMotion[region] = heightMotion[region] * 31 + Math.round(x * 20);
                heightMotion[region] = heightMotion[region] * 31 + Math.round(y * 20);
                heightMotion[region] = heightMotion[region] * 31 + Math.round(z * 20);
                heightColors[region] = heightColors[region] * 31 + Math.round(r * 255);
                heightColors[region] = heightColors[region] * 31 + Math.round(g * 255);
                heightColors[region] = heightColors[region] * 31 + Math.round(b * 255);
            }
            if (samples != null) samples.add(new double[]{x, y, z, r, g, b, a});
        }
    }
}
