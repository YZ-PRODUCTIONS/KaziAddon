package net.kazi.kazimod.abilities.boss.sukuna;

import net.kazi.kazimod.entities.projectiles.FugaProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class BossFugaAbility extends Ability {

    private static final float CHARGE_TIME   = 80.0F;
    private static final float COOLDOWN      = 600.0F;
    private static final float LAUNCH_HEIGHT = 3.0F;

    public static final AbilityCore<BossFugaAbility> INSTANCE;

    private final ChargeComponent    chargeComponent    = new ChargeComponent(this)
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public BossFugaAbility(AbilityCore<BossFugaAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent, this.animationComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        AbilityHelper.setDeltaMovement(entity,
                entity.getDeltaMovement().x,
                LAUNCH_HEIGHT,
                entity.getDeltaMovement().z);

        this.animationComponent.start(entity, KaziAnimations.FUGA_SUKUNA, (int) CHARGE_TIME);

        if (!entity.level.isClientSide) {
            entity.level.playSound(null, entity.blockPosition(),
                    KaziSounds.FUGA_SFX.get(), SoundCategory.PLAYERS, 5.0F, 1.0F);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);

        if (this.chargeComponent.getChargeTime() > 10.0F) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) KaziParticleEffects.FUGA.get(),
                    entity, entity.getX(), entity.getY(), entity.getZ());
        }

        if (entity instanceof MobEntity) {
            LivingEntity target = ((MobEntity) entity).getTarget();
            if (target != null && target.isAlive()) {
                xyz.pixelatedw.mineminenomi.api.entities.GoalUtil.lookAtEntity(
                        (MobEntity) entity, target);
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);

        FugaProjectile projectile = new FugaProjectile(entity.level, entity);
        projectile.multiplier = 1.0F;
        projectile.setSize(1.2F);
        entity.level.addFreshEntity(projectile);

        if (entity instanceof MobEntity) {
            LivingEntity target = ((MobEntity) entity).getTarget();
            if (target != null) {
                // Find the ground position below the target —
                // scan downward from target's feet up to 64 blocks
                double groundY = target.getY();
                BlockPos checkPos = new BlockPos(target.getX(), target.getY() - 1, target.getZ());
                for (int i = 0; i < 64; i++) {
                    if (!entity.level.getBlockState(checkPos).isAir()) {
                        groundY = checkPos.getY() + 1.0;
                        break;
                    }
                    checkPos = checkPos.below();
                }

                // Aim from eye height to ground point below target
                Vector3d from = entity.position().add(0, entity.getEyeHeight(), 0);
                Vector3d to   = new Vector3d(target.getX(), groundY, target.getZ());
                Vector3d dir  = to.subtract(from).normalize();
                projectile.setDeltaMovement(dir.scale(4.0));
            } else {
                projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 4.0F, 0.0F);
            }
        } else {
            projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 4.0F, 0.0F);
        }

        super.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    public boolean isCharging() {
        return this.chargeComponent.isCharging();
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Boss: Fuga",
                AbilityCategory.DEVIL_FRUITS, BossFugaAbility::new)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.FIRE)
                .setSourceType(new SourceType[]{SourceType.PROJECTILE, SourceType.INTERNAL})
                .build();
    }
}