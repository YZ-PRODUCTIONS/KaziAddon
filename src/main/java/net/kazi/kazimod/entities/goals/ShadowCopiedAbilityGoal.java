package net.kazi.kazimod.entities.goals;

import net.kazi.kazimod.entities.boss.luffy.goals.DirectAbilityGoal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

public class ShadowCopiedAbilityGoal<A extends Ability> extends DirectAbilityGoal<A> {

    private final double minDistance;
    private final double maxDistance;
    private final int randomInterval;

    public ShadowCopiedAbilityGoal(MobEntity entity, AbilityCore<A> core) {
        this(entity, core, 0.0, 64.0, 6);
    }

    public ShadowCopiedAbilityGoal(MobEntity entity, AbilityCore<A> core,
                                   double minDistance, double maxDistance, int randomInterval) {
        super(entity, core);
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.randomInterval = Math.max(1, randomInterval);
    }

    @Override
    protected boolean canUseExtra() {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        double distance = entity.distanceTo(target);
        if (distance < minDistance || distance > maxDistance) {
            return false;
        }

        A ability = getAbility();
        if (ability == null) {
            return false;
        }

        if (!entity.canSee(target) && maxDistance <= 10.0D) {
            return false;
        }

        return entity.getRandom().nextInt(randomInterval) == 0;
    }

    @Override
    protected boolean canContinueExtra() {
        return isCharging() || isContinuous();
    }
}
