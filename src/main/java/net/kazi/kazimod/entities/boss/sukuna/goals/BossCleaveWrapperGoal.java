package net.kazi.kazimod.entities.boss.sukuna.goals;

import net.kazi.kazimod.abilities.KamaRework.CleaveAbility;
import net.minecraft.entity.MobEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.api.entities.ai.AbilityWrapperGoal;

public class BossCleaveWrapperGoal extends AbilityWrapperGoal<MobEntity, CleaveAbility> {

    public BossCleaveWrapperGoal(MobEntity entity) {
        super(entity, CleaveAbility.INSTANCE);
    }

    @Override public boolean canUseWrapper() { return GoalUtil.hasAliveTarget(entity); }

    @Override
    public boolean canContinueToUseWrapper() {
        Ability a = (Ability) getAbility();
        return a != null && a.isCharging();
    }

    @Override public void startWrapper() {
        if (entity.getTarget() != null) GoalUtil.lookAtEntity(entity, entity.getTarget());
    }

    @Override public void tickWrapper() {
        if (entity.getTarget() != null && entity.getTarget().isAlive())
            GoalUtil.lookAtEntity(entity, entity.getTarget());
    }

    @Override public void stopWrapper() {}
}