package net.kazi.kazimod.entities.projectiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.kazi.kazimod.abilities.KamaRework.FugaAbility;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.damagesource.ModIndirectEntityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.AirBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.CoreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.FoliageBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.OreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class FugaProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {

    // Static tracker so FugaAbility can check if a projectile is active
    public static final Map<UUID, FugaProjectile> ACTIVE_PROJECTILES = new HashMap<>();

    private static final int    PARTICLE_DURATION    = 100;
    private static final double AOE_RADIUS           = 20.0;
    private static final double MAX_RISE_HEIGHT      = 40.0;
    private static final int    FIRE_REFRESH_INTERVAL = 20;
    private static final int    FIRE_REFRESH_COUNT    = 8;
    private static final int    PARTICLES_WAVE        = 40;
    private static final int    PARTICLES_GROUND      = 20;
    private static final int    PARTICLES_FILL        = 30;
    private static final int    PARTICLES_FLAME       = 20;

    private static final DataParameter<Float>   SIZE;
    private static final DataParameter<Boolean> FINISHED;
    private static final DataParameter<Boolean> PLAYING_PARTICLES;
    private static final DataParameter<Integer> IMPACT_TICK;
    private static final BlockProtectionRule    GRIEF_RULE;

    public float   multiplier     = 0.0F;
    public boolean isChargeVisual = false;
    private int    ticksInAir     = 0;

    public FugaProjectile(EntityType type, World world) {
        super(type, world);
    }

    public FugaProjectile(World world, LivingEntity player) {
        super((EntityType) KaziEntities.FUGA.get(), world, player, FugaAbility.INSTANCE);
        this.setDamage(30.0F);
        this.setMaxLife(400);
        this.setArmorPiercing(1.0F);
        this.setCanGetStuckInGround();
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    @Override
    public void tick() {
        super.tick();
        this.noCulling = true;

        if (!this.isFinished()) {
            Vector3d motion = this.getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-6D) {
                double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                float targetYaw = (float) (MathHelper.atan2(motion.x, motion.z) * (180.0D / Math.PI));
                float targetPitch = (float) (MathHelper.atan2(motion.y, horizontal) * (180.0D / Math.PI));
                if (this.tickCount <= 1) {
                    this.yRotO = targetYaw;
                    this.xRotO = targetPitch;
                } else {
                    this.yRotO = this.yRot;
                    this.xRotO = this.xRot;
                }
                this.yRot = targetYaw;
                this.xRot = targetPitch;
            }
        }

        if (!this.level.isClientSide) {
            if (getThrower() != null && this.isAlive()) {
                ACTIVE_PROJECTILES.put(getThrower().getUUID(), this);
            }
            if (!this.isFinished()) {
                ticksInAir++;
            }
        }

        if (this.isPlayingParticles() && !this.level.isClientSide) {
            int    elapsed  = this.tickCount - this.getImpactTick();
            double progress = Math.min(1.0, elapsed / (double) PARTICLE_DURATION);
            double x        = this.getX();
            double y        = this.getY();
            double z        = this.getZ();
            ServerWorld sw  = (ServerWorld) this.level;

            if (elapsed % FIRE_REFRESH_INTERVAL == 0) {
                for (int i = 0; i < FIRE_REFRESH_COUNT; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2.0;
                    double r     = Math.sqrt(this.random.nextDouble()) * AOE_RADIUS;
                    int fx = (int)(x + Math.cos(angle) * r);
                    int fz = (int)(z + Math.sin(angle) * r);
                    for (int fy = (int) y; fy >= (int) y - 5; fy--) {
                        BlockPos below = new BlockPos(fx, fy - 1, fz);
                        BlockPos above = new BlockPos(fx, fy,     fz);
                        if (!this.level.getBlockState(below).isAir()
                                && this.level.getBlockState(above).isAir()) {
                            this.level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }

            double currentTop = MAX_RISE_HEIGHT * progress;

            for (int i = 0; i < PARTICLES_WAVE; i++) {
                double angle  = this.random.nextDouble() * Math.PI * 2.0;
                double r      = Math.sqrt(this.random.nextDouble()) * AOE_RADIUS;
                double spawnY = y + currentTop + (this.random.nextDouble() * 6.0 - 3.0);
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HEAT_DASH.get(), this,
                        x + Math.cos(angle) * r, Math.max(y, spawnY), z + Math.sin(angle) * r);
            }

            for (int i = 0; i < PARTICLES_GROUND; i++) {
                double angle  = this.random.nextDouble() * Math.PI * 2.0;
                double r      = Math.sqrt(this.random.nextDouble()) * AOE_RADIUS;
                double spawnY = y + this.random.nextDouble() * 3.0;
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HEAT_DASH.get(), this,
                        x + Math.cos(angle) * r, spawnY, z + Math.sin(angle) * r);
            }

            if (currentTop > 4.0) {
                for (int i = 0; i < PARTICLES_FILL; i++) {
                    double angle  = this.random.nextDouble() * Math.PI * 2.0;
                    double r      = Math.sqrt(this.random.nextDouble()) * AOE_RADIUS;
                    double spawnY = y + this.random.nextDouble() * currentTop;
                    WyHelper.spawnParticleEffect(
                            (ParticleEffect) ModParticleEffects.HEAT_DASH.get(), this,
                            x + Math.cos(angle) * r, spawnY, z + Math.sin(angle) * r);
                }
            }

            WyHelper.spawnParticles(ParticleTypes.FLAME, sw,
                    x, y + currentTop * 0.5, z,
                    (float) AOE_RADIUS, (float)(currentTop * 0.5 + 1.0), (float) AOE_RADIUS,
                    PARTICLES_FLAME);

            if (elapsed >= PARTICLE_DURATION) {
                this.remove();
            }
        }
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && getThrower() != null) {
            ACTIVE_PROJECTILES.remove(getThrower().getUUID());
        }
        super.remove();
    }

    /** Manual detonation is disabled for Fuga. */
    public void detonate() {
        // Intentionally disabled: Fuga now only detonates on impact.
    }

    private void onBlockImpactEvent(BlockPos hit) {
        if (!this.isFinished() && !this.isChargeVisual) {
            doImpact(hit);
        }
    }

    private void doImpact(BlockPos hit) {
        if (KaziSounds.FUGA_HIT_SFX.get() != null) {
            this.level.playSound((PlayerEntity) null, this.blockPosition(),
                    (SoundEvent) KaziSounds.FUGA_HIT_SFX.get(), SoundCategory.PLAYERS, 10.0F, 0.25F);
        }

        AbilityHelper.createSphere(this.level, this.blockPosition(), 55, 5, false, Blocks.AIR, 2, GRIEF_RULE);

        List<LivingEntity> damageList    = WyHelper.getNearbyLiving(this.position(), this.level, 13.75F, ModEntityPredicates.getEnemyFactions(this.getThrower()));
        List<LivingEntity> knockbackList = WyHelper.getNearbyLiving(this.position(), this.level, 22.0F,  ModEntityPredicates.getEnemyFactions(this.getThrower()));

        ModDamageSource shockwaveSource = (new ModIndirectEntityDamageSource(
                super.getDamageSource().msgId, this, super.getThrower()))
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setHakiNature(SourceHakiNature.IMBUING)
                .setSourceTypes(new ArrayList<>(Arrays.asList(SourceType.INTERNAL)))
                .setUnavoidable()
                .setPiercing(1.0F);

        for (LivingEntity target : damageList) {
            target.hurtTime = target.invulnerableTime = 0;
            target.hurt(shockwaveSource, 95.0F);
        }

        for (LivingEntity target : knockbackList) {
            Vector3d speed = target.getLookAngle()
                    .multiply(-1.0, -1.0, -1.0)
                    .multiply(1.0, 0.0, 1.0);
            AbilityHelper.setDeltaMovement(target, speed.x, 0.25, speed.z);
        }

        if (!this.level.isClientSide) {
            double ix = this.getX(), iy = this.getY(), iz = this.getZ();
            for (int spoke = 0; spoke < 20; spoke++) {
                double angle = (spoke / 20.0) * Math.PI * 2.0;
                for (double r = 1.0; r <= AOE_RADIUS; r += 2.0) {
                    int fireX = (int)(ix + Math.cos(angle) * r);
                    int fireZ = (int)(iz + Math.sin(angle) * r);
                    for (int fireY = (int) iy; fireY >= (int) iy - 5; fireY--) {
                        BlockPos below = new BlockPos(fireX, fireY - 1, fireZ);
                        BlockPos above = new BlockPos(fireX, fireY,     fireZ);
                        if (!this.level.getBlockState(below).isAir()
                                && this.level.getBlockState(above).isAir()) {
                            this.level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }

            ServerWorld sw = (ServerWorld) this.level;
            for (int i = 0; i < 80; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double r     = Math.sqrt(this.random.nextDouble()) * AOE_RADIUS;
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HEAT_DASH.get(), this,
                        ix + Math.cos(angle) * r, iy + this.random.nextDouble() * 5.0, iz + Math.sin(angle) * r);
            }

            WyHelper.spawnParticles(ParticleTypes.FLAME, sw,
                    ix, iy + 2.0, iz,
                    (float) AOE_RADIUS, 3.0f, (float) AOE_RADIUS, 60);
        }

        this.setImpactTick(this.tickCount);
        this.setFinished();
        this.setPlayingParticles();
        AbilityHelper.setDeltaMovement(this, 0, 0, 0);
        this.teleportTo(this.getX(), this.getY(), this.getZ());
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SIZE,              0.0F);
        this.entityData.define(FINISHED,          false);
        this.entityData.define(PLAYING_PARTICLES, false);
        this.entityData.define(IMPACT_TICK,       0);
    }

    @Override public void setSize(float size) { this.entityData.set(SIZE, size); }
    @Override public float getSize()          { return (Float) this.entityData.get(SIZE); }

    public boolean isFinished()         { return (Boolean) this.entityData.get(FINISHED); }
    public void setFinished()           { this.entityData.set(FINISHED, true); }

    public boolean isPlayingParticles() { return (Boolean) this.entityData.get(PLAYING_PARTICLES); }
    public void setPlayingParticles()   { this.entityData.set(PLAYING_PARTICLES, true); }

    public int getImpactTick()          { return (Integer) this.entityData.get(IMPACT_TICK); }
    public void setImpactTick(int tick) { this.entityData.set(IMPACT_TICK, tick); }

    static {
        SIZE              = EntityDataManager.defineId(FugaProjectile.class, DataSerializers.FLOAT);
        FINISHED          = EntityDataManager.defineId(FugaProjectile.class, DataSerializers.BOOLEAN);
        PLAYING_PARTICLES = EntityDataManager.defineId(FugaProjectile.class, DataSerializers.BOOLEAN);
        IMPACT_TICK       = EntityDataManager.defineId(FugaProjectile.class, DataSerializers.INT);
        GRIEF_RULE = (new BlockProtectionRule.Builder(new BlockProtectionRule[]{
                AirBlockProtectionRule.INSTANCE,
                CoreBlockProtectionRule.INSTANCE,
                FoliageBlockProtectionRule.INSTANCE,
                OreBlockProtectionRule.INSTANCE
        })).build();
    }
}
