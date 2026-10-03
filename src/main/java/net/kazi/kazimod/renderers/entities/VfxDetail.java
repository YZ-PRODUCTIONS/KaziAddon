package net.kazi.kazimod.renderers.entities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.settings.GraphicsFanciness;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

/** Client-only visual budgets. Never changes ability timing, entities or hitboxes. */
public final class VfxDetail {
    private VfxDetail() { }

    public static int level(Entity entity, EntityRendererManager renderer, double visualRadius) {
        int detail = settingsLevel();
        Vector3d camera = renderer.camera.getPosition();
        double distance = camera.distanceToSqr(entity.position());
        // Measure from the effect's edge, not only its origin: large blasts must
        // remain detailed when the camera is beside their far end or inside them.
        if (distance > (visualRadius + 96) * (visualRadius + 96)) return 2;
        if (distance > (visualRadius + 40) * (visualRadius + 40)) return Math.max(1, detail);
        return detail;
    }

    /** Use the distance to an elongated effect's surface, rather than its enclosing sphere. */
    public static int levelForDistance(double distance) {
        int detail = settingsLevel();
        return distance > 96 ? 2 : distance > 40 ? Math.max(1, detail) : detail;
    }

    private static int settingsLevel() {
        Minecraft client = Minecraft.getInstance();
        return client.options.particles == ParticleStatus.MINIMAL ? 2
                : client.options.particles == ParticleStatus.DECREASED
                || client.options.graphicsMode == GraphicsFanciness.FAST ? 1 : 0;
    }

    static int count(int detail, int high, int medium, int low) {
        return detail == 0 ? high : detail == 1 ? medium : low;
    }

    static boolean visible(Entity entity, ClippingHelper frustum, double x, double y, double z,
                             double radius) {
        if (!entity.shouldRender(x, y, z)) return false;
        // Entity collision boxes are too small for these visuals. Cull their full
        // silhouettes instead, even when the entity deliberately has noCulling set.
        return frustum.isVisible(new AxisAlignedBB(entity.getX() - radius, entity.getY() - radius,
                entity.getZ() - radius, entity.getX() + radius, entity.getY() + radius,
                entity.getZ() + radius));
    }
}
