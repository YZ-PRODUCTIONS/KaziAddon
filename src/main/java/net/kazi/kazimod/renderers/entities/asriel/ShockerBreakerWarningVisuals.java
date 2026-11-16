package net.kazi.kazimod.renderers.entities.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry.Sink;

/** Sharp red pre-strike boxes and the pixel exclamation mark from the supplied Undertale reference. */
public final class ShockerBreakerWarningVisuals {
    public static final float ICON_WIDTH = 2F;
    public static final float ICON_HEIGHT = 2F;

    private ShockerBreakerWarningVisuals() { }

    /** A square bounds the strike's circular footprint; all strokes stay inside +/- radius. */
    public static void ground(Sink out, float age, float radius) {
        if (!active(age) || !Float.isFinite(radius) || radius <= 0) return;
        double edge = radius, stroke = Math.min(radius * .035, .18);
        float alpha = pulse(age);
        floor(out, -edge, -edge, edge, edge, .055, .018F + alpha * .014F);
        floor(out, -edge, -edge, edge, -edge + stroke, .065, alpha);
        floor(out, -edge, edge - stroke, edge, edge, .065, alpha);
        floor(out, -edge, -edge + stroke, -edge + stroke, edge - stroke, .065, alpha);
        floor(out, edge - stroke, -edge + stroke, edge, edge - stroke, .065, alpha);
    }

    /** Local XY plane, with its bottom at y=0. The renderer faces this little sign toward the camera. */
    public static void icon(Sink out, float age) {
        if (!active(age)) return;
        // Exact spr_rainbowtarget 20x20 mask. Original gold/red animation is kept in two red shades.
        boolean bright = ((int) Math.floor(age * .375F) & 1) != 0;
        pixels(out, 0, 0, 20, 1, bright);
        pixels(out, 0, 19, 20, 20, bright);
        pixels(out, 0, 1, 1, 19, bright);
        pixels(out, 19, 1, 20, 19, bright);
        pixels(out, 8, 4, 11, 11, bright);
        pixels(out, 8, 13, 11, 15, bright);
    }

    private static boolean active(float age) {
        return Float.isFinite(age) && age >= 0 && age < ShockerBreakerVisualTiming.CHARGE_TICKS;
    }

    private static float pulse(float age) {
        // Undertale advances sprite frames by .25 every 30Hz step: one frame per 2 2/3 Minecraft ticks.
        return ((int) Math.floor(age * .375F) & 1) == 0 ? .96F : .56F;
    }

    private static void floor(Sink out, double left, double back, double right, double front, double y, float alpha) {
        out.vertex(left, y, back, 1, .035F, .025F, alpha);
        out.vertex(right, y, back, 1, .035F, .025F, alpha);
        out.vertex(right, y, front, 1, .035F, .025F, alpha);
        out.vertex(left, y, front, 1, .035F, .025F, alpha);
    }

    private static void pixels(Sink out, int x0, int y0, int x1, int y1, boolean bright) {
        double left = x0 * .10 - 1, right = x1 * .10 - 1, bottom = 2 - y1 * .10, top = 2 - y0 * .10;
        float green = bright ? 64 / 255F : .035F, blue = bright ? 64 / 255F : .025F;
        out.vertex(left, bottom, 0, 1, green, blue, .96F);
        out.vertex(right, bottom, 0, 1, green, blue, .96F);
        out.vertex(right, top, 0, 1, green, blue, .96F);
        out.vertex(left, top, 0, 1, green, blue, .96F);
    }
}
