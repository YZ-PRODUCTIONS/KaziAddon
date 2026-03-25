package net.kazi.kazimod.entities.boss.sukuna.goals;

import net.kazi.kazimod.abilities.KamaRework.SpiderwebCleaveAbility;
import net.minecraft.entity.MobEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.api.entities.ai.AbilityWrapperGoal;

public class BossSpiderwebCleaveWrapperGoal extends AbilityWrapperGoal<MobEntity, SpiderwebCleaveAbility> {

    public BossSpiderwebCleaveWrapperGoal(MobEntity entity) {
        super(entity, SpiderwebCleaveAbility.INSTANCE);
    }

    @Override public boolean canUseWrapper() { return GoalUtil.hasAliveTarget(entity); }

    @Override
    public boolean canContinueToUseWrapper() {
        IAbility a = getAbility();
        return a != null && a.isCharging();
    }

    @Override
    public void startWrapper() {
        if (entity.getTarget() != null) GoalUtil.lookAtEntity(entity, entity.getTarget());
    }

    @Override
    public void tickWrapper() {
        if (entity.getTarget() != null && entity.getTarget().isAlive())
            GoalUtil.lookAtEntity(entity, entity.getTarget());
    }

    @Override public void stopWrapper() {}
}