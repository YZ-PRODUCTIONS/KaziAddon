package net.kazi.kazimod.client.zoan;

import java.util.Locale;

/** Curves are shared by the body, joints and the surrounding transformation mesh. */
public final class ZoanTransitionMotion {
    public enum Shape { HYBRID, BEAST, WINGED, SERPENT, SHELL, GIANT }
    public final Shape shape;
    public final int duration;
    public final int color;

    private ZoanTransitionMotion(Shape shape, int duration, int color) {
        this.shape = shape;
        this.duration = duration;
        this.color = color;
    }

    public static ZoanTransitionMotion forForm(String name, boolean partial) {
        String id = name.toLowerCase(Locale.ROOT);
        boolean hybrid = id.contains("heavy") || id.contains("hybrid") || id.contains("horn_point") || id.contains("stinger_point");
        Shape shape;
        if (id.contains("world_turtle") || id.contains("kame_guard")) shape = Shape.SHELL;
        else if (id.contains("daibutsu")) shape = Shape.GIANT;
        else if (hybrid) shape = Shape.HYBRID;
        else if (id.contains("slither") || id.contains("seiryu") || id.contains("orochi")) shape = Shape.SERPENT;
        else if (id.contains("fly") || id.contains("phoenix") || id.contains("falcon") || id.contains("ptera") || id.contains("nue") || id.contains("god")) shape = Shape.WINGED;
        else if (!partial && (id.contains("walk") || id.contains("guard") || id.contains("full") || id.contains("headless"))) shape = Shape.BEAST;
        else shape = Shape.HYBRID;
        int duration = shape == Shape.GIANT || shape == Shape.SERPENT || id.contains("world_turtle") ? 40
                : id.contains("mammoth") || id.contains("brachio") || id.contains("zou_guard") ? 36 : 30;
        int color = id.contains("phoenix") ? 0x9eeef6 : id.contains("cerberus") ? 0xbcb2e8
                : id.contains("god") || id.contains("daibutsu") || id.contains("world_turtle") ? 0xffe8a1
                : id.contains("seiryu") ? 0xb0dfff : 0xdce8e8;
        return new ZoanTransitionMotion(shape, duration, color);
    }

    public static float clamp(float x) { return Math.max(0, Math.min(1, x)); }
    public static float smooth(float x) { x = clamp(x); return x * x * (3 - 2 * x); }
    public static float reveal(float time) { return smooth((time - .30F) / .48F); }
    public static float sourceOpacity(float time) { return 1 - smooth((time - .22F) / .26F); }
    public static float targetOpacity(float time) { return smooth((time - .28F) / .25F); }
    public static float release(float time) { return (float)Math.sin(Math.PI * smooth((time - .30F) / .70F)); }
    public static float windup(float time) { return smooth(time / .30F); }
    public static float settle(float time) { return (float)Math.sin(Math.PI * clamp((time - .72F) / .28F)); }
    public static float scale(float source, float target, float time) {
        float ratio = Math.max(.025F, Math.min(6, source / Math.max(.1F, target)));
        float growth = reveal(time);
        return ratio + (1 - ratio) * growth + .025F * settle(time);
    }
}
