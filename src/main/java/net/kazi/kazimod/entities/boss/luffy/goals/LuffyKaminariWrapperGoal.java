package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.abilities.boss.luffy.BossKaminariAbility;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.MobEntity;

/**
 * Uses BossKaminariAbility — Gear Fifth only.
 * Boss-specific version stays grounded and aims via BossAimHelper internally.
 * The goal just needs to call use() once and let canContinueExtra track the phases.
 */
public class LuffyKaminariWrapperGoal extends DirectAbilityGoal<BossKaminariAbility> {
    public LuffyKaminariWrapperGoal(MobEntity entity) {
        super(entity, BossKaminariAbility.INSTANCE);
    }

    @Override
    protected boolean canUseExtra() {
        return entity instanceof LuffyBossEntity && ((LuffyBossEntity) entity).isGear5Awakened();
    }

    @Override
    protected boolean canContinueExtra() {
        return isCharging() || isContinuous();
    }
    // No aimAtTarget override needed — BossKaminariAbility.tickBeamEvent calls aimAtTarget internally
}