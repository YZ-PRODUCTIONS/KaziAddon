package net.kazi.kazimod.entities.boss.sukuna.goals;

import net.kazi.kazimod.abilities.KamaRework.SpiderwebCleaveAbility;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

public class BossSpiderwebCleaveWrapperGoal extends Goal {

    private final MobEntity entity;
    private SpiderwebCleaveAbility ability;
    private boolean hasUsed = false;

    public BossSpiderwebCleaveWrapperGoal(MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private SpiderwebCleaveAbility getAbility() {
        if (ability == null) {
            IAbilityData data = AbilityDataCapability.get(entity);
            if (data != null)
                ability = (SpiderwebCleaveAbility) data.getEquippedAbility(SpiderwebCleaveAbility.INSTANCE);
        }
        return ability;
    }

    @Override
    public boolean canUse() {
        if (hasUsed) return false;
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        SpiderwebCleaveAbility a = getAbility();
        if (a == null) return false;
        if (a.isCharging()) return false;
        return !a.getComponent(ModAbilityKeys.COOLDOWN)
                .map(c -> c.isOnCooldown())
                .orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        SpiderwebCleaveAbility a = getAbility();
        return a != null && a.isCharging();
    }

    @Override
    public void start() {
        hasUsed = true;
        if (entity.getTarget() != null) GoalUtil.lookAtEntity(entity, entity.getTarget());
        SpiderwebCleaveAbility a = getAbility();
        if (a != null) a.use(entity);
    }

    @Override
    public void tick() {
        if (entity.getTarget() != null && entity.getTarget().isAlive())
            GoalUtil.lookAtEntity(entity, entity.getTarget());
    }

    @Override
    public void stop() {}
}