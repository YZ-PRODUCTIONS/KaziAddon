import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ShockerBreakerWarningVisuals;

/** Exercises the actual warning geometry and exports its front-facing icon without changing game assets. */
public final class ShockerBreakerWarningTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        for (float age = 0; age < 40; age += .25F) {
            Probe icon = new Probe();
            ShockerBreakerWarningVisuals.icon(icon, age);
            check(icon.count == 24, "The red box and punctuation must stay visible throughout the charge");
            check(icon.valid && icon.maxX <= 1.001 && icon.minY >= 0 && icon.maxY <= 2.001,
                    "Warning icon must stay within its declared size and use legal red vertices");
            check(icon.minAlpha >= .39, "Warning must remain readable even in the dim flash phase");
            for (float radius : new float[]{3, 4, 9, 12}) {
                Probe floor = new Probe();
                ShockerBreakerWarningVisuals.ground(floor, age, radius);
                check(floor.count == 20 && floor.valid, "Ground box must remain visible and finite");
                check(floor.maxX == radius && floor.maxZ == radius, "Warning must precisely bound the intended strike radius");
                check(floor.minY > 0 && floor.maxY < .1, "Ground warning must sit just above the floor");
            }
        }
        exactSprite();
        for (float age : new float[]{-1, 40, 41, 106, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            Probe icon = new Probe(), floor = new Probe();
            ShockerBreakerWarningVisuals.icon(icon, age);
            ShockerBreakerWarningVisuals.ground(floor, age, 9);
            check(icon.count == 0 && floor.count == 0, "Warnings must disappear at the strike and reject invalid ages");
        }
        for (float radius : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            Probe floor = new Probe(); ShockerBreakerWarningVisuals.ground(floor, 10, radius);
            check(floor.count == 0, "Invalid radii must not draw a misleading target");
        }
        if (args.length > 0) preview(new File(args[0]));
        System.out.println("Shocker Breaker warning: " + checks + " phase, visibility, bounds and color checks passed.");
    }

    private static void exactSprite() {
        for (float age : new float[]{0, 2.65F, 2.67F, 5.32F, 5.34F, 39.99F}) {
            final boolean[][] mask = new boolean[20][20];
            final float[] tint = {-1};
            ShockerBreakerWarningVisuals.icon(new AsrielVisualGeometry.Sink() {
                final double[][] quad = new double[4][2]; int vertex;
                public void vertex(double x, double y, double z, float r, float g, float b, float a) {
                    tint[0] = g; quad[vertex][0] = x; quad[vertex++][1] = y;
                    if (vertex == 4) {
                        for (int py = 0; py < 20; py++) for (int px = 0; px < 20; px++) {
                            double cx = (px + .5) * .1 - 1, cy = 2 - (py + .5) * .1;
                            if (cx > quad[0][0] && cx < quad[1][0] && cy > quad[0][1] && cy < quad[2][1]) mask[py][px] = true;
                        }
                        vertex = 0;
                    }
                }
            }, age);
            for (int y = 0; y < 20; y++) for (int x = 0; x < 20; x++) {
                boolean original = x == 0 || x == 19 || y == 0 || y == 19
                        || x >= 8 && x <= 10 && (y >= 4 && y <= 10 || y >= 13 && y <= 14);
                check(mask[y][x] == original, "Warning geometry must reproduce every pixel of the original 20x20 mask");
            }
            boolean bright = age >= 2.666667F && age < 5.333334F;
            if (age < 6) check((tint[0] > .2F) == bright, "Warning frame cadence must preserve Undertale's 30Hz quarter-frame animation");
        }
    }

    private static void preview(File file) throws Exception {
        BufferedImage image = new BufferedImage(360, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(8, 12, 29)); graphics.fillRect(0, 0, 360, 480);
        ShockerBreakerWarningVisuals.icon(new AsrielVisualGeometry.Sink() {
            final int[] xs = new int[4], ys = new int[4]; int index;
            public void vertex(double x, double y, double z, float r, float g, float b, float a) {
                xs[index] = (int)Math.round(180 + x * 150); ys[index++] = (int)Math.round(430 - y * 150);
                if (index == 4) {
                    graphics.setColor(new Color(r, g, b, a)); graphics.fillPolygon(xs, ys, 4); index = 0;
                }
            }
        }, 32);
        graphics.dispose();
        File parent = file.getParentFile(); if (parent != null) parent.mkdirs();
        ImageIO.write(image, "png", file);
    }

    private static final class Probe implements AsrielVisualGeometry.Sink {
        int count; boolean valid = true;
        double maxX, maxZ, minY = Double.POSITIVE_INFINITY, maxY;
        float minAlpha = 1;
        public void vertex(double x, double y, double z, float r, float g, float b, float a) {
            count++;
            valid &= Double.isFinite(x + y + z) && Float.isFinite(r + g + b + a)
                    && r == 1 && g >= 0 && g <= .3 && b >= 0 && b <= .3 && a > 0 && a <= 1;
            maxX = Math.max(maxX, Math.abs(x)); maxZ = Math.max(maxZ, Math.abs(z));
            minY = Math.min(minY, y); maxY = Math.max(maxY, y); minAlpha = Math.min(minAlpha, a);
        }
    }
}
