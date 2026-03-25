package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.Koku.DomainExpansionInfiniteVoidAbility;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import net.minecraft.entity.ai.attributes.Attributes;

import java.util.EnumSet;

public class GojoBossDomainGoal extends Goal {

    private final GojoBossEntity boss;
    private boolean domainUsed    = false;
    private boolean domainStarted = false;
    private boolean speedApplied  = false;
    private double  savedBaseSpeed = -1.0;

    public GojoBossDomainGoal(final GojoBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private DomainExpansionInfiniteVoidAbility getDomain() {
        IAbilityData data = AbilityDataCapability.get(this.boss);
        if (data == null) return null;
        return (DomainExpansionInfiniteVoidAbility) data.getEquippedAbility(
                (AbilityCore) DomainExpansionInfiniteVoidAbility.INSTANCE);
    }

    @Override
    public boolean canUse() {
        if (this.domainUsed || this.boss.hollowNukeQueued) return false;
        if (!this.boss.hollowPurpleFired) return false;

        final LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (this.boss.getHealth() / this.boss.getMaxHealth() > 0.5f) return false;

        final DomainExpansionInfiniteVoidAbility domain = getDomain();
        if (domain == null) return false;
        if (domain.isDomainActive() || domain.isCharging()) return false;
        if (domain.getComponent(ModAbilityKeys.COOLDOWN)
                .map(c -> c.isOnCooldown()).orElse(false)) return false;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.domainUsed) return false;
        final DomainExpansionInfiniteVoidAbility domain = getDomain();
        return domain != null && (!domainStarted || domain.isDomainActive() || domain.isCharging());
    }

    @Override
    public void start() {
        final LivingEntity target = this.boss.getTarget();

        // ── Teleport directly behind the player ───────────────────────────────
        if (target != null && target.isAlive()) {
            // Get the direction the player is looking and teleport 2 blocks behind them
            Vector3d lookDir   = target.getLookAngle();
            double   behindDist = 2.0;
            double   teleX = target.getX() - lookDir.x * behindDist;
            double   teleZ = target.getZ() - lookDir.z * behindDist;
            double   teleY = target.getY();
            this.boss.teleportTo(teleX, teleY, teleZ);

            // Face the player after teleport
            double dx = target.getX() - this.boss.getX();
            double dz = target.getZ() - this.boss.getZ();
            this.boss.yRot     = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            this.boss.yHeadRot = this.boss.yRot;
        }

        // ── Apply 3× speed boost ──────────────────────────────────────────────
        applySpeedBoost();

        // ── Start domain ──────────────────────────────────────────────────────
        final DomainExpansionInfiniteVoidAbility domain = getDomain();
        if (domain == null) return;

        try {
            domain.use(this.boss);
        } catch (final Exception ex) {
            domain.getComponent(ModAbilityKeys.CHARGE)
                    .ifPresent(c -> c.startCharging(this.boss, 60.0f));
        }

        this.domainUsed    = true;
        this.domainStarted = false;
    }

    @Override
    public void tick() {
        final DomainExpansionInfiniteVoidAbility domain = getDomain();
        if (domain == null) return;
        if (domain.isDomainActive()) domainStarted = true;
    }

    @Override
    public void stop() {
        removeSpeedBoost();
        if (domainStarted) {
            boss.domainFinished = true;
        }
    }

    // ── Speed boost helpers ───────────────────────────────────────────────────

    private void applySpeedBoost() {
        if (speedApplied) return;
        ModifiableAttributeInstance attr = this.boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        savedBaseSpeed = attr.getBaseValue();
        attr.setBaseValue(savedBaseSpeed * 3.0);
        speedApplied = true;
    }

    private void removeSpeedBoost() {
        if (!speedApplied) return;
        ModifiableAttributeInstance attr = this.boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && savedBaseSpeed > 0)
            attr.setBaseValue(savedBaseSpeed);
        speedApplied = false;
    }
}