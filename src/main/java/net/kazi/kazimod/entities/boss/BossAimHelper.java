package net.kazi.kazimod.entities.boss;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Random;

/**
 * Lead-prediction targeting for Gojo boss projectiles.
 * Predicts where the target will be when the projectile arrives
 * and adds small random spread for ~90% accuracy.
 */
public final class BossAimHelper {

    private BossAimHelper() {}

    /** Spread factor. 0.08 ≈ 90% accuracy. 0.0 = perfect aimbot. */
    private static final double SPREAD = 0.08;
    private static final Random RNG    = new Random();

    /**
     * Returns a normalised direction from shooter toward where target will be
     * when a projectile at projectileSpeed (blocks/tick) arrives.
     */
    public static Vector3d leadTarget(LivingEntity shooter, LivingEntity target,
                                      double projectileSpeed) {
        Vector3d firePos     = shooter.position().add(0, shooter.getEyeHeight() * 0.9, 0);
        Vector3d targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double   dist        = firePos.distanceTo(targetCenter);
        double   travelTicks = dist / projectileSpeed;

        Vector3d predicted = targetCenter.add(target.getDeltaMovement().scale(travelTicks));
        Vector3d dir       = predicted.subtract(firePos).normalize();

        double spreadScale = Math.min(1.0, dist / 16.0) * SPREAD;
        dir = dir.add(
                (RNG.nextDouble() * 2 - 1) * spreadScale,
                (RNG.nextDouble() * 2 - 1) * spreadScale * 0.5,
                (RNG.nextDouble() * 2 - 1) * spreadScale
        ).normalize();

        return dir;
    }
}