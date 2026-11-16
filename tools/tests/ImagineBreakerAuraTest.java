import net.kazi.kazimod.client.nagi.ImagineBreakerAuraMesh;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Standalone geometry checks and optional software preview of the actual rendered mesh. */
public final class ImagineBreakerAuraTest {
    private static int checks;
    private static final int SIZE = 420;
    private static BufferedImage image;
    private static double[] depth;
    private static double yaw;
    private static boolean glow;

    public static void main(String[] args) throws Exception {
        int[] maximum = new int[3];
        double[] signature = new double[2];
        for (int detail = 0; detail < 3; detail++) {
            for (int frame = 0; frame <= 320; frame++) {
                int[] count = {0};
                ImagineBreakerAuraMesh.render((x, y, z, color, alpha) -> {
                    check(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "finite vertices");
                    check(Float.isFinite(alpha) && alpha >= 0 && alpha <= 1, "finite bounded opacity");
                    check(Math.abs(x) < .6 && Math.abs(z) < .6 && y > -.1 && y < 1, "arm-local bounds");
                    count[0]++;
                }, frame * .5F, detail, 1);
                check(count[0] > 0 && count[0] % 4 == 0 && count[0] <= 1400, "bounded complete quads");
                maximum[detail] = Math.max(maximum[detail], count[0]);
            }
        }
        check(maximum[0] > maximum[1] && maximum[1] > maximum[2], "lower settings reduce geometry");
        for (int frame = 0; frame < 2; frame++) {
            final int index = frame;
            ImagineBreakerAuraMesh.render((x, y, z, color, alpha) ->
                    signature[index] += x * y + z * alpha + alpha, frame * 6, 0, 1);
        }
        check(Math.abs(signature[0] - signature[1]) > .01, "aura actually animates");
        int[] empty = {0};
        ImagineBreakerAuraMesh.render((x, y, z, color, alpha) -> empty[0]++, 0, 0, 0);
        ImagineBreakerAuraMesh.render((x, y, z, color, alpha) -> empty[0]++, Float.NaN, 0, 1);
        check(empty[0] == 0, "invisible or invalid frames emit nothing");
        System.out.println("Imagine Breaker aura: " + checks + " checks passed; vertex budgets "
                + Arrays.toString(maximum));
        if (args.length > 0) preview(Paths.get(args[0]));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static double[] project(float x, float y, float z, int color, float alpha) {
        double px = x * Math.cos(yaw) - z * Math.sin(yaw);
        double pz = x * Math.sin(yaw) + z * Math.cos(yaw);
        double py = y - .36;
        return new double[]{SIZE * .5 + px * 330, SIZE * .5 + (py * .94 - pz * .342) * 330,
                py * .342 + pz * .94, color >> 16 & 255, color >> 8 & 255, color & 255, alpha};
    }

    private static ImagineBreakerAuraMesh.Sink sink() {
        double[][] quad = new double[4][];
        int[] count = {0};
        return (x, y, z, color, alpha) -> {
            quad[count[0]++] = project(x, y, z, color, alpha);
            if (count[0] == 4) {
                triangle(quad[0], quad[1], quad[2]);
                triangle(quad[0], quad[2], quad[3]);
                count[0] = 0;
            }
        };
    }

    private static double edge(double[] a, double[] b, double x, double y) {
        return (x - a[0]) * (b[1] - a[1]) - (y - a[1]) * (b[0] - a[0]);
    }

    private static void triangle(double[] a, double[] b, double[] c) {
        double area = edge(a, b, c[0], c[1]);
        if (Math.abs(area) < .000001) return;
        int x0 = Math.max(0, (int) Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int x1 = Math.min(SIZE - 1, (int) Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int y0 = Math.max(0, (int) Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int y1 = Math.min(SIZE - 1, (int) Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            double u = edge(b, c, x + .5, y + .5) / area;
            double v = edge(c, a, x + .5, y + .5) / area, w = 1 - u - v;
            if (u < 0 || v < 0 || w < 0) continue;
            double z = u * a[2] + v * b[2] + w * c[2];
            if (z < depth[y * SIZE + x] - .0001) continue;
            double alpha = glow ? u * a[6] + v * b[6] + w * c[6] : 1;
            int old = glow ? image.getRGB(x, y) : 0, color = 0xFF000000;
            for (int channel = 0; channel < 3; channel++) {
                int shift = (2 - channel) * 8;
                int rgb = Math.min(255, (old >> shift & 255)
                        + (int) ((u * a[3 + channel] + v * b[3 + channel] + w * c[3 + channel]) * alpha));
                color |= rgb << shift;
            }
            image.setRGB(x, y, color);
            if (!glow) depth[y * SIZE + x] = z;
        }
    }

    private static void box(ImagineBreakerAuraMesh.Sink out, float y0, float y1, int color) {
        float[][] vertices = {{-.125F,y0,-.125F},{.125F,y0,-.125F},{.125F,y1,-.125F},{-.125F,y1,-.125F},
                {-.125F,y0,.125F},{.125F,y0,.125F},{.125F,y1,.125F},{-.125F,y1,.125F}};
        int[][] faces = {{0,1,2,3},{4,5,6,7},{0,4,7,3},{1,5,6,2},{0,1,5,4},{3,2,6,7}};
        for (int[] face : faces) for (int index : face) {
            float[] p = vertices[index];
            out.vertex(p[0], p[1], p[2], color, 1);
        }
    }

    private static void preview(Path path) throws Exception {
        BufferedImage sheet = new BufferedImage(SIZE * 3, SIZE + 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(19, 24, 32));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int panel = 0; panel < 3; panel++) {
            image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = image.createGraphics();
            bg.setColor(new Color(19, 24, 32)); bg.fillRect(0, 0, SIZE, SIZE); bg.dispose();
            depth = new double[SIZE * SIZE]; Arrays.fill(depth, -Double.MAX_VALUE);
            yaw = panel * .8 + .3;
            glow = false;
            box(sink(), -.125F, .38F, 0x395D70);
            box(sink(), .38F, .625F, 0x987253);
            glow = true;
            ImagineBreakerAuraMesh.render(sink(), panel * 7, panel == 2 ? 2 : 0, 1);
            g.drawImage(image, panel * SIZE, 0, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16)); g.setColor(new Color(235, 218, 176));
            g.drawString(panel == 2 ? "Low-detail / side view" : "Full-detail / frame " + panel * 7,
                    panel * SIZE + 20, SIZE + 24);
        }
        g.dispose();
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        ImageIO.write(sheet, "png", path.toFile());
        System.out.println("Software mesh preview: " + path.toAbsolutePath());
    }
}
