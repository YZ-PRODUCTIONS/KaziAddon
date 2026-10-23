package net.kazi.kazimod.client.zoan;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.kazi.kazimod.mixin.client.ZoanTransitionRenderAccessors;
import net.minecraft.client.renderer.model.ModelRenderer;

/** Restores cached models to their authored pose instead of accumulating additive animation. */
final class ZoanTransitionPose {
    private final List<Part> parts = new ArrayList<>();

    static ZoanTransitionPose capture(Object model) {
        ZoanTransitionPose pose = new ZoanTransitionPose();
        Map<ModelRenderer, Boolean> seen = new IdentityHashMap<>();
        for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Class<?> fieldType = field.getType();
                if (!ModelRenderer.class.isAssignableFrom(fieldType) && !Iterable.class.isAssignableFrom(fieldType)
                        && !ModelRenderer[].class.isAssignableFrom(fieldType)) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(model);
                    if (value instanceof ModelRenderer) pose.add((ModelRenderer)value, seen);
                    else if (value instanceof Iterable) {
                        for (Object part : (Iterable<?>)value)
                            if (part instanceof ModelRenderer) pose.add((ModelRenderer)part, seen);
                    } else if (value instanceof ModelRenderer[]) {
                        for (ModelRenderer part : (ModelRenderer[])value) pose.add(part, seen);
                    }
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Cannot snapshot zoan model " + model.getClass().getName(), ex);
                }
            }
        }
        return pose;
    }

    private void add(ModelRenderer part, Map<ModelRenderer, Boolean> seen) {
        if (part == null || seen.put(part, Boolean.TRUE) != null) return;
        parts.add(new Part(part));
        for (ModelRenderer child : ((ZoanTransitionRenderAccessors.Children)(Object)part).kazimod$children())
            add(child, seen);
    }

    void restore() {
        for (Part part : parts) part.restore();
    }

    private static final class Part {
        final ModelRenderer part;
        final float x, y, z, xRot, yRot, zRot;
        final boolean visible, mirror;
        Part(ModelRenderer part) {
            this.part = part;
            x = part.x; y = part.y; z = part.z;
            xRot = part.xRot; yRot = part.yRot; zRot = part.zRot;
            visible = part.visible; mirror = part.mirror;
        }
        void restore() {
            part.x = x; part.y = y; part.z = z;
            part.xRot = xRot; part.yRot = yRot; part.zRot = zRot;
            part.visible = visible; part.mirror = mirror;
        }
    }
}
