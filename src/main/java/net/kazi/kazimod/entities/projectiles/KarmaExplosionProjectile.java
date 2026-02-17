//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.LiquidBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.SnowLayerBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KarmaExplosionProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {
    private static final BlockProtectionRule GRIEF_RULE;
    private float sizeScale = 1.0F; // Multiplier for size based on karma

    public KarmaExplosionProjectile(EntityType<Entity> type, World world) {
        super(type, world);
    }

    public KarmaExplosionProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType)KarmaProjectiles.KARMA_EXPLOSION.get(), world, player, ability.getCore());
        super.setBlocksAffectedLimit(42875);
        super.setArmorPiercing(0.75F);
        super.setUnavoidable();
        super.onBlockImpactEvent = this::onBlockImpactEvent;
        super.onTickEvent = this::onTickEvent;
    }

    /**
     * Set the scaled size based on karma (0.0 to 1.0 ratio)
     * @param scale - Normalized scale factor from karma calculation
     */
    public void setScaledSize(float scale) {
        this.sizeScale = Math.max(0.5F, Math.min(1.0F, scale)); // Clamp between 0.5 and 1.0
    }

    public void onBlockImpactEvent(BlockPos hit) {
        // Scale explosion size based on karma
        float explosionSize = 0.6F * this.getSize() * this.sizeScale;
        float explosionDamage = 2.0F * this.getSize() * this.sizeScale;

        ExplosionAbility explosion = super.createExplosion(super.getThrower(), super.level, (double)hit.getX(), (double)hit.getY(), (double)hit.getZ(), explosionSize);
        explosion.setStaticDamage(explosionDamage);
        explosion.setStaticBlockResistance(0.25F);
        explosion.setFireAfterExplosion(true);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect((int)(explosionSize)));
        explosion.doExplosion();
    }

    private void onTickEvent() {
        // Scale damage based on size and karma
        super.setDamage(2.0F * this.getSize() * this.sizeScale);

        if (super.isEyeInFluid(FluidTags.WATER) && CommonConfig.INSTANCE.getDestroyWater()) {
            int waterRadius = (int)(9 * this.sizeScale);
            for(BlockPos blockPos : AbilityHelper.createFilledSphere(super.getCommandSenderWorld(), (int)super.getX(), (int)super.getY(), (int)super.getZ(), waterRadius, Blocks.AIR, GRIEF_RULE)) {
                WyHelper.spawnParticles(ParticleTypes.BUBBLE, (ServerWorld)super.getCommandSenderWorld(), (double)blockPos.getX() + WyHelper.randomDouble() / 2.0, (double)blockPos.getY() + 0.8, (double)blockPos.getZ() + WyHelper.randomDouble() / 2.0);
                super.getCommandSenderWorld().addParticle(ParticleTypes.SMOKE, (double)blockPos.getX(), (double)blockPos.getY() + 1.1, (double)blockPos.getZ(), 0.0, 0.0, 0.0);
            }
        }

        if (!super.level.isClientSide) {
            // Scale particle count based on size
            int flameParticles = (int)(20 * this.sizeScale);
            int lavaParticles = (int)(15 * this.sizeScale);

            // Red flame particles for karma theme
            for(int i = 0; i < flameParticles; ++i) {
                double offsetX = WyHelper.randomDouble() * this.sizeScale;
                double offsetY = WyHelper.randomDouble() * this.sizeScale;
                double offsetZ = WyHelper.randomDouble() * this.sizeScale;
                WyHelper.spawnParticles(ParticleTypes.FLAME, (ServerWorld)super.level, super.getX() + offsetX, super.getY() + offsetY, super.getZ() + offsetZ);
            }

            // Additional lava particles for intensity
            for(int i = 0; i < lavaParticles; ++i) {
                double offsetX = WyHelper.randomDouble() * this.sizeScale;
                double offsetY = WyHelper.randomDouble() * this.sizeScale;
                double offsetZ = WyHelper.randomDouble() * this.sizeScale;
                super.level.addParticle(ParticleTypes.LAVA, super.getX() + offsetX, super.getY() + offsetY, super.getZ() + offsetZ, 0.0, 0.0, 0.0);
            }

            // Smoke particles
            for(int i = 0; i < 2; ++i) {
                double offsetX = WyHelper.randomDouble() * this.sizeScale;
                double offsetY = WyHelper.randomDouble() * this.sizeScale;
                double offsetZ = WyHelper.randomDouble() * this.sizeScale;
                SimpleParticleData data = new SimpleParticleData((ParticleType)ModParticleTypes.MOKU.get());
                data.setLife(4);
                data.setSize(1.2F * this.sizeScale);
                WyHelper.spawnParticles(data, (ServerWorld)super.level, super.getX() + offsetX, super.getY() + offsetY, super.getZ() + offsetZ);
            }
        }
    }

    public float getSize() {
        return (float)super.getBoundingBox().getSize() * 4.0F;
    }

    public void increaseSize() {
        // Increase size with scaling factor
        float baseIncrease = 0.0625F;
        float scaledIncrease = baseIncrease * this.sizeScale;
        float size = (float)super.getBoundingBox().getSize() + scaledIncrease;
        super.setEntityCollisionSize((double)size);
    }

    static {
        GRIEF_RULE = (new BlockProtectionRule.Builder(new BlockProtectionRule[]{LiquidBlockProtectionRule.INSTANCE, SnowLayerBlockProtectionRule.INSTANCE})).build();
    }
}