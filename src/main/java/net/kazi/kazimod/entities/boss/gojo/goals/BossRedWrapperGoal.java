package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.Koku.RedAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossRedAbility;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.kazi.kazimod.entities.boss.gojo.GojoCooldownTracker;
import net.kazi.kazimod.entities.projectiles.LapseBlueProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

public class BossRedWrapperGoal extends Goal {

    private final MobEntity entity;
    private BossRedAbility ability;

    public BossRedWrapperGoal(final MobEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private BossRedAbility getAbility() {
        if (ability == null) {
            IAbilityData data = AbilityDataCapability.get(entity);
            if (data != null)
                ability = (BossRedAbility) data.getEquippedAbility(BossRedAbility.INSTANCE);
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
        if (LapseBlueProjectile.ACTIVE_PROJECTILES.containsKey(entity.getUUID())) return false;
        // Reduced mutual cooldown from 5s to 2s
        if (GojoCooldownTracker.isOnCooldown(GojoCooldownTracker.LAST_MAX_BLUE_TIME, entity.getUUID())) return false;
        if (GojoCooldownTracker.isOnCooldown(GojoCooldownTracker.LAST_RED_TIME, entity.getUUID())) return false;
        BossRedAbility a = getAbility();
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
        if (target == null) return;

        if (entity instanceof GojoBossEntity) {
            GojoBossEntity boss = (GojoBossEntity) entity;
            IAbilityData data = AbilityDataCapability.get(boss);
            if (data != null) {
                RedAbility redAbility = (RedAbility) data.getEquippedAbility(RedAbility.INSTANCE);
                if (redAbility != null) {
                    boolean wantMax = boss.domainFinished
                            || boss.distanceTo(target) >= BossRedAbility.MAX_OUTPUT_RANGE;
                    redAbility.getComponent(ModAbilityKeys.ALT_MODE).ifPresent(c -> {
                        AltModeComponent alt = (AltModeComponent) c;
                        RedAbility.RedMode desired = wantMax
                                ? RedAbility.RedMode.MAX_OUTPUT
                                : RedAbility.RedMode.NORMAL;
                        if (!alt.isMode(desired)) alt.setMode(boss, desired);
                    });
                }
            }
        }

        GoalUtil.lookAtEntity(entity, target);
        BossRedAbility a = getAbility();
        if (a != null) a.use(entity);
    }

    @Override
    public void stop() {}
}