package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.BrawlerRework.SuplexRework;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.pathfinding.PathNavigator;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.api.entities.ai.AbilityWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/**
 * Wraps SuplexRework exactly as Garp's SuplexWrapperGoal does for the base
 * SuplexAbility — pattern confirmed from bytecode.
 */
public class BossSuplexWrapperGoal extends AbilityWrapperGoal<MobEntity, SuplexRework> {

    private LivingEntity target;
    private final float distance;

    public BossSuplexWrapperGoal(MobEntity entity) {
        super(entity, SuplexRework.INSTANCE);
        this.distance = getAbility() != null
                ? getAbility().getComponent(ModAbilityKeys.RANGE)
                .map(c -> ((xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent) c).getRange())
                .orElse(5.0f)
                : 5.0f;
    }

    @Override
    public boolean canUseWrapper() {
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        target = entity.getTarget();
        if (target == null) return false;
        if (!GoalUtil.canSee(entity, target)) return false;
        return !GoalUtil.isOutsideDistance(entity, target, distance);
    }

    @Override
    public boolean canContinueToUseWrapper() {
        return GoalUtil.hasAliveTarget(entity);
    }

    @Override
    public void startWrapper() {
        if (target == null) return;
        GoalUtil.lookAtEntity(entity, target);
        PathNavigator nav = entity.getNavigation();
        if (!nav.isDone()) nav.stop();
        nav.moveTo(target, 1.0);
    }

    @Override
    public void tickWrapper() {
        if (target != null && target.isAlive()) {
            GoalUtil.lookAtEntity(entity, target);
        }
    }

    @Override
    public void stopWrapper() {
        target = null;
    }
}