package net.kazi.kazimod.entities.boss.sukuna.goals;

import net.kazi.kazimod.abilities.boss.sukuna.BossDismantleAbility;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

public class BossDismantleWrapperGoal extends Goal {

    private final MobEntity entity;
    private BossDismantleAbility ability;

    public BossDismantleWrapperGoal(MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private BossDismantleAbility getAbility() {
        if (ability == null) {
            IAbilityData data = AbilityDataCapability.get(entity);
            if (data != null)
                ability = (BossDismantleAbility) data.getEquippedAbility(BossDismantleAbility.INSTANCE);
        }
        return ability;
    }

    @Override
    public boolean canUse() {
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        BossDismantleAbility a = getAbility();
        if (a == null) return false;
        if (a.isCharging()) return false;
        // Check cooldown via component
        return !a.getComponent(ModAbilityKeys.COOLDOWN)
                .map(c -> c.isOnCooldown())
                .orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        BossDismantleAbility a = getAbility();
        return a != null && a.isCharging();
    }

    @Override
    public void start() {
        if (entity.getTarget() != null) GoalUtil.lookAtEntity(entity, entity.getTarget());
        BossDismantleAbility a = getAbility();
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