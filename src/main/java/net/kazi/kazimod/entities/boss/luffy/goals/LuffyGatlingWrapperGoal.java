package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoGatlingRework;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.MobEntity;

/**
 * Uses GomuGomuNoGatlingRework (Gatling/Jet Gatling/Dawn Gatling).
 * Switches to Dawn Gatling mode when Gear Fifth awakens.
 */
public class LuffyGatlingWrapperGoal extends DirectAbilityGoal<GomuGomuNoGatlingRework> {

    private boolean g5ModeApplied = false;

    public LuffyGatlingWrapperGoal(MobEntity entity) {
        super(entity, GomuGomuNoGatlingRework.INSTANCE);
    }

    @Override
    protected boolean canUseExtra() {
        applyGearFifthModeIfNeeded();
        return true;
    }

    @Override protected boolean canContinueExtra() { return !isOnCooldown(); }

    private void applyGearFifthModeIfNeeded() {
        if (g5ModeApplied) return;
        if (!(entity instanceof LuffyBossEntity)) return;
        if (!((LuffyBossEntity) entity).isGear5Awakened()) return;
        GomuGomuNoGatlingRework ability = getAbility();
        if (ability == null) return;
        ability.switchFifthGear(entity);
        g5ModeApplied = true;
    }
}