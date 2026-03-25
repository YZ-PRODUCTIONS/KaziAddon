package net.kazi.kazimod.entities.boss.sukuna.goals;

import net.kazi.kazimod.abilities.boss.sukuna.BossFugaAbility;
import net.kazi.kazimod.entities.boss.sukuna.SukunaBossEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import java.util.EnumSet;

public class BossFugaWrapperGoal extends Goal {

    private final MobEntity entity;
    private BossFugaAbility ability;
    private boolean hasUsed = false;

    public BossFugaWrapperGoal(MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private BossFugaAbility getAbility() {
        if (ability == null) {
            IAbilityData data = AbilityDataCapability.get(entity);
            if (data != null)
                ability = (BossFugaAbility) data.getEquippedAbility(BossFugaAbility.INSTANCE);
        }
        return ability;
    }

    @Override
    public boolean canUse() {
        if (hasUsed) return false;
        if (!(entity instanceof SukunaBossEntity)) return false;
        SukunaBossEntity boss = (SukunaBossEntity) entity;
        // Fire once when boss is at 25% HP or below and target is on the ground
        if (boss.getHealth() / boss.getMaxHealth() > 0.25f) return false;
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        if (!entity.getTarget().isOnGround()) return false;
        BossFugaAbility a = getAbility();
        if (a == null) return false;
        return !a.isCharging();
    }

    @Override
    public boolean canContinueToUse() {
        BossFugaAbility a = getAbility();
        return a != null && a.isCharging();
    }

    @Override
    public void start() {
        hasUsed = true;
        if (entity.getTarget() != null) GoalUtil.lookAtEntity(entity, entity.getTarget());
        BossFugaAbility a = getAbility();
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