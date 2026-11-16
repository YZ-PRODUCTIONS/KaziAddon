package net.kazi.kazimod.abilities.NagiAwakening;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.events.ProjectileHitEvent;

/** Projectile and beam contact protection owned by the renamed Imagine Breaker passive. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class ImagineBreakerNegation {
    public static final double RANGE = 3.0D;
    private static final Map<LivingEntity, Long> LAST_PARTICLES = new WeakHashMap<>();

    private ImagineBreakerNegation() {}

    public static void tick(LivingEntity user) {
        if (IllusionShatterAbility.getActiveNegation(user) == null) return;
        Vector3d eye = user.getEyePosition(1.0F);
        for (Entity projectile : user.level.getEntities(user, user.getBoundingBox().inflate(RANGE),
                entity -> NagiAwakeningSupport.canNegate(user, entity))) {
            Vector3d point = projectile.getBoundingBox().getCenter();
            if (canReach(user, eye, point)) erase(user, projectile, point);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (negateAtImpact(event.getEntity(), event.getRayTraceResult())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAbilityProjectileImpact(ProjectileHitEvent event) {
        // MMNM reports impacts before damage, splash effects and custom hit callbacks.
        if (negateAtImpact(event.getProjectile(), event.getHit())) event.setCanceled(true);
    }

    private static boolean negateAtImpact(Entity projectile, RayTraceResult hit) {
        if (projectile == null || projectile.level.isClientSide || !projectile.isAlive()
                || !NagiAwakeningSupport.isFruitProjectile(projectile)
                || hit == null || hit.getType() == RayTraceResult.Type.MISS) return false;
        Vector3d point = hit.getLocation();
        for (LivingEntity user : projectile.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(point, point).inflate(RANGE))) {
            if (IllusionShatterAbility.getActiveNegation(user) == null
                    || !NagiAwakeningSupport.canNegate(user, projectile)) continue;
            if (!canReach(user, user.getEyePosition(1.0F), point)) continue;
            erase(user, projectile, point);
            return true;
        }
        return false;
    }

    /** Beam ticks call this before damage, knockback, impact explosions or terrain edits. */
    public static boolean negateBeam(Entity beam, Entity owner, AbilityCore<?> source,
            AxisAlignedBB bounds, Function<Vector3d, Vector3d> closestPoint) {
        if (beam == null || beam.level.isClientSide || !beam.isAlive()
                || bounds == null || closestPoint == null
                || !((source != null && source.getCategory() == AbilityCategory.DEVIL_FRUITS)
                    || NagiAwakeningSupport.isFruitProjectile(beam))) return false;
        for (LivingEntity user : beam.level.getEntitiesOfClass(LivingEntity.class, bounds.inflate(RANGE))) {
            if (owner instanceof LivingEntity && !NagiAwakeningSupport.isEnemy(user, owner)) continue;
            if (owner != null && (owner == user || user.isAlliedTo(owner) || owner.isAlliedTo(user))) continue;
            if (IllusionShatterAbility.getActiveNegation(user) == null) continue;
            Vector3d eye = user.getEyePosition(1.0F);
            Vector3d point = closestPoint.apply(eye);
            if (point == null || !canReach(user, eye, point)) continue;
            erase(user, beam, point);
            return true;
        }
        return false;
    }

    public static boolean negateBeam(Entity beam, Entity owner, AbilityCore<?> source,
            Vector3d start, Vector3d end, double radius) {
        if (start == null || end == null || !Double.isFinite(radius) || radius < 0
                || !Double.isFinite(start.lengthSqr()) || !Double.isFinite(end.lengthSqr())
                || start.distanceToSqr(end) < 1.0E-18) return false;
        return negateBeam(beam, owner, source, new AxisAlignedBB(start, end).inflate(radius), eye -> {
            double[] point = ImagineBreakerNegationGeometry.closestCylinderPoint(eye.x, eye.y, eye.z,
                    start.x, start.y, start.z, end.x, end.y, end.z, radius);
            return point == null ? null : new Vector3d(point[0], point[1], point[2]);
        });
    }

    private static boolean canReach(LivingEntity user, Vector3d eye, Vector3d point) {
        if (!ImagineBreakerNegationGeometry.withinRadius(
                point.x - eye.x, point.y - eye.y, point.z - eye.z, RANGE)) return false;
        // Stop just before a hit surface; nearby impacts cannot be negated through walls.
        Vector3d sightPoint = point.add(eye.subtract(point).normalize().scale(.01));
        return DragonStrikeAbility.clearPath(user, eye, sightPoint);
    }

    private static void erase(LivingEntity user, Entity attack, Vector3d point) {
        attack.remove();
        NagiAwakeningSupport.playNullificationSound(user, point);
        long tick = user.level.getGameTime();
        Long last = LAST_PARTICLES.put(user, tick);
        if (last == null || last.longValue() != tick) NagiAwakeningSupport.shatterParticles(user, point);
    }
}
