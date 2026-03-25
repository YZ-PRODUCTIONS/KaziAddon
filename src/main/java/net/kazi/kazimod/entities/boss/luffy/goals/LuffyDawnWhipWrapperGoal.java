package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoDawnWhipRework;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.MobEntity;

/**
 * Uses GomuGomuNoDawnWhipRework — Gear Fifth only (ability's own canUseCheck
 * gates it behind GearFifthRework.isContinuous, so it blocks itself before G5).
 * We additionally gate it in canUseExtra for clarity.
 */
public class LuffyDawnWhipWrapperGoal extends DirectAbilityGoal<GomuGomuNoDawnWhipRework> {
    public LuffyDawnWhipWrapperGoal(MobEntity entity) {
        super(entity, GomuGomuNoDawnWhipRework.INSTANCE);
    }

    @Override
    protected boolean canUseExtra() {
        return entity instanceof LuffyBossEntity && ((LuffyBossEntity) entity).isGear5Awakened();
    }

    @Override protected boolean canContinueExtra() { return isContinuous(); }
}