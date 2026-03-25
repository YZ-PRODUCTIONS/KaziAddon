package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.boss.gojo.BossHollowPurpleAbility;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import java.util.EnumSet;

public class BossHollowPurpleWrapperGoal extends Goal {

    private final MobEntity entity;
    private BossHollowPurpleAbility ability;

    public BossHollowPurpleWrapperGoal(MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private BossHollowPurpleAbility getAbility() {
        if (ability == null) {
            IAbilityData data = AbilityDataCapability.get(entity);
            if (data != null)
                ability = (BossHollowPurpleAbility) data.getEquippedAbility(BossHollowPurpleAbility.INSTANCE);
        }
        return ability;
    }

    @Override
    public boolean canUse() {
        if (!(entity instanceof GojoBossEntity)) return false;
        if (((GojoBossEntity) entity).hollowPurpleFired) return false;
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        BossHollowPurpleAbility a = getAbility();
        return a != null && !a.isCharging();
    }

    @Override
    public boolean canContinueToUse() {
        BossHollowPurpleAbility a = getAbility();
        return a != null && a.isCharging();
    }

    @Override
    public void start() {
        ((GojoBossEntity) entity).hollowPurpleFired = true;
        LivingEntity target = entity.getTarget();
        if (target != null) GoalUtil.lookAtEntity(entity, target);
        BossHollowPurpleAbility a = getAbility();
        if (a != null) a.use(entity);
    }

    @Override
    public void tick() {
        LivingEntity target = entity.getTarget();
        if (target != null && target.isAlive()) {
            GoalUtil.lookAtEntity(entity, target);
            double dx = target.getX() - entity.getX();
            double dy = (target.getY() + target.getBbHeight() * 0.5)
                    - (entity.getY() + entity.getEyeHeight());
            double dz = target.getZ() - entity.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            entity.yRot     = (float) (Math.atan2(-dx, dz) * (180.0 / Math.PI));
            entity.yHeadRot = entity.yRot;
            entity.xRot     = (float) (Math.atan2(-dy, horiz) * (180.0 / Math.PI));
        }
    }

    @Override
    public void stop() {}
}