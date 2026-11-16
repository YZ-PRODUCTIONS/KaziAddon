import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerDimensions;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ShockerBreakerVisuals;

/** Additive software preview of the production mesh; never edits ability icons or game resources. */
public final class ShockerBreakerPreview {
    private static final int BACKGROUND = 0xFF0C101C;
    private static final double[] FORWARD = unit(.65, .17, 1);
    private static final double[] RIGHT = unit(FORWARD[2], 0, -FORWARD[0]);
    private static final double[] UP = {
            FORWARD[1] * RIGHT[2] - FORWARD[2] * RIGHT[1],
            FORWARD[2] * RIGHT[0] - FORWARD[0] * RIGHT[2],
            FORWARD[0] * RIGHT[1] - FORWARD[1] * RIGHT[0]};
    private ShockerBreakerPreview() { }

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "." : args[0], "deliverables/asriel_vfx");
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create preview directory");
        BufferedImage sheet = blank(1680, 1000);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(230, 235, 248)); g.setFont(new Font("SansSerif", Font.BOLD, 23));
        int charge = ShockerBreakerVisualTiming.CHARGE_TICKS;
        int endOfSustain = charge + ShockerBreakerVisualTiming.STRIKE_TICKS;
        int lifetime = ShockerBreakerVisualTiming.LIFETIME;
        g.drawString(String.format("Shocker Breaker / EX — violent rainbow discharge; %.1f-second impact; unchanged hitboxes",
                (lifetime - charge) / 20.0), 22, 32);
        float[] ages = {charge-16, charge, charge+2, charge+14, lifetime-20, endOfSustain+1, lifetime-1, charge};
        String[] titles = {"Charge: rainbow arcs at caster", "Impact: white core / rainbow fringe", "One expanding blast", "Sustained jagged branches",
                "Late discharge / radial streaks", "Full pillar begins fading", "Final fade", "EX: stronger discharge"};
        for (int i = 0; i < ages.length; i++) {
            int x = i % 4 * 420, y = 50 + i / 4 * 475;
            g.drawImage(render(ages[i], i == 7 ? 4 : 3, i == 7, i == 0, 420, 425), x, y + 28, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 16)); g.setColor(new Color(230, 235, 248));
            g.drawString(titles[i], x + 14, y + 18);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(164, 176, 200));
            g.drawString(String.format("%.2f s | damage radius: %.0f blocks", ages[i] / 20.0,
                    ShockerBreakerDimensions.damageRadius(i == 7)), x + 14, y + 451);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(output, "shocker_breaker_rework.png"));
        impactDetail(output);
        animation(new File(output, "shocker_breaker_rework.gif"));
        System.out.println("Shocker Breaker production-mesh PNG and animated preview written to " + output.getAbsolutePath());
    }

    private static BufferedImage render(float age, float radius, boolean enhanced, boolean aura, int width, int height) {
        return render(age, radius, enhanced, aura, width, height, false);
    }

    private static BufferedImage render(float age, float radius, boolean enhanced, boolean aura, int width, int height,
                                        boolean close) {
        Mesh mesh = new Mesh();
        double worldScale = ShockerBreakerDimensions.VISUAL_SCALE;
        AsrielVisualGeometry.Sink scaled = (x, y, z, r, g, b, a) ->
                mesh.vertex(x * worldScale, y * worldScale, z * worldScale, r, g, b, a);
        if (aura) ShockerBreakerVisuals.chargeAura(scaled, age, 0);
        else ShockerBreakerVisuals.render(scaled, age, radius, enhanced, 0);
        if (mesh.index != 0) throw new AssertionError("Unfinished visual quad");
        BufferedImage image = blank(width, height);
        // Fixed extents across phases: the fade and pulsing must not be auto-rescaled in a preview.
        double minX = aura ? -2.8 : close ? -6 : -8, maxX = aura ? 2.8 : close ? 6 : 8;
        double minY = aura ? -.8 : close ? -.7 : -1.3, maxY = aura ? 3.8 : close ? 6.3 : 24.8;
        minX *= worldScale; maxX *= worldScale; minY *= worldScale; maxY *= worldScale;
        double scale = Math.min((width - 24) / (maxX - minX), (height - 16) / (maxY - minY));
        double centerX = (minX + maxX) / 2, centerY = (minY + maxY) / 2;
        for (Vertex[] face : mesh.faces) {
            double[][] points = new double[4][2];
            for (int i = 0; i < 4; i++) {
                Vertex v = face[i];
                points[i][0] = (dot(v, RIGHT) - centerX) * scale + width * .5;
                points[i][1] = height * .5 - (dot(v, UP) - centerY) * scale;
            }
            triangle(image, points[0], points[1], points[2], face[0], face[1], face[2]);
            triangle(image, points[0], points[2], points[3], face[0], face[2], face[3]);
        }
        return image;
    }

    private static void impactDetail(File output) throws Exception {
        BufferedImage sheet = blank(1680, 1000);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(230, 235, 248)); g.setFont(new Font("SansSerif", Font.BOLD, 23));
        g.drawString("Shocker Breaker — one detonation, sustained electrical spray, then a full-column fade", 22, 32);
        int charge = ShockerBreakerVisualTiming.CHARGE_TICKS;
        int lifetime = ShockerBreakerVisualTiming.LIFETIME;
        float[] ages = {charge, charge+3, charge+18, lifetime-20, lifetime-3, charge};
        String[] titles = {"Initial rainbow impact", "Expanding pressure front", "Sustained ground discharges",
                "Late electrical spray", "Column and sparks fade together", "EX impact"};
        for (int i = 0; i < ages.length; i++) {
            int x = i % 3 * 560, y = 50 + i / 3 * 475;
            g.drawImage(render(ages[i], i == 5 ? 4 : 3, i == 5, false, 560, 425, true), x, y + 28, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 17)); g.setColor(new Color(230, 235, 248));
            g.drawString(titles[i], x + 14, y + 18);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(164, 176, 200));
            g.drawString(String.format("%.2f seconds from activation", ages[i] / 20), x + 14, y + 451);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(output, "shocker_breaker_impact_detail.png"));
    }

    private static BufferedImage blank(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setColor(new Color(BACKGROUND)); g.fillRect(0, 0, width, height); g.dispose();
        return image;
    }

    private static void triangle(BufferedImage image, double[] a, double[] b, double[] c, Vertex va, Vertex vb, Vertex vc) {
        double denominator = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]);
        if (Math.abs(denominator) < 1e-10) return;
        int x0 = Math.max(0, (int)Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int x1 = Math.min(image.getWidth() - 1, (int)Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int y0 = Math.max(0, (int)Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int y1 = Math.min(image.getHeight() - 1, (int)Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            double u = ((b[1] - c[1]) * (x + .5 - c[0]) + (c[0] - b[0]) * (y + .5 - c[1])) / denominator;
            double v = ((c[1] - a[1]) * (x + .5 - c[0]) + (a[0] - c[0]) * (y + .5 - c[1])) / denominator;
            if (u < 0 || v < 0 || u + v > 1) continue;
            double w = 1 - u - v, alpha = va.a * u + vb.a * v + vc.a * w;
            int prior = image.getRGB(x, y);
            int r = Math.min(255, (prior >> 16 & 255) + (int)((va.r * u + vb.r * v + vc.r * w) * alpha * 255));
            int g = Math.min(255, (prior >> 8 & 255) + (int)((va.g * u + vb.g * v + vc.g * w) * alpha * 255));
            int b0 = Math.min(255, (prior & 255) + (int)((va.b * u + vb.b * v + vc.b * w) * alpha * 255));
            image.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | b0);
        }
    }

    private static void animation(File output) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        try (FileImageOutputStream stream = new FileImageOutputStream(output)) {
            writer.setOutput(stream); writer.prepareWriteSequence(null);
            ImageWriteParam params = writer.getDefaultWriteParam();
            int frames = (int) Math.ceil((ShockerBreakerVisualTiming.LIFETIME + 4) * 1.5);
            for (int frame = 0; frame < frames; frame++) {
                float age = frame * 2F / 3;
                boolean charging = age < ShockerBreakerVisualTiming.CHARGE_TICKS;
                BufferedImage image = render(age, 3, false, charging, 420, 520);
                Graphics2D g = image.createGraphics(); g.setColor(new Color(230, 235, 248));
                g.setFont(new Font("SansSerif", Font.BOLD, 19));
                g.drawString(charging ? "Charge: arcs around caster" : "Shocker Breaker: violent discharge", 14, 25);
                g.setFont(new Font("SansSerif", Font.PLAIN, 15));
                g.drawString(String.format("%.2f s | 216 blocks tall | 9-block hitbox", age / 20), 14, 48); g.dispose();
                IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), params);
                String format = metadata.getNativeMetadataFormatName();
                IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(format);
                IIOMetadataNode control = (IIOMetadataNode) tree.getElementsByTagName("GraphicControlExtension").item(0);
                if (control == null) { control = new IIOMetadataNode("GraphicControlExtension"); tree.appendChild(control); }
                control.setAttribute("disposalMethod", "none"); control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE"); control.setAttribute("delayTime", frame % 3 == 2 ? "4" : "3");
                control.setAttribute("transparentColorIndex", "0");
                if (frame == 0) {
                    IIOMetadataNode extensions = new IIOMetadataNode("ApplicationExtensions"), loop = new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID", "NETSCAPE"); loop.setAttribute("authenticationCode", "2.0");
                    loop.setUserObject(new byte[]{1, 0, 0}); extensions.appendChild(loop); tree.appendChild(extensions);
                }
                metadata.setFromTree(format, tree); writer.writeToSequence(new IIOImage(image, null, metadata), params);
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
    }

    private static double[] unit(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z); return new double[]{x / length, y / length, z / length};
    }
    private static double dot(Vertex v, double[] axis) { return v.x * axis[0] + v.y * axis[1] + v.z * axis[2]; }
    private static final class Vertex {
        final double x, y, z; final float r, g, b, a;
        Vertex(double x, double y, double z, float r, float g, float b, float a) {
            this.x = x; this.y = y; this.z = z; this.r = r; this.g = g; this.b = b; this.a = a;
        }
    }
    private static final class Mesh implements AsrielVisualGeometry.Sink {
        final List<Vertex[]> faces = new ArrayList<>();
        final Vertex[] pending = new Vertex[4]; int index;
        public void vertex(double x, double y, double z, float r, float g, float b, float a) {
            pending[index++] = new Vertex(x, y, z, r, g, b, a);
            if (index == 4) { faces.add(pending.clone()); index = 0; }
        }
    }
}
