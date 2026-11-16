package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

/** One ally-healing pulse after Chaos Beam's finale has fully faded back to gameplay. */
public final class ChaosBeamHealing {
    public static final double RADIUS = 200D;
    public static final float MAX_HEALTH_FRACTION = .25F;

    private ChaosBeamHealing() { }

    public static void healAllies(LivingEntity caster) {
        if (caster == null || !(caster.level instanceof ServerWorld) || caster.level.isClientSide
                || !caster.isAlive() || caster.removed || caster.isSpectator()) return;
        Predicate<Entity> friendly = ModEntityPredicates.getFriendlyFactions(caster);
        for (LivingEntity ally : caster.level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(RADIUS))) {
            if (ally == caster || ally.level != caster.level || !ally.isAlive() || ally.removed
                    || ally.isSpectator() || caster.distanceToSqr(ally) > RADIUS * RADIUS
                    || !(caster.isAlliedTo(ally) || ally.isAlliedTo(caster) || friendly.test(ally))) continue;
            float healing = ally.getMaxHealth() * MAX_HEALTH_FRACTION;
            if (Float.isFinite(healing) && healing > 0) ally.heal(healing);
        }
    }
}
