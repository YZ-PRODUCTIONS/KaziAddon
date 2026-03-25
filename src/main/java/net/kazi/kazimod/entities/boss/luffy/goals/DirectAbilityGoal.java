package net.kazi.kazimod.entities.boss.luffy.goals;

import net.kazi.kazimod.entities.boss.BossAimHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.EnumSet;

/**
 * Bypasses AbilityWrapperGoal.func_75250_a() entirely.
 * That method calls GomuHelper.canUseGearCheck on gear abilities, which
 * always returns false for NPC entities. This base class calls ability.use()
 * directly once the ability is off cooldown.
 */
public abstract class DirectAbilityGoal<A extends Ability> extends Goal {

    protected final MobEntity entity;
    private final AbilityCore<A> abilityCore;

    public DirectAbilityGoal(MobEntity entity, AbilityCore<A> core) {
        this.entity = entity;
        this.abilityCore = core;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @SuppressWarnings("unchecked")
    protected A getAbility() {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return null;
        return (A) data.getEquippedAbility(abilityCore);
    }

    protected boolean isOnCooldown() {
        A ability = getAbility();
        if (ability == null) return true;
        return ability.getComponent(ModAbilityKeys.COOLDOWN)
                .map(c -> ((CooldownComponent) c).isOnCooldown())
                .orElse(false);
    }

    protected boolean isCharging() {
        A ability = getAbility();
        if (ability == null) return false;
        return ability.getComponent(ModAbilityKeys.CHARGE)
                .map(c -> ((ChargeComponent) c).isCharging())
                .orElse(false);
    }

    protected boolean isContinuous() {
        A ability = getAbility();
        if (ability == null) return false;
        return ability.getComponent(ModAbilityKeys.CONTINUOUS)
                .map(c -> ((ContinuousComponent) c).isContinuous())
                .orElse(false);
    }

    protected void useAbility() {
        A ability = getAbility();
        if (ability != null) ability.use(entity);
    }

    @Override
    public boolean canUse() {
        if (!entity.isAlive()) return false;
        if (!GoalUtil.hasAliveTarget(entity)) return false;
        if (isOnCooldown()) return false;
        return canUseExtra();
    }

    @Override public boolean canContinueToUse() { return canContinueExtra(); }

    @Override
    public void start() {
        aimAtTarget();
        useAbility();
        onStart();
    }

    @Override
    public void tick() {
        aimAtTarget();
        onTick();
    }

    @Override public void stop() { onStop(); }

    /**
     * Force yRot, xRot, yHeadRot and yBodyRot to aim directly at the target
     * using BossAimHelper lead-prediction. This ensures projectiles fire at the
     * player regardless of whether Luffy is airborne or grounded.
     */
    protected void aimAtTarget() {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return;

        // Use lead-prediction direction from BossAimHelper
        net.minecraft.util.math.vector.Vector3d dir =
                BossAimHelper.leadTarget(entity, target, 2.5);

        float yaw   = (float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.asin(dir.y)));

        entity.yRot      = yaw;
        entity.yHeadRot  = yaw;
        entity.yBodyRot  = yaw;
        entity.xRot      = pitch;
    }

    protected boolean canUseExtra()     { return true; }
    protected boolean canContinueExtra(){ return false; }
    protected void    onStart()         {}
    protected void    onTick()          {}
    protected void    onStop()          {}
}