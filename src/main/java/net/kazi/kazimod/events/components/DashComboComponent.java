package net.kazi.kazimod.events.components;

import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponentKey;
import net.minecraft.util.ResourceLocation;


/**
 * Reusable combo component that executes
 * a dash action multiple times with a delay.
 */
public class DashComboComponent extends AbilityComponent<IAbility>
 {


    @FunctionalInterface
    public interface DashAction {
        void execute(LivingEntity entity, IAbility ability);
    }

    private final DashAction dashAction;

    private LivingEntity user;
    private int dashesRemaining;
    private int delayTicks;
    private int delayBetween;

    private boolean active = false;

     public static final AbilityComponentKey<DashComboComponent> KEY =
             new AbilityComponentKey<>(
                     new ResourceLocation("kazimod", "dash_combo")
             );



     public DashComboComponent(IAbility ability, DashAction action) {
        super(KEY, ((DashComboComponent) ability).getAbility());
        this.dashAction = action;
    }

    public void startCombo(LivingEntity entity, int totalDashes, int delayBetweenTicks) {
        this.user = entity;
        this.dashesRemaining = totalDashes;
        this.delayBetween = delayBetweenTicks;
        this.delayTicks = 0;
        this.active = true;
    }

    public void tick(IAbility ability) {
        if (!active || user == null) return;

        if (!user.isAlive()) {
            stopCombo();
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        if (dashesRemaining > 0) {
            dashAction.execute(user, ability);
            dashesRemaining--;

            if (dashesRemaining > 0) {
                delayTicks = delayBetween;
            } else {
                stopCombo();
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public void stopCombo() {
        active = false;
        user = null;
        dashesRemaining = 0;
        delayTicks = 0;
    }
}
