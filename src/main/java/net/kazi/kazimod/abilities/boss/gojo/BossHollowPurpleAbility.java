package net.kazi.kazimod.abilities.boss.gojo;

import net.kazi.kazimod.animations.gojo.GojoHollowPurpleAnimation;
import net.kazi.kazimod.entities.boss.BossAimHelper;
import net.kazi.kazimod.entities.projectiles.HollowPurpleProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.SoundCategory;
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
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class BossHollowPurpleAbility extends Ability {

    public static final float  CHARGE_TIME      = 140.0f;
    private static final float  COOLDOWN         = 1800.0f;
    private static final double PROJECTILE_SPEED = 4.5;

    public static final AbilityCore<BossHollowPurpleAbility> INSTANCE;

    private LivingEntity trackedTarget;
    private boolean fireAnimTriggered = false;
    private boolean chantTriggered    = false;

    private final ChargeComponent    chargeComponent;
    private final AnimationComponent animationComponent;
    private final Interval particleInterval = new Interval(2);

    public BossHollowPurpleAbility(final AbilityCore<BossHollowPurpleAbility> core) {
        super(core);
        this.trackedTarget     = null;
        this.chargeComponent   = new ChargeComponent(this)
                .addStartEvent(this::onChargeStart)
                .addTickEvent(this::onChargeTick)
                .addEndEvent(this::onChargeEnd);
        this.animationComponent = new AnimationComponent(this);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{ this.chargeComponent, this.animationComponent });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide || this.chargeComponent.isCharging()) return;
        this.trackedTarget = (entity instanceof MobEntity) ? ((MobEntity) entity).getTarget() : null;
        this.chargeComponent.startCharging(entity, CHARGE_TIME);
    }

    private void onChargeStart(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;
        // Launch upward like the original
        final Vector3d vel = entity.getDeltaMovement();
        entity.setDeltaMovement(vel.x, 5.5, vel.z);

        // Start animation matching HollowPurpleAbility
        this.animationComponent.start(entity, KaziAnimations.GOJO_HOLLOW_PURPLE);
        fireAnimTriggered = false;
        chantTriggered    = false;
        if (GojoHollowPurpleAnimation.INSTANCE != null) {
            GojoHollowPurpleAnimation.INSTANCE.reset();
        }
    }

    private void onChargeTick(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;

        // Hold boss at apex
        if (entity.getDeltaMovement().y <= 0.0 && !entity.isOnGround()) {
            entity.setDeltaMovement(0.0, 0.0, 0.0);
        }

        // Track target for aiming
        if (this.trackedTarget == null && entity instanceof MobEntity) {
            this.trackedTarget = ((MobEntity) entity).getTarget();
        }
        if (this.trackedTarget != null && this.trackedTarget.isAlive()) {
            final double dx    = this.trackedTarget.getX() - entity.getX();
            final double dy    = this.trackedTarget.getY() + this.trackedTarget.getBbHeight() * 0.5
                    - (entity.getY() + entity.getEyeHeight());
            final double dz    = this.trackedTarget.getZ() - entity.getZ();
            final double horiz = Math.sqrt(dx * dx + dz * dz);
            entity.yRot     = (float)(Math.atan2(-dx, dz) * 57.29577951308232);
            entity.yBodyRot = entity.yRot;
            entity.xRot     = (float)(Math.atan2(-dy, horiz) * 57.29577951308232);
        }

        // ── Particles matching HollowPurpleAbility.onChargeTick exactly ─────────
        if (particleInterval.canTick()) {
            float progress = this.chargeComponent.getChargePercentage();

            Vector3d look  = entity.getLookAngle().normalize();
            Vector3d right = new Vector3d(-look.z, 0, look.x);

            if (progress < 0.7F) {
                Vector3d redPos = entity.position().add(0, 1.5, 0).add(right.scale(-2.5));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_RED.get(),
                        entity, redPos.x, redPos.y, redPos.z);

                Vector3d bluePos = entity.position().add(0, 1.5, 0).add(right.scale(2.5));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_BLUE.get(),
                        entity, bluePos.x, bluePos.y, bluePos.z);
            } else {
                float  mergeProgress = (progress - 0.7F) / 0.3F;
                double separation    = 2.5 * (1.0F - mergeProgress);

                Vector3d redPos = entity.position().add(0, 1.5, 0).add(right.scale(-separation));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_RED.get(),
                        entity, redPos.x, redPos.y, redPos.z);

                Vector3d bluePos = entity.position().add(0, 1.5, 0).add(right.scale(separation));
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_BLUE.get(),
                        entity, bluePos.x, bluePos.y, bluePos.z);

                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_PURPLE.get(),
                        entity, entity.getX(), entity.getY() + 1.5, entity.getZ());

                if (!chantTriggered) {
                    chantTriggered = true;
                    entity.level.playSound(null, entity.blockPosition(),
                            KaziSounds.PURPLE_CHANT_SFX.get(),
                            SoundCategory.PLAYERS, 1.0F, 1.0F);
                }

                if (!fireAnimTriggered) {
                    fireAnimTriggered = true;
                    if (GojoHollowPurpleAnimation.INSTANCE != null) {
                        GojoHollowPurpleAnimation.INSTANCE.triggerFire();
                    }
                }
            }
        }
    }

    private void onChargeEnd(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;

        entity.setOnGround(false);

        final LivingEntity target = (this.trackedTarget != null && this.trackedTarget.isAlive())
                ? this.trackedTarget
                : ((entity instanceof MobEntity) ? ((MobEntity) entity).getTarget() : null);
        this.trackedTarget = null;

        if (target != null && target.isAlive()) {
            final Vector3d spawnPos = entity.position().add(0, entity.getEyeHeight(), 0);
            Vector3d dir = BossAimHelper.leadTarget(entity, target, PROJECTILE_SPEED);

            final HollowPurpleProjectile proj = new HollowPurpleProjectile(
                    entity.level, entity, (Ability) ability);
            proj.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            proj.setDeltaMovement(dir.scale(PROJECTILE_SPEED));
            entity.level.addFreshEntity(proj);
        }

        // Stop animation and reset flags
        this.animationComponent.stop(entity);
        fireAnimTriggered = false;
        chantTriggered    = false;
        if (GojoHollowPurpleAnimation.INSTANCE != null) {
            GojoHollowPurpleAnimation.INSTANCE.reset();
        }

        super.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    public boolean isCharging() {
        return this.chargeComponent.isCharging();
    }

    /**
     * Force-stops the charge without firing. Called from GojoBossEntity.remove()
     * so the ability doesn't remain active when the boss dies/despawns.
     */
    public void forceCancel(LivingEntity entity) {
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.forceStopCharging(entity);
        }
        this.animationComponent.stop(entity);
        fireAnimTriggered = false;
        chantTriggered    = false;
        this.trackedTarget = null;
        if (GojoHollowPurpleAnimation.INSTANCE != null) {
            GojoHollowPurpleAnimation.INSTANCE.reset();
        }
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Boss: Hollow Purple",
                AbilityCategory.DEVIL_FRUITS,
                BossHollowPurpleAbility::new
        ).setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(SourceType.INDIRECT, SourceType.PROJECTILE)
                // setPhantomKey prevents NPE in SDisableAbilityPacket.encode when the
                // boss dies while this ability is mid-charge (getRegistryName() would
                // otherwise return null, crashing writeResourceLocation)
                .setPhantomKey(new net.minecraft.util.ResourceLocation("kazimod", "boss_hollow_purple"))
                .build();
    }
}