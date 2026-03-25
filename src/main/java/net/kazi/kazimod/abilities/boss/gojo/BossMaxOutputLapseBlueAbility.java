package net.kazi.kazimod.abilities.boss.gojo;

import net.kazi.kazimod.entities.boss.BossAimHelper;
import net.kazi.kazimod.entities.projectiles.LapseBlueProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;

/**
 * Boss Max Output Lapse Blue.
 *
 * The original MaxOutputLapseBlueAbility uses ProjectileComponent.shoot() which
 * fires along entity.xRot/entity.yRot — same root cause as HollowPurple's miss.
 *
 * This version fires LapseBlueProjectile directly using BossAimHelper.leadTarget()
 * so it always tracks the target correctly regardless of boss facing angle.
 *
 * Used by BossMaxOutputLapseBlueWrapperGoal as a wrapper goal.
 */
public class BossMaxOutputLapseBlueAbility extends Ability {

    private static final float   COOLDOWN         = 400.0F; // 20 seconds
    private static final double  PROJECTILE_SPEED = 2.0;

    public static final AbilityCore<BossMaxOutputLapseBlueAbility> INSTANCE;

    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public BossMaxOutputLapseBlueAbility(AbilityCore<BossMaxOutputLapseBlueAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{ this.animationComponent });
        // NO canUseCheck — works freely for mobs via AbilityWrapperGoal
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        LivingEntity target = entity instanceof MobEntity
                ? ((MobEntity) entity).getTarget() : null;
        if (target == null || !target.isAlive()) return;

        Vector3d firePos = entity.position().add(0, entity.getEyeHeight() * 0.9, 0);
        Vector3d dir     = BossAimHelper.leadTarget(entity, target, PROJECTILE_SPEED);

        LapseBlueProjectile proj = new LapseBlueProjectile(
                entity.level, entity, (Ability) ability);
        proj.setPos(firePos.x, firePos.y, firePos.z);
        proj.setDeltaMovement(dir.scale(PROJECTILE_SPEED));
        entity.level.addFreshEntity(proj);

        super.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Boss: Max Output Lapse Blue",
                AbilityCategory.DEVIL_FRUITS, BossMaxOutputLapseBlueAbility::new)
                .build();
    }
}