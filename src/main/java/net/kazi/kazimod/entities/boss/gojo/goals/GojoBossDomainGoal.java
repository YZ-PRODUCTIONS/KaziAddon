package net.kazi.kazimod.entities.boss.gojo;

import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.abilities.Koku.DomainExpansionInfiniteVoidAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

/**
 * Triggers Domain Expansion: Infinite Void once below 50% HP.
 * After the domain finishes, sets boss.domainFinished = true so that
 * GojoBossRedGoal switches Red to MAX_OUTPUT mode permanently.
 */
public class GojoBossDomainGoal extends Goal {

    private final GojoBossEntity boss;
    private boolean domainUsed = false;
    private boolean wasActive  = false;
    private static final float HP_THRESHOLD = 0.50f;

    public GojoBossDomainGoal(GojoBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (domainUsed) return false;
        if (boss.hollowNukeQueued) return false;

        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;

        float hpFraction = boss.getHealth() / boss.getMaxHealth();
        if (hpFraction > HP_THRESHOLD) return false;

        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return false;

        DomainExpansionInfiniteVoidAbility domain =
                data.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
        if (domain == null) return false;
        if (domain.isDomainActive() || domain.isCharging()) return false;

        boolean onCooldown = domain.getComponent(ModAbilityKeys.COOLDOWN)
                .map(c -> ((CooldownComponent) c).isOnCooldown())
                .orElse(false);
        return !onCooldown;
    }

    @Override
    // Keep running while domain is active so we can detect when it finishes.
    public boolean canContinueToUse() {
        if (!domainUsed) return false;
        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return false;
        DomainExpansionInfiniteVoidAbility domain =
                data.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
        if (domain == null) return false;
        // Continue until both domain ends AND the flag hasn't been set yet
        return (domain.isDomainActive() || domain.isCharging()) && !boss.domainFinished;
    }

    @Override
    public void start() {
        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return;

        DomainExpansionInfiniteVoidAbility domain =
                data.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
        if (domain == null) return;

        boolean chargeStarted = domain.getComponent(ModAbilityKeys.CHARGE)
                .filter(c -> c instanceof ChargeComponent)
                .map(c -> {
                    ((ChargeComponent) c).startCharging(boss, 60.0f);
                    return true;
                })
                .orElse(false);

        if (chargeStarted) {
            domainUsed = true;
            wasActive  = false;
        } else {
            KaziMod.LOGGER.warn("[GojoBossDomainGoal] No CHARGE component — attempting direct use().");
            try {
                domain.use(boss);
                domainUsed = true;
                wasActive  = false;
            } catch (Exception e) {
                KaziMod.LOGGER.error("[GojoBossDomainGoal] Direct use() failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public void tick() {
        // Detect domain becoming active then finishing — that's when we
        // set domainFinished so Red switches to MAX_OUTPUT.
        IAbilityData data = AbilityDataCapability.get(boss);
        if (data == null) return;

        DomainExpansionInfiniteVoidAbility domain =
                data.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
        if (domain == null) return;

        boolean active = domain.isDomainActive();
        if (active) wasActive = true;

        // Domain was active and is now done — switch Red to MAX_OUTPUT.
        if (wasActive && !active && !boss.domainFinished) {
            boss.domainFinished = true;
        }
    }
}