package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoRocketRework;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.MobEntity;

/**
 * Uses GomuGomuNoRocketRework (Rocket / Dawn Rocket).
 * Switches to Dawn Rocket mode when Gear Fifth awakens.
 */
public class LuffyRocketWrapperGoal extends DirectAbilityGoal<GomuGomuNoRocketRework> {

    private boolean g5ModeApplied = false;

    public LuffyRocketWrapperGoal(MobEntity entity) {
        super(entity, GomuGomuNoRocketRework.INSTANCE);
    }

    @Override
    protected boolean canUseExtra() {
        applyGearFifthModeIfNeeded();
        return true;
    }

    @Override protected boolean canContinueExtra() { return isContinuous() || isCharging(); }

    private void applyGearFifthModeIfNeeded() {
        if (g5ModeApplied) return;
        if (!(entity instanceof LuffyBossEntity)) return;
        if (!((LuffyBossEntity) entity).isGear5Awakened()) return;
        GomuGomuNoRocketRework ability = getAbility();
        if (ability == null) return;
        ability.switchDawnRocket(entity);
        g5ModeApplied = true;
    }
}