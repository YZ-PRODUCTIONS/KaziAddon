import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.client.nagi.DragonStrikeMesh;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;

/** Runs the actual summon profiles and meshes without Minecraft; not a live AI integration test. */
public final class DragonStrikeTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        check(DragonKind.values().length == 8, "eight independent kinds");
        HashSet<String> names = new HashSet<>();
        HashSet<Integer> colors = new HashSet<>();
        for (DragonKind kind : DragonKind.values()) {
            check(names.add(kind.id) && colors.add(kind.color), "unique identifiers and palettes");
            check(kind.damage > 0 && kind.damage <= 9, "per-dragon damage budget");
            check(kind.attackTicks >= 40 && kind.attackTicks <= 80, "no per-tick damage spam");
            check(kind.health > 0 && kind.armor >= 0 && kind.speed > 0, "valid mob attributes");
            check(DragonKind.byId(kind.ordinal()) == kind, "network kind round trip");
        }
        check(DragonKind.byId(-1) == DragonKind.ANGEL && DragonKind.byId(8) == DragonKind.FIRST, "invalid synced IDs safe");
        check(DragonKind.ICE_CRYSTAL.health > DragonKind.FIRST.health
                && DragonKind.ICE_CRYSTAL.armor > DragonKind.FIRST.armor, "ice is tougher");
        check(DragonKind.SPEARHEAD.speed > DragonKind.FIRST.speed
                && DragonKind.SPEARHEAD.reach > DragonKind.FIRST.reach, "lightning is faster and ranged");
        int[] budgets = new int[3];
        for (DragonKind kind : DragonKind.values()) for (int detail = 0; detail < 3; detail++) {
            for (int frame = 0; frame <= 40; frame++) {
                int[] count = {0};
                double age = frame * 23.5;
                DragonStrikeMesh.render((x,y,z,c,a) -> {
                    check(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z), "finite vertex");
                    check(a >= 0 && a <= 1 && Float.isFinite(a), "valid opacity");
                    check(Math.abs(x) <= 3 && y > -.8 && y < 2 && z >= -3.5 && z <= 2, "bounded entity visual");
                    count[0]++;
                }, kind, age, (frame % 12) / 12F, detail);
                check(count[0] % 4 == 0 && count[0] <= 1100, "bounded complete quads per dragon");
                if (age >= 8) check(count[0] > 0, "continuous summon never expires visually");
                budgets[detail] = Math.max(budgets[detail], count[0]);
            }
        }
        check(budgets[0] > budgets[1] && budgets[1] > budgets[2], "lower detail reduces geometry");
        int[] hidden = {0};
        DragonStrikeMesh.render((x,y,z,c,a) -> hidden[0]++, DragonKind.FIRST, Double.NaN, 0, 0);
        DragonStrikeMesh.render((x,y,z,c,a) -> hidden[0]++, null, 20, 0, 0);
        check(hidden[0] == 0, "invalid visuals absent");
        System.out.println("Dragon summons: " + checks + " checks; per-dragon vertex budgets "
                + java.util.Arrays.toString(budgets));
        if (args.length > 0) preview(Paths.get(args[0]));
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); checks++; }

    private static final class Face {
        final int[] x = new int[4], y = new int[4];
        double depth; int color; float alpha;
    }
    private static void panel(Graphics2D g, int left, int top, DragonKind kind) {
        List<Face> faces = new ArrayList<>();
        Face[] face = {new Face()}; int[] corner = {0};
        double yaw = 1.05, scale = 69;
        DragonStrikeMesh.render((x,y,z,c,a) -> {
            z += .8;
            double horizontal = x * Math.cos(yaw) + z * Math.sin(yaw);
            double depth = z * Math.cos(yaw) - x * Math.sin(yaw);
            Face f = face[0]; int i = corner[0]++;
            f.x[i] = (int)(left + 200 + horizontal * scale);
            f.y[i] = (int)(top + 195 - (y * .94 - depth * .342) * scale);
            f.depth += depth; f.color = c; f.alpha = a;
            if (corner[0] == 4) { faces.add(f); face[0] = new Face(); corner[0] = 0; }
        }, kind, 30, .4F, 0);
        faces.sort(Comparator.comparingDouble(f -> f.depth));
        Shape clip = g.getClip(); g.setClip(left, top, 400, 300);
        for (Face f : faces) {
            g.setColor(new Color(f.color >> 16 & 255, f.color >> 8 & 255, f.color & 255, (int)(f.alpha * 255)));
            g.fillPolygon(f.x, f.y, 4);
        }
        g.setColor(new Color(230,230,240));
        g.drawString(kind.id.replace('_', ' '), left + 20, top + 30);
        g.setClip(clip);
    }
    private static void preview(Path path) throws Exception {
        BufferedImage image = new BufferedImage(1600, 650, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(20,25,34)); g.fillRect(0,0,1600,650);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        for (DragonKind kind : DragonKind.values())
            panel(g, kind.ordinal() % 4 * 400, 50 + kind.ordinal() / 4 * 300, kind);
        g.setColor(new Color(230,230,240));
        g.drawString("Dragon Strike | eight independent summons (software mesh preview)", 25, 30);
        g.dispose();
        Files.createDirectories(path.toAbsolutePath().getParent());
        ImageIO.write(image, "png", path.toFile());
    }
}
