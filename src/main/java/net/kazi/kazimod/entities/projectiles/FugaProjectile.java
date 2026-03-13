package net.kazi.kazimod.entities.projectiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // 5 seconds = 100 ticks
    private static final int    PARTICLE_DURATION = 100;
    private static final double AOE_RADIUS        = 20.0;
    private static final double MAX_RISE_HEIGHT   = 40.0;

    // Fire is placed all at once on impact; these control the periodic refresh
    // Run every 20 ticks (once per second) instead of every 10
    private static final int FIRE_REFRESH_INTERVAL = 20;
    // How many random fire spots to (re)place per refresh — much lower than before
    private static final int FIRE_REFRESH_COUNT    = 8;

    // Particle counts per tick — kept low; visuals live mostly on the CLIENT
    // Rising-front wave spawned every tick
    private static final int PARTICLES_WAVE    = 40;
    // Persistent ground layer spawned every tick
    private static final int PARTICLES_GROUND  = 20;
    // Column fill spawned every tick once the column is tall enough
    private static final int PARTICLES_FILL    = 30;
    // Vanilla FLAME particles spawned via spawnParticles every tick
    private static final int PARTICLES_FLAME   = 20;

    private static final DataParameter<Float>   SIZE;
    private static final DataParameter<Boolean> FINISHED;
    private static final DataParameter<Boolean> PLAYING_PARTICLES;
    private static final DataParameter<Integer> IMPACT_TICK;
    private static final BlockProtectionRule    GRIEF_RULE;

    public float   multiplier     = 0.0F;
    public boolean isChargeVisual = false;

    public FugaProjectile(EntityType type, World world) {
        super(type, world);
    }

    public FugaProjectile(World world, LivingEntity player) {
        super((EntityType) KaziEntities.FUGA.get(), world, player, FugaAbility.INSTANCE);
        this.setDamage(15.0F);
        this.setMaxLife(400);
        this.setArmorPiercing(1.0F);
        this.setCanGetStuckInGround();
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    public void tick() {
        super.tick();
        this.noCulling = true;

        // Only run the post-impact particle/fire logic on the SERVER.
        // Particle helpers already forward data to clients via packets — we must NOT
        // also run them on the client or every particle spawns twice.
        if (this.isPlayingParticles() && !this.level.isClientSide) {
            int    elapsed  = this.tickCount - this.getImpactTick();
            double progress = Math.min(1.0, elapsed / (double) PARTICLE_DURATION);
            double x        = this.getX();
            double y        = this.getY();
            double z        = this.getZ();
            ServerWorld sw  = (ServerWorld) this.level;

            // --- Fire refresh: runs every FIRE_REFRESH_INTERVAL ticks, places a small
            //     number of spots rather than rebuilding the whole disc each time. ---
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

            // --- Rising-front wave ---
            for (int i = 0; i < PARTICLES_WAVE; i++) {
                double angle  = this.random.nextDouble() * Math.PI * 2.0;
                double r      = Math.sqrt(this.random.nextDouble()) * AOE_RADIUS;
                double spawnY = y + currentTop + (this.random.nextDouble() * 6.0 - 3.0);
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HEAT_DASH.get(), this,
                        x + Math.cos(angle) * r, Math.max(y, spawnY), z + Math.sin(angle) * r);
            }

            // --- Persistent ground layer ---
            for (int i = 0; i < PARTICLES_GROUND; i++) {
                double angle  = this.random.nextDouble() * Math.PI * 2.0;
                double r      = Math.sqrt(this.random.nextDouble()) * AOE_RADIUS;
                double spawnY = y + this.random.nextDouble() * 3.0;
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HEAT_DASH.get(), this,
                        x + Math.cos(angle) * r, spawnY, z + Math.sin(angle) * r);
            }

            // --- Column fill (only once the column has meaningful height) ---
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

            // --- Vanilla FLAME particles ---
            WyHelper.spawnParticles(ParticleTypes.FLAME, sw,
                    x, y + currentTop * 0.5, z,
                    (float) AOE_RADIUS, (float)(currentTop * 0.5 + 1.0), (float) AOE_RADIUS,
                    PARTICLES_FLAME);

            if (elapsed >= PARTICLE_DURATION) {
                this.remove();
            }
        }
    }

    private void onBlockImpactEvent(BlockPos hit) {
        if (!this.isFinished() && !this.isChargeVisual) {
            if (KaziSounds.FUGA_HIT_SFX.get() != null) {
                this.level.playSound((PlayerEntity) null, this.blockPosition(),
                        (SoundEvent) KaziSounds.FUGA_HIT_SFX.get(), SoundCategory.PLAYERS, 10.0F, 0.25F);
            }

            AbilityHelper.createSphere(this.level, this.blockPosition(), 55, 5, false, Blocks.AIR, 2, GRIEF_RULE);

            List<LivingEntity> damageList    = WyHelper.getNearbyLiving(this.position(), this.level, 13.75F, ModEntityPredicates.getEnemyFactions(this.getThrower()));
            List<LivingEntity> knockbackList = WyHelper.getNearbyLiving(this.position(), this.level, 22.0F,  ModEntityPredicates.getEnemyFactions(this.getThrower()));

            ModDamageSource shockwaveSource = (new ModIndirectEntityDamageSource(super.getDamageSource().msgId, this, super.getThrower()))
                    .setSourceElement(SourceElement.SHOCKWAVE)
                    .setHakiNature(SourceHakiNature.IMBUING)
                    .setSourceTypes(new ArrayList(Arrays.asList(SourceType.INTERNAL)))
                    .setUnavoidable()
                    .setPiercing(1.0F);

            for (LivingEntity target : damageList) {
                target.hurtTime = target.invulnerableTime = 0;
                target.hurt(shockwaveSource, 85.0F);
            }

            for (LivingEntity target : knockbackList) {
                Vector3d speed = target.getLookAngle()
                        .multiply(-1.0, -1.0, -1.0)
                        .multiply(1.0, 0.0, 1.0);
                AbilityHelper.setDeltaMovement(target, speed.x, 0.25, speed.z);
            }

            if (!this.level.isClientSide) {
                // Place fire across the disc once on impact using a single structured pass.
                // We use a spiral of 20 angles × radius steps — same coverage as before but
                // done once rather than per-tick, and capped at 20 radial spokes.
                double ix = this.getX(), iy = this.getY(), iz = this.getZ();
                for (int spoke = 0; spoke < 20; spoke++) {
                    double angle = (spoke / 20.0) * Math.PI * 2.0;
                    // Step every 2 blocks along each spoke — reduces block accesses by 4×
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

                // Initial burst — greatly reduced from 800 to 80; still looks dramatic
                // at ground level on impact.
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
    }

    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SIZE,              0.0F);
        this.entityData.define(FINISHED,          false);
        this.entityData.define(PLAYING_PARTICLES, false);
        this.entityData.define(IMPACT_TICK,       0);
    }

    public void setSize(float size)  { this.entityData.set(SIZE, size); }
    public float getSize()           { return (Float) this.entityData.get(SIZE); }

    public boolean isFinished()      { return (Boolean) this.entityData.get(FINISHED); }
    public void setFinished()        { this.entityData.set(FINISHED, true); }

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