package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.kazi.kazimod.init.KaziItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

/** Shared eligibility, targeting and attributed damage for the four awakening moves. */
public final class YagiAwakeningSupport {
    private YagiAwakeningSupport() {}

    public static boolean canUnlock(LivingEntity user) {
        if (user == null || KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT == null
                || !KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT.isPresent()) return false;
        IDevilFruit fruit = DevilFruitCapability.get(user);
        return fruit != null && fruit.hasAwakenedFruit()
                && fruit.hasDevilFruit(KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT.get());
    }

    public static boolean enhanced(LivingEntity user) {
        return user != null && YagiAwakeningMath.enhanced(user.getHealth(), user.getMaxHealth());
    }

    public static boolean isEnemy(LivingEntity user, Entity other) {
        return user != null && other instanceof LivingEntity && other != user && other.level == user.level
                && other.isAlive() && !other.isSpectator()
                && !(other instanceof PlayerEntity && ((PlayerEntity) other).isCreative())
                && !user.isAlliedTo(other) && !other.isAlliedTo(user)
                && ModEntityPredicates.getEnemyFactions(user).test(other);
    }

    /** Auto-targeting ignores peaceful wildlife; opposing players sort ahead of hostile mobs. */
    public static List<LivingEntity> targets(LivingEntity user, double range, int limit) {
        return targets(user, range, limit,
                Comparator.<LivingEntity>comparingInt(target -> target instanceof PlayerEntity ? 0 : 1)
                        .thenComparingDouble(user::distanceToSqr).thenComparingInt(Entity::getId));
    }

    /** Shocker Breaker EX samples the closest eligible enemies again for each warning round. */
    public static List<LivingEntity> nearestTargets(LivingEntity user, double range, int limit) {
        return targets(user, range, limit,
                Comparator.<LivingEntity>comparingDouble(user::distanceToSqr).thenComparingInt(Entity::getId));
    }

    private static List<LivingEntity> targets(LivingEntity user, double range, int limit,
                                             Comparator<LivingEntity> order) {
        double radius = Math.max(0, Math.min(100, range));
        return user.level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(radius),
                target -> isEnemy(user, target) && user.distanceToSqr(target) <= radius * radius
                        && (target instanceof PlayerEntity || target instanceof MonsterEntity
                            || target instanceof MobEntity && ((MobEntity) target).getTarget() == user)
                        && user.canSee(target)).stream()
                .sorted(order)
                .limit(Math.max(0, limit)).collect(Collectors.toList());
    }

    public static Vector3d aimPoint(LivingEntity user, double range) {
        Vector3d start = user.getEyePosition(1);
        Vector3d end = start.add(user.getLookAngle().scale(Math.min(100, Math.max(1, range))));
        return user.level.clip(new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, user)).getLocation();
    }

    /** Finds a nearby floor without loading chunks. Missing floors retain the requested position. */
    public static Vector3d groundPoint(LivingEntity user, Vector3d point) {
        if (!user.level.hasChunkAt(new BlockPos(point))) return point;
        Vector3d start = new Vector3d(point.x, Math.min(255, point.y + 2), point.z);
        Vector3d end = new Vector3d(point.x, Math.max(0, point.y - 64), point.z);
        BlockRayTraceResult hit = user.level.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        return hit.getType() == RayTraceResult.Type.BLOCK ? hit.getLocation().add(0, 0.025, 0) : point;
    }

    public static boolean clearPath(LivingEntity user, Vector3d from, Vector3d to) {
        return user.level.clip(new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, user)).getType() == RayTraceResult.Type.MISS;
    }

    public static boolean hurt(LivingEntity user, LivingEntity target, AbilityCore<?> core, float damage) {
        if (user == null || user.level.isClientSide || !user.isAlive() || !isEnemy(user, target)
                || core == null || !Float.isFinite(damage) || damage <= 0) return false;
        return target.hurt(new AbilityDamageSource(core.getId(), user, core), damage);
    }
}
