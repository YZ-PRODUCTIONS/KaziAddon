package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.BrawlerRework.SpinningBrawlRework;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.pathfinding.PathNavigator;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.api.entities.ai.AbilityWrapperGoal;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/**
 * Wraps SpinningBrawlRework exactly as Garp's SpinningBrawlWrapperGoal does
 * for the base SpinningBrawlAbility — pattern confirmed from bytecode.
 *
 * canUseWrapper: hasAliveTarget + canSee + isOutsideDistance(range from RangeComponent)
 * startWrapper: lookAtEntity + navigate to target
 * tickWrapper: keep looking at target while spinning
 */
public class BossSpinningBrawlWrapperGoal extends AbilityWrapperGoal<MobEntity, SpinningBrawlRework> {

    private LivingEntity target;
    private final float distance;

    public BossSpinningBrawlWrapperGoal(MobEntity entity) {
        super(entity, SpinningBrawlRework.INSTANCE);
        // Read range from RangeComponent; fall back to 5 blocks
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
        // Only use when close enough to actually grab
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