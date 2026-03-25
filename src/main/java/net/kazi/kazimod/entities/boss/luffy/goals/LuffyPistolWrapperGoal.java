package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoPistolRework;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.entity.MobEntity;

/**
 * Uses GomuGomuNoPistolRework (Pistol/Jet Pistol/King Kong Gun/Star Gun).
 * Switches to Star Gun mode when Gear Fifth awakens (same as GearFifthRework.startContinuityEvent).
 */
public class LuffyPistolWrapperGoal extends DirectAbilityGoal<GomuGomuNoPistolRework> {

    private boolean g5ModeApplied = false;

    public LuffyPistolWrapperGoal(MobEntity entity) {
        super(entity, GomuGomuNoPistolRework.INSTANCE);
    }

    @Override
    protected boolean canUseExtra() {
        applyGearFifthModeIfNeeded();
        return true;
    }

    private void applyGearFifthModeIfNeeded() {
        if (g5ModeApplied) return;
        if (!(entity instanceof LuffyBossEntity)) return;
        if (!((LuffyBossEntity) entity).isGear5Awakened()) return;
        GomuGomuNoPistolRework ability = getAbility();
        if (ability == null) return;
        ability.switchFifthGear(entity);
        g5ModeApplied = true;
    }
}