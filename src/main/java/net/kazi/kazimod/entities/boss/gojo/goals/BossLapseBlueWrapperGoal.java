package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.boss.gojo.BossLapseBlueAbility;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.kazi.kazimod.entities.projectiles.HollowNukeProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

public class BossLapseBlueWrapperGoal extends Goal {

    private final MobEntity entity;
    private BossLapseBlueAbility ability;

    public BossLapseBlueWrapperGoal(final MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private BossLapseBlueAbility getAbility() {
        if (ability == null) {
            IAbilityData data = AbilityDataCapability.get(entity);
            if (data != null)
                ability = (BossLapseBlueAbility) data.getEquippedAbility(BossLapseBlueAbility.INSTANCE);
        }
        return ability;
    }

    @Override
    public boolean canUse() {
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        LivingEntity target = entity.getTarget();
        if (target == null) return false;
        if (entity.distanceTo(target) > 22.0) return false;
        if (entity instanceof GojoBossEntity) {
            GojoBossEntity boss = (GojoBossEntity) entity;
            if (!boss.hollowPurpleFired) return false;
            if (boss.hollowNukeQueued) return false;
        }
        if (HollowNukeProjectile.ACTIVE_PROJECTILES.containsKey(entity.getUUID())) return false;
        BossLapseBlueAbility a = getAbility();
        if (a == null) return false;
        return !a.getComponent(ModAbilityKeys.COOLDOWN).map(c -> c.isOnCooldown()).orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        BossLapseBlueAbility a = getAbility();
        return a != null && a.isContinuous();
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target != null) GoalUtil.lookAtEntity(entity, target);
        BossLapseBlueAbility a = getAbility();
        if (a != null) a.use(entity);
    }

    @Override
    public void tick() {
        LivingEntity target = entity.getTarget();
        if (target != null && target.isAlive()) GoalUtil.lookAtEntity(entity, target);
    }

    @Override
    public void stop() {}
}