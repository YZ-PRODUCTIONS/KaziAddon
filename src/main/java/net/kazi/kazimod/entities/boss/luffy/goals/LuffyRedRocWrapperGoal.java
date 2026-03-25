package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoRedRocAbility;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;

/**
 * Uses GomuGomuNoRedRocAbility (Red Roc only — never switches to Bajrang Gun).
 */
public class LuffyRedRocWrapperGoal extends DirectAbilityGoal<GomuGomuNoRedRocAbility> {

    public LuffyRedRocWrapperGoal(MobEntity entity) {
        super(entity, GomuGomuNoRedRocAbility.INSTANCE);
    }

    @Override
    protected boolean canUseExtra() {
        LivingEntity t = entity.getTarget();
        return t != null && GoalUtil.canSee(entity, t);
    }

    @Override protected boolean canContinueExtra() { return isCharging(); }

    @Override
    protected void onTick() {
        LivingEntity t = entity.getTarget();
        if (t == null) return;
        entity.getNavigation().stop();
        aimAtTarget();
    }
}