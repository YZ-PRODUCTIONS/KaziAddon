package net.kazi.kazimod.abilities.Kyoka;

import java.awt.Color;
import java.util.UUID;
import net.kazi.kazimod.entities.KurohitsugiEntity;
import net.kazi.kazimod.entities.KurohitsugiSpikeEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KurohitsugiAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "kurohitsugi",
            new Pair[]{ImmutablePair.of("After a heavy chant, the user manifests a giant black coffin around the target and crushes them with darkness and spikes.", null)}
    );

    private static final float RANGE = 40.0F;
    private static final float CHARGE_TIME = 50.0F;
    private static final float COOLDOWN = 800.0F;
    private static final float COFFIN_TIME = 32.0F;
    private static final float PULSE_DAMAGE = 10.0F;
    private static final float FINAL_DAMAGE = 61.0F;
    private static final double BOX_HALF_WIDTH = 7.5D;
    private static final double BOX_HEIGHT = 33.75D;
    private static final double SPIKE_LENGTH = 22.0D;
    private static final Color LIGHTNING_INNER = new Color(245, 220, 255, 210);
    private static final Color LIGHTNING_OUTER = new Color(153, 72, 255, 220);
    private static final Color LIGHTNING_OUTLINE = new Color(86, 24, 150, 170);
    public static final AbilityCore<KurohitsugiAbility> INSTANCE;

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final Interval visualInterval = new Interval(2);
    private final Interval damageInterval = new Interval(8);
    private UUID targetId;
    private Vector3d lockPos;
    private LightningDischargeEntity leftHandLightning;
    private LightningDischargeEntity rightHandLightning;
    private KurohitsugiEntity coffinEntity;
    private KurohitsugiEntity chargeOutlineEntity;

    public KurohitsugiAbility(AbilityCore<KurohitsugiAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging() || this.continuousComponent.isContinuous()) {
            return;
        }

        LivingEntity target = this.findTarget(entity);
        if (target == null) {
            return;
        }

        this.targetId = target.getUUID();
        this.lockPos = target.position();
        this.chargeComponent.startCharging(entity, CHARGE_TIME);
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundCategory.PLAYERS, 1.2F, 0.7F);
        if (!entity.level.isClientSide) {
            LivingEntity target = this.getTarget(entity);
            if (target != null) {
                this.lockPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                this.spawnChargeOutline(entity, this.lockPos);
            }
            this.spawnChargeLightning(entity);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        LivingEntity target = this.getTarget(entity);
        if (!entity.level.isClientSide) {
            if (target == null) {
                this.removeCoffinEntity();
                this.removeChargeOutline();
                this.stopChargeLightning();
                this.chargeComponent.stopCharging(entity);
                return;
            }

            this.lockPos = target.position();
            this.updateChargeOutline(entity, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), this.chargeComponent.getChargePercentage());
            this.updateChargeLightning(entity);
            if (this.visualInterval.canTick() && entity.level instanceof ServerWorld) {
                this.spawnChargeParticles((ServerWorld) entity.level, this.lockPos.add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.stopChargeLightning();
            this.removeChargeOutline();
            LivingEntity target = this.getTarget(entity);
            if (target != null) {
                this.lockPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                this.continuousComponent.startContinuity(entity, COFFIN_TIME);
            } else {
                this.removeCoffinEntity();
                this.clearState();
                this.cooldownComponent.startCooldown(entity, COOLDOWN);
            }
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && entity.level instanceof ServerWorld) {
            Vector3d center = this.getBoxCenter(entity);
            this.spawnCoffinEntity(entity, center);
            this.spawnSpikeBurst((ServerWorld) entity.level, center);
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1.0F, 0.55F);
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !(entity.level instanceof ServerWorld)) {
            return;
        }

        LivingEntity target = this.getTarget(entity);
        if (target == null) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        ServerWorld world = (ServerWorld) entity.level;
        Vector3d center = this.getBoxCenter(entity);
        this.updateCoffinEntity(center);
        this.pullTargetToCenter(target, center);
        this.enforceCoffinBarrier(world, target, center);
        this.destroyTouchingProjectiles(world, center);
        target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 6, 3, false, false));
        target.addEffect(new EffectInstance((Effect) ModEffects.ANTI_KNOCKBACK.get(), 6, 0, false, false));
        target.addEffect(new EffectInstance(Effects.BLINDNESS, 12, 0, false, false));
        target.addEffect(new EffectInstance(Effects.WEAKNESS, 12, 1, false, false));
        target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 12, 3, false, false));
        AbilityHelper.setDeltaMovement(target, 0.0D, Math.min(0.0D, target.getDeltaMovement().y), 0.0D);

        if (this.damageInterval.canTick()) {
            AbilityDamageSource source = this.getDamageSource(entity);
            this.dealDamageComponent.hurtTarget(entity, target, PULSE_DAMAGE, source);
            world.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundCategory.PLAYERS, 0.8F, 1.35F);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && entity.level instanceof ServerWorld) {
            LivingEntity target = this.getTarget(entity);
            if (target != null) {
                this.dealDamageComponent.hurtTarget(entity, target, FINAL_DAMAGE, this.getDamageSource(entity));
            }

            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, 0.8F);
        }

        this.removeCoffinEntity();
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
        this.clearState();
    }

    private LivingEntity findTarget(LivingEntity entity) {
        RayTraceResult trace = WyHelper.rayTraceBlocksAndEntities(entity, RANGE);
        if (trace instanceof EntityRayTraceResult) {
            Entity hit = ((EntityRayTraceResult) trace).getEntity();
            if (hit instanceof LivingEntity && hit != entity) {
                return (LivingEntity) hit;
            }
        }
        return null;
    }

    private LivingEntity getTarget(LivingEntity entity) {
        if (this.targetId == null || !(entity.level instanceof ServerWorld)) {
            return null;
        }

        Entity found = ((ServerWorld) entity.level).getEntity(this.targetId);
        if (!(found instanceof LivingEntity)) {
            return null;
        }

        LivingEntity target = (LivingEntity) found;
        return !target.isAlive() || target.distanceTo(entity) > RANGE + 8.0F ? null : target;
    }

    private Vector3d getBoxCenter(LivingEntity entity) {
        return this.lockPos != null ? this.lockPos : entity.position().add(0.0D, 1.0D, 0.0D);
    }

    private AbilityDamageSource getDamageSource(LivingEntity entity) {
        AbilityDamageSource source = (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
        source.setInternal();
        source.setUnavoidable();
        source.markIndirectDamage();
        source.bypassArmor();
        return source;
    }

    private void spawnChargeParticles(ServerWorld world, Vector3d center) {
        double minX = center.x - BOX_HALF_WIDTH;
        double maxX = center.x + BOX_HALF_WIDTH;
        double minY = center.y - BOX_HEIGHT * 0.5D;
        double maxY = center.y + BOX_HEIGHT * 0.5D;
        double minZ = center.z - BOX_HALF_WIDTH;
        double maxZ = center.z + BOX_HALF_WIDTH;
        double step = 1.8D;

        for (double x = minX; x <= maxX + 0.01D; x += step) {
            world.sendParticles(ParticleTypes.DRAGON_BREATH, x, minY, minZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            world.sendParticles(ParticleTypes.DRAGON_BREATH, x, maxY, minZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            world.sendParticles(ParticleTypes.DRAGON_BREATH, x, minY, maxZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            world.sendParticles(ParticleTypes.DRAGON_BREATH, x, maxY, maxZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }

        for (double y = minY; y <= maxY + 0.01D; y += step) {
            world.sendParticles(ParticleTypes.DRAGON_BREATH, minX, y, minZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            world.sendParticles(ParticleTypes.DRAGON_BREATH, maxX, y, minZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            world.sendParticles(ParticleTypes.DRAGON_BREATH, minX, y, maxZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            world.sendParticles(ParticleTypes.DRAGON_BREATH, maxX, y, maxZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private void spawnChargeLightning(LivingEntity entity) {
        this.stopChargeLightning();
        Vector3d[] hands = this.getHandPositions(entity);
        this.leftHandLightning = this.createHandLightning(entity, hands[0], true);
        this.rightHandLightning = this.createHandLightning(entity, hands[1], false);
    }

    private LightningDischargeEntity createHandLightning(LivingEntity entity, Vector3d pos, boolean inner) {
        LightningDischargeEntity discharge = new LightningDischargeEntity(entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot);
        discharge.setAliveTicks(-1);
        discharge.setUpdateRate(6);
        discharge.setLightningLength(1.9F);
        discharge.setColor(inner ? LIGHTNING_INNER : LIGHTNING_OUTER);
        discharge.setOutlineColor(inner ? LIGHTNING_OUTER : LIGHTNING_OUTLINE);
        discharge.setRenderTransparent();
        discharge.setDetails(18);
        discharge.setDensity(inner ? 24 : 34);
        discharge.setSize(inner ? 0.18F : 0.28F);
        discharge.setSkipSegments(1);
        entity.level.addFreshEntity(discharge);
        return discharge;
    }

    private void updateChargeLightning(LivingEntity entity) {
        Vector3d[] hands = this.getHandPositions(entity);
        this.updateChargeLightningEntity(this.leftHandLightning, hands[0]);
        this.updateChargeLightningEntity(this.rightHandLightning, hands[1]);
    }

    private void updateChargeLightningEntity(LightningDischargeEntity discharge, Vector3d pos) {
        if (discharge != null) {
            discharge.setPos(pos.x, pos.y, pos.z);
        }
    }

    private Vector3d[] getHandPositions(LivingEntity entity) {
        Vector3d look = entity.getLookAngle().normalize();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vector3d(0.0D, 0.0D, 1.0D);
        }
        Vector3d right = new Vector3d(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-4D) {
            right = new Vector3d(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vector3d base = entity.position().add(0.0D, entity.getBbHeight() * 0.7D, 0.0D).add(look.scale(0.45D));
        return new Vector3d[]{
                base.subtract(right.scale(0.5D)),
                base.add(right.scale(0.5D))
        };
    }

    private void stopChargeLightning() {
        this.stopChargeLightningEntity(this.leftHandLightning);
        this.stopChargeLightningEntity(this.rightHandLightning);
        this.leftHandLightning = null;
        this.rightHandLightning = null;
    }

    private void stopChargeLightningEntity(LightningDischargeEntity discharge) {
        if (discharge != null) {
            discharge.setAliveTicks(4);
        }
    }

    private void spawnCoffinParticles(ServerWorld world, Vector3d center, boolean heavy) {
        double minX = center.x - BOX_HALF_WIDTH;
        double maxX = center.x + BOX_HALF_WIDTH;
        double minY = center.y - BOX_HEIGHT * 0.5D;
        double maxY = center.y + BOX_HEIGHT * 0.5D;
        double minZ = center.z - BOX_HALF_WIDTH;
        double maxZ = center.z + BOX_HALF_WIDTH;
        double step = heavy ? 1.0D : 1.8D;

        for (double x = minX; x <= maxX + 0.01D; x += step) {
            for (double y = minY; y <= maxY + 0.01D; y += step) {
                this.spawnPurpleEdgeParticle(world, x, y, minZ, heavy);
                this.spawnPurpleEdgeParticle(world, x, y, maxZ, heavy);
            }
        }

        for (double z = minZ; z <= maxZ + 0.01D; z += step) {
            for (double y = minY; y <= maxY + 0.01D; y += step) {
                this.spawnPurpleEdgeParticle(world, minX, y, z, heavy);
                this.spawnPurpleEdgeParticle(world, maxX, y, z, heavy);
            }
        }

        for (double x = minX; x <= maxX + 0.01D; x += step) {
            for (double z = minZ; z <= maxZ + 0.01D; z += step) {
                this.spawnPurpleEdgeParticle(world, x, minY, z, heavy);
                this.spawnPurpleEdgeParticle(world, x, maxY, z, heavy);
            }
        }
    }

    private void spawnSpikeBurst(ServerWorld world, Vector3d center) {
        double faceOffset = 0.0D;
        double upperY = center.y + 6.0D;
        double lowerY = center.y - 6.0D;
        double topY = center.y + BOX_HEIGHT * 0.5D - 3.0D;
        this.spawnSpikeEntity(world, center.x - BOX_HALF_WIDTH - faceOffset, center.y, center.z, -90.0F, 0.0F, 1.5F);
        this.spawnSpikeEntity(world, center.x + BOX_HALF_WIDTH + faceOffset, center.y, center.z, 90.0F, 0.0F, 1.5F);
        this.spawnSpikeEntity(world, center.x, center.y, center.z - BOX_HALF_WIDTH - faceOffset, 0.0F, 0.0F, 1.5F);
        this.spawnSpikeEntity(world, center.x, center.y, center.z + BOX_HALF_WIDTH + faceOffset, 180.0F, 0.0F, 1.5F);
        this.spawnSpikeEntity(world, center.x - BOX_HALF_WIDTH - faceOffset, upperY, center.z, -90.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x + BOX_HALF_WIDTH + faceOffset, upperY, center.z, 90.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x, upperY, center.z - BOX_HALF_WIDTH - faceOffset, 0.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x, upperY, center.z + BOX_HALF_WIDTH + faceOffset, 180.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x - BOX_HALF_WIDTH - faceOffset, lowerY, center.z, -90.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x + BOX_HALF_WIDTH + faceOffset, lowerY, center.z, 90.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x, lowerY, center.z - BOX_HALF_WIDTH - faceOffset, 0.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x, lowerY, center.z + BOX_HALF_WIDTH + faceOffset, 180.0F, 0.0F, 1.22F);
        this.spawnSpikeEntity(world, center.x - BOX_HALF_WIDTH - faceOffset, topY, center.z, -90.0F, 0.0F, 1.1F);
        this.spawnSpikeEntity(world, center.x + BOX_HALF_WIDTH + faceOffset, topY, center.z, 90.0F, 0.0F, 1.1F);
        this.spawnSpikeEntity(world, center.x, topY, center.z - BOX_HALF_WIDTH - faceOffset, 0.0F, 0.0F, 1.1F);
        this.spawnSpikeEntity(world, center.x, topY, center.z + BOX_HALF_WIDTH + faceOffset, 180.0F, 0.0F, 1.1F);
    }

    private void pullTargetToCenter(LivingEntity target, Vector3d center) {
        Vector3d targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vector3d delta = center.subtract(targetCenter);
        AbilityHelper.setDeltaMovement(target, delta.x * 0.35D, Math.max(-0.08D, delta.y * 0.2D), delta.z * 0.35D);
        target.fallDistance = 0.0F;
    }

    private void enforceCoffinBarrier(ServerWorld world, LivingEntity trappedTarget, Vector3d center) {
        double minX = center.x - BOX_HALF_WIDTH;
        double maxX = center.x + BOX_HALF_WIDTH;
        double minY = center.y - BOX_HEIGHT * 0.5D;
        double maxY = center.y + BOX_HEIGHT * 0.5D;
        double minZ = center.z - BOX_HALF_WIDTH;
        double maxZ = center.z + BOX_HALF_WIDTH;
        AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ).inflate(1.25D);

        for (Entity entity : world.getEntities((Entity) null, box, e -> e != null && e.isAlive())) {
            if (!(entity instanceof LivingEntity) || entity == trappedTarget) {
                continue;
            }

            Vector3d pos = entity.position();
            boolean inside = pos.x > minX && pos.x < maxX && pos.y > minY && pos.y < maxY && pos.z > minZ && pos.z < maxZ;
            if (!inside) {
                continue;
            }

            double distX = Math.min(pos.x - minX, maxX - pos.x);
            double distY = Math.min(pos.y - minY, maxY - pos.y);
            double distZ = Math.min(pos.z - minZ, maxZ - pos.z);
            double outX = pos.x;
            double outY = pos.y;
            double outZ = pos.z;
            double padding = 1.25D;

            if (distX <= distY && distX <= distZ) {
                outX = pos.x < center.x ? minX - padding : maxX + padding;
            } else if (distZ <= distX && distZ <= distY) {
                outZ = pos.z < center.z ? minZ - padding : maxZ + padding;
            } else {
                outY = pos.y < center.y ? minY - padding : maxY + padding;
            }

            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                player.connection.teleport(outX, outY, outZ, player.yRot, player.xRot);
            } else {
                entity.teleportTo(outX, outY, outZ);
            }

            entity.setDeltaMovement(Vector3d.ZERO);
            entity.hurtMarked = true;
        }
    }

    private void destroyTouchingProjectiles(ServerWorld world, Vector3d center) {
        double minX = center.x - BOX_HALF_WIDTH;
        double maxX = center.x + BOX_HALF_WIDTH;
        double minY = center.y - BOX_HEIGHT * 0.5D;
        double maxY = center.y + BOX_HEIGHT * 0.5D;
        double minZ = center.z - BOX_HALF_WIDTH;
        double maxZ = center.z + BOX_HALF_WIDTH;
        double wallThickness = 1.25D;
        AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ).inflate(2.0D);

        for (Entity entity : world.getEntities((Entity) null, box, e -> e instanceof ProjectileEntity && e.isAlive())) {
            Vector3d pos = entity.position();
            boolean nearXWall = pos.x >= minX - wallThickness && pos.x <= maxX + wallThickness
                    && (Math.abs(pos.x - minX) <= wallThickness || Math.abs(pos.x - maxX) <= wallThickness)
                    && pos.y >= minY - wallThickness && pos.y <= maxY + wallThickness
                    && pos.z >= minZ - wallThickness && pos.z <= maxZ + wallThickness;
            boolean nearYWall = pos.y >= minY - wallThickness && pos.y <= maxY + wallThickness
                    && (Math.abs(pos.y - minY) <= wallThickness || Math.abs(pos.y - maxY) <= wallThickness)
                    && pos.x >= minX - wallThickness && pos.x <= maxX + wallThickness
                    && pos.z >= minZ - wallThickness && pos.z <= maxZ + wallThickness;
            boolean nearZWall = pos.z >= minZ - wallThickness && pos.z <= maxZ + wallThickness
                    && (Math.abs(pos.z - minZ) <= wallThickness || Math.abs(pos.z - maxZ) <= wallThickness)
                    && pos.x >= minX - wallThickness && pos.x <= maxX + wallThickness
                    && pos.y >= minY - wallThickness && pos.y <= maxY + wallThickness;

            if (nearXWall || nearYWall || nearZWall) {
                entity.remove();
            }
        }
    }

    private void spawnCoffinEntity(LivingEntity entity, Vector3d center) {
        this.removeCoffinEntity();
        this.coffinEntity = KurohitsugiEntity.create(entity.level, center.x, center.y, center.z, (int) COFFIN_TIME + 4);
        entity.level.addFreshEntity(this.coffinEntity);
    }

    private void spawnChargeOutline(LivingEntity entity, Vector3d center) {
        this.removeChargeOutline();
        this.chargeOutlineEntity = KurohitsugiEntity.createOutline(entity.level, center.x, center.y, center.z, (int) CHARGE_TIME + 8);
        entity.level.addFreshEntity(this.chargeOutlineEntity);
    }

    private void updateCoffinEntity(Vector3d center) {
        if (this.coffinEntity != null) {
            this.coffinEntity.setPos(center.x, center.y, center.z);
        }
    }

    private void updateChargeOutline(LivingEntity entity, Vector3d center, float progress) {
        if (this.chargeOutlineEntity == null) {
            this.spawnChargeOutline(entity, center);
        }

        if (this.chargeOutlineEntity != null) {
            this.chargeOutlineEntity.setPos(center.x, center.y, center.z);
            this.chargeOutlineEntity.setProgress(progress);
        }
    }

    private void removeCoffinEntity() {
        if (this.coffinEntity != null) {
            this.coffinEntity.remove();
            this.coffinEntity = null;
        }
    }

    private void removeChargeOutline() {
        if (this.chargeOutlineEntity != null) {
            this.chargeOutlineEntity.remove();
            this.chargeOutlineEntity = null;
        }
    }

    private void spawnSpikeEntity(ServerWorld world, double x, double y, double z, float yaw, float pitch, float scale) {
        KurohitsugiSpikeEntity spike = KurohitsugiSpikeEntity.create(world, x, y, z, yaw, pitch, scale, 28);
        world.addFreshEntity(spike);
    }

    private void spawnPurpleEdgeParticle(ServerWorld world, double x, double y, double z, boolean heavy) {
        world.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, heavy ? 2 : 1, 0.02D, 0.02D, 0.02D, 0.0D);
    }

    private void clearState() {
        this.targetId = null;
        this.lockPos = null;
        this.removeCoffinEntity();
        this.removeChargeOutline();
        this.stopChargeLightning();
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Hado 90: Kurohitsugi", AbilityCategory.DEVIL_FRUITS, KurohitsugiAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(PULSE_DAMAGE, FINAL_DAMAGE),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(COFFIN_TIME),
                        CooldownComponent.getTooltip(COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.LINE)
                )
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INTERNAL, SourceType.INDIRECT})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setUnlockCheck(KurohitsugiAbility::canUnlock)
                .build();
    }
}
