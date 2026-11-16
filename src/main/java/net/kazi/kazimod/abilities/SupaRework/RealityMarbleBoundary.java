package net.kazi.kazimod.abilities.SupaRework;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;

/** Uses the actual synchronized sky sphere, so collision shares its center, radius and lifetime. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class RealityMarbleBoundary {
    private static final ConcurrentLinkedQueue<WeakReference<SphereEntity>> SPHERES = new ConcurrentLinkedQueue<>();
    private RealityMarbleBoundary() { }
    @SubscribeEvent public static void joined(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof SphereEntity) SPHERES.add(new WeakReference<>((SphereEntity) event.getEntity()));
    }
    @SubscribeEvent public static void unloaded(WorldEvent.Unload event) {
        SPHERES.removeIf(ref -> {
            SphereEntity sphere = ref.get();
            return sphere == null || sphere.level == event.getWorld();
        });
    }
    private static boolean marble(SphereEntity sphere) {
        ResourceLocation[] textures = sphere.getTexture();
        if (textures == null) return false;
        for (ResourceLocation texture : textures) {
            if (texture != null && "kazimod".equals(texture.getNamespace())
                    && ("textures/skyboxes/reality_marble_blue_sky.png".equals(texture.getPath())
                    || "textures/skyboxes/reality_marble_orange_sky.png".equals(texture.getPath()))) return true;
        }
        return false;
    }
    /** Destination checks also cover abilities that reposition directly instead of moving. */
    public static boolean blocksTeleport(Entity mover, double x, double y, double z) {
        // Entity's constructor calls setPos before ServerPlayerEntity initializes its game mode.
        // Check initialization before calling overrides such as isSpectator().
        if (!(mover instanceof net.minecraft.entity.LivingEntity) || mover.tickCount <= 0
                || mover.isSpectator()) return false;
        Vector3d center = mover.getBoundingBox().getCenter();
        Vector3d destination = center.add(x-mover.getX(), y-mover.getY(), z-mover.getZ());
        for (WeakReference<SphereEntity> ref : SPHERES) {
            SphereEntity sphere = ref.get();
            if (sphere == null || !sphere.isAlive()) { SPHERES.remove(ref); continue; }
            if (sphere.level != mover.level || !marble(sphere)) continue;
            double radius = sphere.getRadius();
            if (MarbleBoundaryMath.teleportExits(center.distanceToSqr(sphere.position()),
                    destination.distanceToSqr(sphere.position()), radius)) return true;
        }
        return false;
    }
    public static Vector3d collide(Entity mover, Vector3d movement) {
        if (mover.tickCount <= 0 || mover.isSpectator() || mover.noPhysics
                || movement.lengthSqr() < 1.0E-14) return movement;
        return clipMovement(mover, movement);
    }

    /** Let traced dashes finish at the shell instead of rejecting their entire teleport. */
    public static Vector3d clampTeleportDestination(Entity mover, Vector3d destination) {
        if (!(mover instanceof net.minecraft.entity.LivingEntity) || mover.tickCount <= 0
                || mover.isSpectator()) return destination;
        Vector3d clipped = clipMovement(mover, new Vector3d(destination.x - mover.getX(),
                destination.y - mover.getY(), destination.z - mover.getZ()));
        Vector3d result = new Vector3d(mover.getX() + clipped.x, mover.getY() + clipped.y,
                mover.getZ() + clipped.z);
        return blocksTeleport(mover, result.x, result.y, result.z) ? mover.position() : result;
    }

    private static Vector3d clipMovement(Entity mover, Vector3d movement) {
        AxisAlignedBB box = mover.getBoundingBox();
        Vector3d center = box.getCenter();
        // Enclose the whole hitbox so heads and corners cannot protrude through the sky shell.
        double padding = Math.sqrt(box.getXsize()*box.getXsize() + box.getYsize()*box.getYsize()
                + box.getZsize()*box.getZsize()) * .5 + .02;
        double fraction = 1;
        for (WeakReference<SphereEntity> ref : SPHERES) {
            SphereEntity sphere = ref.get();
            if (sphere == null || !sphere.isAlive()) { SPHERES.remove(ref); continue; }
            if (sphere.level != mover.level || sphere == mover || !marble(sphere)) continue;
            fraction = Math.min(fraction, MarbleBoundaryMath.fraction(center.x-sphere.getX(),
                    center.y-sphere.getY(), center.z-sphere.getZ(), movement.x, movement.y, movement.z,
                    sphere.getRadius(), padding));
        }
        return movement.scale(fraction);
    }
}
