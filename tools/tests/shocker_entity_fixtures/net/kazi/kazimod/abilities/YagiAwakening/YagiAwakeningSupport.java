package net.kazi.kazimod.abilities.YagiAwakening;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
public final class YagiAwakeningSupport {
    public static boolean enhanced(LivingEntity owner) {
        return owner != null && YagiAwakeningMath.enhanced(owner.getHealth(), owner.getMaxHealth());
    }
    public static List<LivingEntity> nearestTargets(LivingEntity owner, double range, int limit) {
        return owner.level.entities.stream().filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity).filter(target -> isEnemy(owner, target) && owner.distanceToSqr(target) <= range * range)
                .sorted(Comparator.comparingDouble(owner::distanceToSqr).thenComparingInt(Entity::getId))
                .limit(limit).collect(Collectors.toList());
    }
    public static Vector3d groundPoint(LivingEntity owner, Vector3d point) { return point; }
    public static Vector3d aimPoint(LivingEntity owner, double range) {
        return owner.aimPoint != null ? owner.aimPoint : owner.position().add(20, 0, 0);
    }
    public static boolean clearPath(LivingEntity owner, Vector3d from, Vector3d to) { return true; }
    public static boolean isEnemy(LivingEntity owner, Entity candidate) {
        return candidate instanceof LivingEntity && candidate != owner && candidate.isAlive() && ((LivingEntity) candidate).hostile;
    }
    public static boolean hurt(LivingEntity owner, LivingEntity target, Object core, float damage) {
        target.damageAttempts++;
        if (target.invulnerable) return false;
        target.successfulHits++;
        target.damageAmounts.add(damage);
        target.hitTimes.add(owner.level.getGameTime());
        return true;
    }
}
