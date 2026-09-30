package net.kazi.kazimod.entities.projectiles;

import java.util.function.Predicate;

import net.kazi.kazimod.abilities.YamiRework.DarkMatterRework;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DarkMatterReworkProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {
    private static final DataParameter<Float> DISPLAY_SIZE =
            EntityDataManager.defineId(DarkMatterReworkProjectile.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> LAUNCHED =
            EntityDataManager.defineId(DarkMatterReworkProjectile.class, DataSerializers.BOOLEAN);

    private final Interval particleInterval = new Interval(10);
    private final Interval damageInterval = new Interval(10);
    private int launchedTicks;

    public DarkMatterReworkProjectile(EntityType<?> type, World world) {
        super((EntityType) type, world);
        this.configureProjectile();
    }

    public DarkMatterReworkProjectile(World world, LivingEntity thrower) {
        super((EntityType) KaziEntities.DARK_MATTER_PROJECTILE.get(), world, thrower, DarkMatterRework.INSTANCE);
        this.configureProjectile();
    }

    private void configureProjectile() {
        this.setDamage(10.0F);
        this.setMaxLife(200);
        this.setNoGravity(true);
        this.setCollideWithEntities(false);
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent = this::onTickEvent;
    }

    public void grow(float chargeProgress) {
        float progress = Math.max(0.0F, Math.min(1.0F, chargeProgress));
        this.setSize(1.0F + 11.0F * progress);
        this.setEntityCollisionSize(0.25D + 0.75D * progress);
    }

    public void setLaunched(boolean launched) {
        this.entityData.set(LAUNCHED, launched);
        if (launched) {
            this.launchedTicks = 0;
        }
    }

    public boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    private void onBlockImpactEvent(BlockPos hit) {
        if (!this.isLaunched() || this.getThrower() == null) {
            return;
        }

        DarkMatterReworkProjectile gravityWell =
                new DarkMatterReworkProjectile(this.level, this.getThrower());
        gravityWell.setLife(100);
        gravityWell.setNoGravity(true);
        gravityWell.setLaunched(true);
        gravityWell.setSize(this.getSize());
        gravityWell.setEntityCollisionSize(1.0D);
        gravityWell.moveTo(this.getX(), this.getY() + 0.25D, this.getZ(), 0.0F, 0.0F);
        AbilityHelper.setDeltaMovement(gravityWell, 0.0D, 0.0D, 0.0D);
        gravityWell.setThrower(this.getThrower());
        this.level.addFreshEntity(gravityWell);
        WyHelper.spawnParticleEffect(
                (ParticleEffect) ModParticleEffects.DARK_MATTER.get(),
                this,
                gravityWell.getX(),
                gravityWell.getY(),
                gravityWell.getZ()
        );
    }

    private void onTickEvent() {
        if (this.particleInterval.canTick()) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) ModParticleEffects.DARK_MATTER.get(),
                    this,
                    this.getX(),
                    this.getY(),
                    this.getZ()
            );
        }

        if (this.level.isClientSide || !this.isLaunched() || this.getThrower() == null) {
            return;
        }

        ++this.launchedTicks;
        this.growAndDestroyBlocks();

        Vector3d center = this.position();
        double radius = 32.0D;
        double baseStrength = 0.1D;
        Predicate<Entity> enemies = ModEntityPredicates.getEnemyFactions(this.getThrower());

        for (LivingEntity target : WyHelper.getNearbyLiving(
                this.position(), (IWorld) this.level, radius, 4.5D, radius, enemies)) {
            if (!target.isAlive()) {
                continue;
            }

            Vector3d towardCenter = center.subtract(target.position());
            double distance = towardCenter.length();
            if (distance < 0.001D || distance > radius) {
                continue;
            }

            double falloff = 1.0D - distance / radius;
            Vector3d pull = towardCenter.scale(1.0D / distance).scale(baseStrength * falloff);
            target.setDeltaMovement(target.getDeltaMovement().add(pull));
            target.hurtMarked = true;
        }

        if (this.damageInterval.canTick()) {
            for (LivingEntity target : WyHelper.getNearbyLiving(
                    this.position(), (IWorld) this.level, 3.0D, 4.5D, 3.0D, enemies)) {
                if (!target.isAlive()) {
                    continue;
                }

                target.hurt((DamageSource) this.getDamageSource(), this.getDamage());
                AbilityHelper.disableAbilities(
                        target,
                        20,
                        ability -> ability.getCore().getCategory() == AbilityCategory.DEVIL_FRUITS
                );
            }
        }
    }

    private void growAndDestroyBlocks() {
        // Continue growing after release: full held size (12) -> launched maximum (20).
        float launchedSize = Math.min(20.0F, 12.0F + this.launchedTicks * 0.1F);
        this.setSize(launchedSize);

        // Expand the destruction radius one block at a time instead of clearing
        // the maximum area immediately. Repeating this while moving carves a tunnel.
        if (this.launchedTicks % 5 == 0) {
            int baseRadius = Math.min(5, 1 + (int) ((launchedSize - 12.0F) / 2.0F));
            int radius = (int) Math.ceil(baseRadius * 2.25D);
            AbilityHelper.createSphere(
                    this,
                    this.blockPosition(),
                    radius,
                    radius,
                    false,
                    Blocks.AIR,
                    2,
                    DefaultProtectionRules.AIR_CORE_FOLIAGE_ORE
            );
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DISPLAY_SIZE, 1.0F);
        this.entityData.define(LAUNCHED, false);
    }

    @Override
    public void setSize(float size) {
        this.entityData.set(DISPLAY_SIZE, size);
    }

    @Override
    public float getSize() {
        return this.entityData.get(DISPLAY_SIZE);
    }
}
