import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ChaosCannonVisuals;

/** Checks the production cannon/charge meshes across their actual animation states. */
public final class ChaosCannonVisualsTest {
    private static int samples;
    private static long vertices;
    private static final int[] cannonPeak = new int[3], chargePeak = new int[3];

    public static void main(String[] args) {
        for (boolean enhanced : new boolean[]{false, true}) {
            for (float age : new float[]{0, 6.5F, 10, 19.5F, 40, 151, 153.5F, 156, 166, 184.75F}) {
                for (float fill : new float[]{0, .25F, .6F, 1}) {
                    int previousCannon = Integer.MAX_VALUE, previousCharge = Integer.MAX_VALUE;
                    for (int detail = 0; detail < 3; detail++) {
                        Probe cannon = new Probe(), charge = new Probe();
                        AsrielVisualGeometry.busterCannon(cannon, age, fill, enhanced, detail);
                        AsrielVisualGeometry.busterCharge(charge, age, fill, 3.2F, enhanced, detail);
                        require(cannon.count > 0 && cannon.count % 4 == 0, "A cannon must have complete visible faces");
                        require(charge.count % 4 == 0, "Charge streamers must have complete faces");
                        require(cannon.count < previousCannon, "Cannon detail levels must reduce geometry");
                        if (charge.count > 0) require(charge.count < previousCharge, "Charge detail levels must reduce geometry");
                        previousCannon = cannon.count; previousCharge = charge.count;
                        cannonPeak[detail] = Math.max(cannonPeak[detail], cannon.count / 4);
                        chargePeak[detail] = Math.max(chargePeak[detail], charge.count / 4);
                        require(cannon.minZ < -2 && cannon.maxZ > -.2, "The cannon must span its rear body and anchored muzzle");
                        require(cannon.maxX - cannon.minX > 1 && cannon.maxY - cannon.minY > 1,
                                "The sculpted body must retain a readable three-dimensional silhouette at low detail");
                        samples++;
                    }
                }
            }
        }
        require(cannonPeak[0] <= 350 && cannonPeak[1] <= 220 && cannonPeak[2] <= 130,
                "Cannon meshes must stay inside their detail budgets");
        require(chargePeak[0] <= 500 && chargePeak[1] <= 320 && chargePeak[2] <= 190,
                "Charge effects must stay inside their detail budgets");
        for (boolean enhanced : new boolean[]{false, true}) {
            Probe empty = cannon(10, 0, 1, enhanced, false);
            Probe charged = cannon(10, 1, 1, enhanced, false);
            Probe firing = cannon(10, 1, 1, enhanced, true);
            Probe half = cannon(10, 1, .5F, enhanced, true);
            require(empty.fingerprint != charged.fingerprint, "Charging must visibly open and energize the cannon");
            require(charged.fingerprint != firing.fingerprint, "Firing must visibly animate the cannon");
            require(charged.spectral && firing.spectral, "Charged and firing cannons must retain their rainbow reactor");
            require(half.count == firing.count && Math.abs(half.alpha - firing.alpha * .5) < .0001,
                    "Beam tail opacity must fade the entire cannon without changing its geometry");
            require(cannon(10, 1, 0, enhanced, true).count == 0, "An invisible cannon must emit no geometry");
        }
        require(cannon(10, 1, 1, false, false).fingerprint != cannon(10, 1, 1, true, false).fingerprint,
                "Chaos Blaster must have a distinct cannon silhouette");
        for (float invalid : new float[]{-1, Float.NaN, Float.POSITIVE_INFINITY}) {
            require(cannon(invalid, 1, 1, false, false).count == 0, "Invalid ages cannot emit a cannon");
            Probe charge = new Probe();
            AsrielVisualGeometry.busterCharge(charge, invalid, 1, 3.2F, false, 0);
            require(charge.count == 0, "Invalid ages cannot emit charge effects");
        }
        require(cannon(2, Float.NaN, 1, false, false).count == 0, "Invalid fill cannot emit a cannon");
        require(cannon(2, 1, Float.NaN, false, false).count == 0, "Invalid opacity cannot emit a cannon");
        System.out.println("Chaos cannon: " + samples + " charge/variant/LOD samples, " + vertices
                + " bounded vertices; cannon peaks " + cannonPeak[0] + "/" + cannonPeak[1] + "/" + cannonPeak[2]
                + ", charge peaks " + chargePeak[0] + "/" + chargePeak[1] + "/" + chargePeak[2] + " quads.");
    }

    private static Probe cannon(float age, float fill, float opacity, boolean enhanced, boolean firing) {
        Probe probe = new Probe();
        ChaosCannonVisuals.cannon(probe, age, fill, opacity, enhanced, 0, firing);
        return probe;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Probe implements AsrielVisualGeometry.Sink {
        int count;
        long fingerprint = 17;
        double minX, maxX, minY, maxY, minZ, maxZ, alpha;
        boolean spectral;
        public void vertex(double x, double y, double z, float r, float g, float b, float a) {
            require(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                    && Float.isFinite(r) && Float.isFinite(g) && Float.isFinite(b) && Float.isFinite(a),
                    "All cannon coordinates and colors must be finite");
            require(r >= 0 && r <= 1 && g >= 0 && g <= 1 && b >= 0 && b <= 1 && a >= 0 && a <= 1,
                    "Colors and opacity must stay bounded");
            require(x*x + y*y + z*z <= Math.pow(ChaosCannonVisuals.cullingRadius(), 2) + .000001,
                    "Culling must enclose the complete cannon and charge at every orientation");
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
            fingerprint = fingerprint * 31 + Double.doubleToLongBits(x);
            fingerprint = fingerprint * 31 + Double.doubleToLongBits(y);
            fingerprint = fingerprint * 31 + Double.doubleToLongBits(z);
            if (a > .03F && Math.max(r, Math.max(g,b)) - Math.min(r, Math.min(g,b)) > .4F) spectral = true;
            count++; vertices++; alpha += a;
        }
    }
}
