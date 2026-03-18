package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.awt.*;

public class MaxOutputRedProjectile extends AbilityProjectileEntity {

    private LivingEntity hookedEntity = null;
    private LivingEntity caster = null;
    private boolean returning = false;
    private int pullTicks = 0;
    private static final int MAX_PULL_TICKS = 100;
    private static final double BEHIND_DISTANCE = 1.5;
    private static final double PROJECTILE_SPEED = 3.5;

    private static final Color RED_LIGHTNING_INNER = new Color(255, 255, 255);
    private static final Color RED_LIGHTNING_OUTER = new Color(180, 0, 0, 200);

    public MaxOutputRedProjectile(EntityType type, World world) {
        super(type, world);
    }

    public MaxOutputRedProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) GojoProjectiles.MAX_OUTPUT_RED.get(), world, player, ability);
        this.caster = player;
        super.setPassThroughEntities();
        this.setDamage(70.0F);
        this.setMaxLife(999);
        this.setHurtTime(5);
        this.setUnavoidable();
        this.setPassThroughBlocks();
        this.setBlocksAffectedLimit(2048);
        this.setEntityCollisionSize((double) 4.0F, (double) 4.0F, (double) 4.0F);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent = this::onTickEvent;
    }

    private Vector3d getBehindTarget() {
        // Predict where entity will be next tick based on current movement
        Vector3d predictedPos = hookedEntity.position().add(hookedEntity.getDeltaMovement());

        // Place projectile behind predicted position away from caster
        Vector3d casterToHooked = predictedPos
                .subtract(caster.position())
                .normalize()
                .scale(BEHIND_DISTANCE);
        return new Vector3d(
                predictedPos.x + casterToHooked.x,
                predictedPos.y + 1.0,
                predictedPos.z + casterToHooked.z
        );
    }

    private void spawnRedLightning(LivingEntity target) {
        LightningDischargeEntity inner = new LightningDischargeEntity(
                target, target.getX(), target.getY() + 1.0, target.getZ(),
                target.yRot, target.xRot);
        inner.setAliveTicks(15);
        inner.setLightningLength(4.0F);
        inner.setColor(RED_LIGHTNING_INNER);
        inner.setOutlineColor(RED_LIGHTNING_OUTER);
        inner.setRenderTransparent();
        inner.setDetails(20);
        inner.setDensity(40);
        inner.setSize(0.3F);
        inner.setSkipSegments(1);
        target.level.addFreshEntity(inner);

        LightningDischargeEntity outer = new LightningDischargeEntity(
                target, target.getX(), target.getY() + 1.0, target.getZ(),
                target.yRot, target.xRot);
        outer.setAliveTicks(15);
        outer.setLightningLength(4.0F);
        outer.setColor(RED_LIGHTNING_OUTER);
        outer.setOutlineColor(new Color(100, 0, 0, 150));
        outer.setRenderTransparent();
        outer.setDetails(20);
        outer.setDensity(40);
        outer.setSize(0.5F);
        outer.setSkipSegments(1);
        target.level.addFreshEntity(outer);
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        if (hookedEntity == null) {
            hookedEntity = hitEntity;
            Vector3d direction = this.getDeltaMovement().normalize();
            AbilityHelper.setDeltaMovement(hitEntity, direction.x * 1.40, 1.25, direction.z * 1.40);
        }
    }

    private void onBlockImpactEvent(BlockPos hit) {
        if (hookedEntity == null) {
            ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level,
                    (double) hit.getX(), (double) hit.getY(), (double) hit.getZ(), 4.0F);
            explosion.setStaticDamage(0.0F);
            explosion.setSmokeParticles(new CommonExplosionParticleEffect(2));
            explosion.doExplosion();
        }
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {

            // ── Return / pull phase ──────────────────────────────────────────
            if (returning) {
                if (hookedEntity == null || !hookedEntity.isAlive() || caster == null) {
                    this.remove();
                    return;
                }

                pullTicks++;
                if (pullTicks > MAX_PULL_TICKS) {
                    this.remove();
                    return;
                }

                Vector3d target = getBehindTarget();
                Vector3d toTarget = target.subtract(this.position());
                double distToTarget = toTarget.length();

                if (distToTarget > BEHIND_DISTANCE) {
                    Vector3d velocity = toTarget.normalize().scale(PROJECTILE_SPEED);
                    this.setDeltaMovement(velocity.x, velocity.y, velocity.z);
                } else {
                    this.setDeltaMovement(Vector3d.ZERO);
                    this.setPos(target.x, target.y, target.z);
                }

                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GOJO_RED.get(), this,
                        this.getX(), this.getY(), this.getZ()
                );

                // Drag hooked entity toward caster
                Vector3d toCaster = caster.position()
                        .add(0, 1, 0)
                        .subtract(hookedEntity.position())
                        .normalize();
                AbilityHelper.setDeltaMovement(hookedEntity,
                        toCaster.x * 1.8,
                        toCaster.y * 1.8,
                        toCaster.z * 1.8);

// Keep entity locked while being pulled
                hookedEntity.addEffect(new EffectInstance(
                        (Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 5, false, false));
                hookedEntity.addEffect(new EffectInstance(
                        (Effect) ModEffects.NO_HANDS.get(), 10, 0, false, false));

                // Once hooked entity reaches caster — damage, red lightning, blackflash sound, punch away
                // Once hooked entity reaches caster — damage, red lightning, blackflash sound, punch away
                if (hookedEntity.distanceTo(caster) < 2.5) {
                    hookedEntity.hurt(DamageSource.mobAttack(caster), 40.0F);

                    spawnRedLightning(hookedEntity);

                    // Swing animation on final punch
                    ((ServerWorld) this.level).getChunkSource()
                            .broadcastAndSend(caster, new SAnimateHandPacket(caster, 0));

                    this.level.playSound(
                            (PlayerEntity) null,
                            caster.blockPosition(),
                            KaziSounds.BLACK_FLASH_HIT_SFX.get(),
                            SoundCategory.PLAYERS,
                            1.0F,
                            1.0F
                    );

                    Vector3d lookDir = caster.getLookAngle();
                    AbilityHelper.setDeltaMovement(hookedEntity,
                            lookDir.x * 3.5,
                            1.8,
                            lookDir.z * 3.5);
                    this.remove();
                }

                return;
            }

            // ── Normal flight phase ──────────────────────────────────────────
            WyHelper.spawnParticleEffect((ParticleEffect) KaziParticleEffects.GOJO_RED.get(), this,
                    this.getX(), this.getY(), this.getZ());

            if (hookedEntity == null && this.tickCount >= 80) {
                this.remove();
                return;
            }

            if (hookedEntity == null || caster == null) return;

            if (!hookedEntity.isAlive()) {
                this.remove();
                return;
            }

            // Start return phase once 20+ blocks from caster
            if (!returning && this.distanceTo(caster) >= 20.0) {
                returning = true;
            }
        }
    }
}