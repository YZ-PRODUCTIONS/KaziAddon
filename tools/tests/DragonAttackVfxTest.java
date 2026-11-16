import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.abilities.NagiAwakening.DragonVfxPhase;
import net.kazi.kazimod.models.abilities.DragonAttackMesh;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;

/** Exercises actual procedural geometry without Minecraft. The preview approximates game blending. */
public final class DragonAttackVfxTest {
    private static int checks;
    private static final int PANEL_W = 460, PANEL_H = 290, HEADER_H = 84;

    public static void main(String[] args) throws Exception {
        int[] peak = new int[3];
        long[] total = new long[3];
        float[] lengths = {0, 3, 12, 32};
        float[] frames = {0, .08F, .25F, .5F, .8F, .97F};
        for (DragonKind kind : DragonKind.values()) {
            for (DragonVfxPhase phase : DragonVfxPhase.values()) {
                float life = lifetime(kind, phase);
                for (float length : lengths) {
                    float radius = DragonAttackMesh.radius(kind, phase, length);
                    check(Float.isFinite(radius) && radius > 0, "valid culling radius");
                    for (float frame : frames) {
                        int previous = Integer.MAX_VALUE;
                        for (int detail = 0; detail < 3; detail++) {
                            Capture surface = new Capture(radius, false), glow = new Capture(radius, false);
                            DragonAttackMesh.render(surface, glow, kind, phase, detail, frame * life, life, length, 167);
                            validate(surface); validate(glow);
                            int vertices = surface.count + glow.count;
                            check(vertices <= 12000, "bounded geometry: " + kind + "/" + phase + " produced " + vertices);
                            check(vertices <= previous, "detail reduction must not increase geometry: " + kind + "/" + phase);
                            previous = vertices;
                            peak[detail] = Math.max(peak[detail], vertices);
                            total[detail] += vertices;
                        }
                    }
                    for (float age : new float[]{-1, life, life + 1}) {
                        Capture c = new Capture(radius, false);
                        DragonAttackMesh.render(c, c, kind, phase, 0, age, life, length, 167);
                        check(c.count == 0, "expired or unstarted effects must emit no geometry");
                    }
                }
            }
        }
        check(total[0] > total[1] && total[1] > total[2], "lower detail materially reduces total work");
        for (DragonVfxPhase phase : new DragonVfxPhase[]{DragonVfxPhase.NORMAL, DragonVfxPhase.SPECIAL}) {
            Set<String> shapes = new HashSet<>();
            for (DragonKind kind : DragonKind.values()) {
                Capture c = new Capture(DragonAttackMesh.radius(kind, phase, 8), true);
                DragonAttackMesh.render(c, c, kind, phase, 0, lifetime(kind, phase) * .4F, lifetime(kind, phase), 8, 167);
                check(c.count > 0 && c.visible > 0, "normal and special effects must be visible for every dragon");
                check(shapes.add(c.fingerprint()), "each dragon needs distinct geometry, not only a palette swap");
                Capture repeat = new Capture(DragonAttackMesh.radius(kind, phase, 8), true);
                DragonAttackMesh.render(repeat, repeat, kind, phase, 0, lifetime(kind, phase) * .4F, lifetime(kind, phase), 8, 167);
                check(c.fingerprint().equals(repeat.fingerprint()), "same frame/seed must render deterministically");
            }
        }
        Capture invalid = new Capture(100, false);
        for (float bad : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            DragonAttackMesh.render(invalid, invalid, DragonKind.FIRST, DragonVfxPhase.NORMAL, 0, bad, 12, 3, 167);
            DragonAttackMesh.render(invalid, invalid, DragonKind.FIRST, DragonVfxPhase.NORMAL, 0, 3, bad, 3, 167);
            DragonAttackMesh.render(invalid, invalid, DragonKind.FIRST, DragonVfxPhase.NORMAL, 0, 3, 12, bad, 167);
        }
        check(invalid.count == 0, "nonfinite inputs must not reach the renderer");
        System.out.println("Dragon attack VFX: " + checks + " geometry checks passed; peak vertices high/medium/low "
                + Arrays.toString(peak) + ".");
        if (args.length > 0) preview(Paths.get(args[0]));
    }

    private static float lifetime(DragonKind kind, DragonVfxPhase phase) {
        switch (phase) {
            case NORMAL: return 12;
            case SPECIAL: return kind == DragonKind.ICE_CRYSTAL ? 60 : 22;
            case CHARGE: return kind.special.windupTicks;
            case BIND: return 54;
            case SLAM: return 20;
            default: throw new AssertionError(phase);
        }
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        checks++;
    }
    private static void validate(Capture c) {
        check(c.finite, "all emitted vertices must be finite");
        check(c.alpha, "all opacity values must lie in [0,1]");
        check(c.bounded, "renderer culling bounds must contain every vertex");
        check(c.count % 4 == 0, "render streams must contain complete quads");
    }
    private static final class Capture implements DragonAttackMesh.Sink {
        final double radiusSquared;
        final TreeSet<String> shape;
        int count, visible;
        boolean finite = true, alpha = true, bounded = true;
        Capture(float radius, boolean fingerprint) {
            radiusSquared = (radius + .05) * (radius + .05);
            shape = fingerprint ? new TreeSet<>() : null;
        }
        @Override public void vertex(float x, float y, float z, int color, float a) {
            count++;
            finite &= Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
            alpha &= Float.isFinite(a) && a >= 0 && a <= 1;
            bounded &= x * (double)x + y * (double)y + z * (double)z <= radiusSquared;
            if (a > .01F) {
                visible++;
                if (shape != null) shape.add(Math.round(x * 1000) + ":" + Math.round(y * 1000) + ":" + Math.round(z * 1000));
            }
        }
        String fingerprint() { return shape.toString(); }
    }

    private static final class Face {
        final float[] x = new float[4], y = new float[4];
        float depth, red, green, blue, alpha;
    }
    private static final class DrawStream implements DragonAttackMesh.Sink {
        final List<Face> faces = new ArrayList<>();
        private final boolean vertical;
        private Face face = new Face();
        private int index;
        DrawStream(boolean vertical) { this.vertical = vertical; }
        @Override public void vertex(float x, float y, float z, int color, float alpha) {
            if (vertical) { float oldY = y; y = z; z = -oldY; }
            double yaw = 1.05, pitch = .34;
            float depth = (float) (z * Math.cos(yaw) - x * Math.sin(yaw));
            face.x[index] = (float) (x * Math.cos(yaw) + z * Math.sin(yaw));
            face.y[index] = (float) (y * Math.cos(pitch) - depth * Math.sin(pitch));
            face.depth += depth * .25F;
            face.red += (color >> 16 & 255) * .25F;
            face.green += (color >> 8 & 255) * .25F;
            face.blue += (color & 255) * .25F;
            face.alpha += alpha * .25F;
            if (++index == 4) {
                if (face.alpha > .005F) faces.add(face);
                face = new Face(); index = 0;
            }
        }
    }
    private static void preview(Path path) throws Exception {
        BufferedImage sheet = new BufferedImage(PANEL_W * 4, HEADER_H + PANEL_H * 4 + 44, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(9, 13, 22)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(236, 242, 255)); g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("INNER DRAGONS / Attack visual study", 28, 36);
        g.setColor(new Color(145, 161, 185)); g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("Actual procedural geometry  |  normal and special mid-animation frames  |  each panel fitted independently", 29, 62);
        for (DragonKind kind : DragonKind.values()) {
            int column = kind.ordinal() % 4, group = kind.ordinal() / 4;
            panel(sheet, column * PANEL_W, HEADER_H + group * PANEL_H * 2, kind, DragonVfxPhase.NORMAL);
            panel(sheet, column * PANEL_W, HEADER_H + (group * 2 + 1) * PANEL_H, kind, DragonVfxPhase.SPECIAL);
        }
        g.setColor(new Color(125, 143, 168)); g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Software preview: shape, color, and timing study. Game depth, camera fade, additive blending, and target motion require in-game review.", 28, sheet.getHeight() - 16);
        g.dispose();
        Files.createDirectories(path.toAbsolutePath().getParent());
        ImageIO.write(sheet, "png", path.toFile());
        System.out.println("Preview: " + path.toAbsolutePath());
    }
    private static void panel(BufferedImage sheet, int left, int top, DragonKind kind, DragonVfxPhase phase) {
        BufferedImage panel = new BufferedImage(PANEL_W, PANEL_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = panel.createGraphics();
        g.setColor(new Color(16, 22, 34)); g.fillRect(5, 5, PANEL_W - 10, PANEL_H - 10);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean vertical = phase == DragonVfxPhase.SPECIAL && (kind == DragonKind.ICE_CRYSTAL || kind == DragonKind.ANGEL);
        DrawStream surface = new DrawStream(vertical), glow = new DrawStream(vertical);
        float length = (float) (phase == DragonVfxPhase.NORMAL ? kind.reach : kind.special.range);
        if (phase == DragonVfxPhase.SPECIAL && (kind == DragonKind.FIRST || vertical)) length = 0;
        DragonVfxPhase drawPhase = kind == DragonKind.ANGEL && phase == DragonVfxPhase.SPECIAL ? DragonVfxPhase.BIND : phase;
        DragonAttackMesh.render(surface, glow, kind, drawPhase, 0, lifetime(kind, drawPhase) * .4F, lifetime(kind, drawPhase), length, 167);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        List<Face> all = new ArrayList<>(surface.faces); all.addAll(glow.faces);
        for (Face f : all) for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, f.x[i]); maxX = Math.max(maxX, f.x[i]);
            minY = Math.min(minY, f.y[i]); maxY = Math.max(maxY, f.y[i]);
        }
        float scale = Math.min((PANEL_W - 54) / Math.max(1, maxX - minX), (PANEL_H - 112) / Math.max(1, maxY - minY));
        float cx = (minX + maxX) * .5F, cy = (minY + maxY) * .5F;
        drawFaces(g, surface.faces, scale, cx, cy);
        BufferedImage glowImage = new BufferedImage(PANEL_W, PANEL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = glowImage.createGraphics();
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawFaces(gg, glow.faces, scale, cx, cy); gg.dispose();
        for (int y = 0; y < PANEL_H; y++) for (int x = 0; x < PANEL_W; x++) {
            int add = glowImage.getRGB(x, y), base = panel.getRGB(x, y);
            float a = (add >>> 24) / 255F;
            int r = Math.min(255, (base >> 16 & 255) + Math.round((add >> 16 & 255) * a));
            int green = Math.min(255, (base >> 8 & 255) + Math.round((add >> 8 & 255) * a));
            int b = Math.min(255, (base & 255) + Math.round((add & 255) * a));
            panel.setRGB(x, y, (r << 16) | (green << 8) | b);
        }
        g.setColor(new Color(kind.color)); g.fillRect(19, 18, 4, 36);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(232, 239, 251)); g.drawString(kind.id.replace('_', ' ').toUpperCase(), 33, 34);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14)); g.setColor(new Color(149, 167, 192));
        g.drawString(phase == DragonVfxPhase.NORMAL ? "NORMAL ATTACK" : kind.special.displayName, 33, 55);
        if (kind == DragonKind.ANGEL && phase == DragonVfxPhase.SPECIAL) slamInset(g);
        g.dispose();
        Graphics2D out = sheet.createGraphics(); out.drawImage(panel, left, top, null); out.dispose();
    }
    private static void slamInset(Graphics2D panel) {
        int left = PANEL_W - 155, top = PANEL_H - 108;
        panel.setColor(new Color(8, 12, 22, 235)); panel.fillRoundRect(left, top, 140, 94, 8, 8);
        panel.setColor(new Color(192, 172, 201)); panel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        panel.drawString("GROUND IMPACT", left + 9, top + 15);
        DrawStream surface = new DrawStream(true), glow = new DrawStream(true);
        DragonAttackMesh.render(surface, glow, DragonKind.ANGEL, DragonVfxPhase.SLAM, 0, 8, 20, 0, 167);
        List<Face> faces = new ArrayList<>(surface.faces); faces.addAll(glow.faces);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Face f : faces) for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, f.x[i]); maxX = Math.max(maxX, f.x[i]);
            minY = Math.min(minY, f.y[i]); maxY = Math.max(maxY, f.y[i]);
        }
        float scale = Math.min((PANEL_W - 40) / Math.max(1, maxX - minX), (PANEL_H - 85) / Math.max(1, maxY - minY));
        Graphics2D inset = (Graphics2D) panel.create();
        inset.clipRect(left, top + 15, 140, 77); inset.translate(left, top + 12); inset.scale(140.0 / PANEL_W, 77.0 / PANEL_H);
        drawFaces(inset, faces, scale, (minX + maxX) * .5F, (minY + maxY) * .5F); inset.dispose();
    }
    private static void drawFaces(Graphics2D g, List<Face> faces, float scale, float cx, float cy) {
        faces.sort(Comparator.comparingDouble(f -> f.depth));
        int[] x = new int[4], y = new int[4];
        for (Face face : faces) {
            for (int i = 0; i < 4; i++) {
                x[i] = Math.round(PANEL_W * .5F + (face.x[i] - cx) * scale);
                y[i] = Math.round(PANEL_H * .57F - (face.y[i] - cy) * scale);
            }
            g.setColor(new Color(clamp(face.red), clamp(face.green), clamp(face.blue), clamp(face.alpha * 255)));
            g.fillPolygon(x, y, 4);
        }
    }
    private static int clamp(float v) { return Math.max(0, Math.min(255, Math.round(v))); }
}
