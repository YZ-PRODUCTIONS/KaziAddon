package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.boss.gojo.BossMaxOutputLapseBlueAbility;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.kazi.kazimod.entities.boss.gojo.GojoCooldownTracker;
import net.kazi.kazimod.entities.projectiles.HollowNukeProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

public class BossMaxOutputLapseBlueWrapperGoal extends Goal {

    private final MobEntity entity;
    private BossMaxOutputLapseBlueAbility ability;

    public BossMaxOutputLapseBlueWrapperGoal(final MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private BossMaxOutputLapseBlueAbility getAbility() {
        if (ability == null) {
            IAbilityData data = AbilityDataCapability.get(entity);
            if (data != null)
                ability = (BossMaxOutputLapseBlueAbility) data.getEquippedAbility(BossMaxOutputLapseBlueAbility.INSTANCE);
        }
        return ability;
    }

    @Override
    public boolean canUse() {
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        if (entity instanceof GojoBossEntity) {
            GojoBossEntity boss = (GojoBossEntity) entity;
            if (!boss.hollowPurpleFired) return false;
            if (boss.hollowNukeQueued) return false;
        }
        if (HollowNukeProjectile.ACTIVE_PROJECTILES.containsKey(entity.getUUID())) return false;
        // Reduced mutual cooldown from 5s to 2s
        if (GojoCooldownTracker.isOnCooldown(GojoCooldownTracker.LAST_MAX_BLUE_TIME, entity.getUUID())) return false;
        if (GojoCooldownTracker.isOnCooldown(GojoCooldownTracker.LAST_RED_TIME, entity.getUUID())) return false;
        BossMaxOutputLapseBlueAbility a = getAbility();
        if (a == null) return false;
        return !a.getComponent(ModAbilityKeys.COOLDOWN).map(c -> c.isOnCooldown()).orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = entity.getTarget();
        if (target != null) GoalUtil.lookAtEntity(entity, target);
        BossMaxOutputLapseBlueAbility a = getAbility();
        if (a != null) a.use(entity);
    }

    @Override
    public void stop() {}
}