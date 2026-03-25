package net.kazi.kazimod.entities.boss.sukuna.goals;

import net.kazi.kazimod.abilities.boss.sukuna.BossMalevolentShrineAbility;
import net.kazi.kazimod.entities.boss.sukuna.SukunaBossEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

public class SukunaBossDomainGoal extends Goal {

    private final SukunaBossEntity boss;
    private boolean domainUsed    = false;
    private boolean domainStarted = false;

    public SukunaBossDomainGoal(SukunaBossEntity boss) {
        this.boss = boss;
        // No flags — don't compete with MOVE/LOOK so nothing can interrupt it
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (domainUsed) return false;
        if (boss.getHealth() / boss.getMaxHealth() > 0.5f) return false;
        if (boss.getTarget() == null || !boss.getTarget().isAlive()) return false;

        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return false;

        BossMalevolentShrineAbility domain =
                (BossMalevolentShrineAbility) data.getEquippedAbility(BossMalevolentShrineAbility.INSTANCE);
        if (domain == null || domain.isDomainActive() || domain.isCharging()) return false;

        boolean onCooldown = domain.getComponent(ModAbilityKeys.COOLDOWN)
                .map(c -> ((CooldownComponent) c).isOnCooldown())
                .orElse(false);
        return !onCooldown;
    }

    @Override
    public boolean canContinueToUse() {
        if (!domainUsed) return false;
        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return false;
        BossMalevolentShrineAbility domain =
                (BossMalevolentShrineAbility) data.getEquippedAbility(BossMalevolentShrineAbility.INSTANCE);
        if (domain == null) return false;
        return !domainStarted || domain.isDomainActive() || domain.isCharging();
    }

    @Override
    public void start() {
        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return;
        BossMalevolentShrineAbility domain =
                (BossMalevolentShrineAbility) data.getEquippedAbility(BossMalevolentShrineAbility.INSTANCE);
        if (domain == null) return;
        domain.use(boss);
        domainUsed    = true;
        domainStarted = false;
    }

    @Override
    public void tick() {
        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return;
        BossMalevolentShrineAbility domain =
                (BossMalevolentShrineAbility) data.getEquippedAbility(BossMalevolentShrineAbility.INSTANCE);
        if (domain == null) return;

        if (domain.isDomainActive()) domainStarted = true;

        // Force cleanup if boss dies mid-domain so shrine/sphere don't persist
        if (!boss.isAlive() && (domain.isDomainActive() || domain.isCharging())) {
            domain.stopChargingAndCooldown(boss);
        }
    }

    @Override
    public void stop() {
        if (domainStarted) {
            boss.domainFinished = true;
        }
    }
}