package net.kazi.kazimod.entities.boss.gojo.goals;

import net.kazi.kazimod.abilities.boss.gojo.BossHollowPurpleAbility;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.api.entities.ai.AbilityWrapperGoal;

/**
 * Fires BossHollowPurpleAbility exactly once at fight start.
 *
 * Uses AbilityWrapperGoal so only CooldownComponent blocks it — no canUse
 * chain, no momentum check, no pool. canContinueToUseWrapper stays true while
 * charging so tickWrapper keeps the boss facing the target.
 */
public class BossHollowPurpleWrapperGoal extends AbilityWrapperGoal<MobEntity, BossHollowPurpleAbility> {

    public BossHollowPurpleWrapperGoal(MobEntity entity) {
        super(entity, BossHollowPurpleAbility.INSTANCE);
    }

    @Override
    public boolean canUseWrapper() {
        if (!(entity instanceof GojoBossEntity)) return false;
        if (((GojoBossEntity) entity).hollowPurpleFired) return false;
        return GoalUtil.hasAliveTarget(entity);
    }

    @Override
    public boolean canContinueToUseWrapper() {
        // Stay active for the full 140-tick charge so tickWrapper keeps facing target
        BossHollowPurpleAbility ability = getAbility();
        return ability != null && ability.isCharging();
    }

    @Override
    public void startWrapper() {
        if (entity instanceof GojoBossEntity) {
            ((GojoBossEntity) entity).hollowPurpleFired = true;
        }
        // Face target before charge starts so onChargeStart launches in right direction
        LivingEntity target = entity.getTarget();
        if (target != null) GoalUtil.lookAtEntity(entity, target);
    }

    @Override
    public void tickWrapper() {
        // Keep the boss facing the target throughout the 140-tick charge
        LivingEntity target = entity.getTarget();
        if (target != null && target.isAlive()) {
            GoalUtil.lookAtEntity(entity, target);
            // Also write xRot/yRot directly in case any internal code reads them
            double dx = target.getX() - entity.getX();
            double dy = (target.getY() + target.getBbHeight() * 0.5)
                    - (entity.getY() + entity.getEyeHeight());
            double dz = target.getZ() - entity.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            entity.yRot     = (float)(Math.atan2(-dx, dz) * (180.0 / Math.PI));
            entity.yHeadRot = entity.yRot;
            entity.xRot     = (float)(Math.atan2(-dy, horiz) * (180.0 / Math.PI));
        }
    }

    @Override
    public void stopWrapper() {
        // nothing to clean up
    }
}