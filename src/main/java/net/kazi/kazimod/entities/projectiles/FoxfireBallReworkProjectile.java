//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.MrMagicalCart.cartaddon.entities.projectiles.inukitsune.InuKitsuneProjectiles;
import net.MrMagicalCart.cartaddon.init.CartParticleTypes;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.LiquidBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.SnowLayerBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.IFlexibleSizeProjectile;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.List;
import java.util.Optional;

public class FoxfireBallReworkProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {
    private static final BlockProtectionRule GRIEF_RULE;
    private Optional<LivingEntity> target;
    private boolean isLaunched = false;

    public FoxfireBallReworkProjectile(EntityType<Entity> type, World world) {
        super(type, world);
    }

    public FoxfireBallReworkProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) InuKitsuneProjectiles.FOXFIRE_BALL.get(), world, player, ability.getCore());
        super.setBlocksAffectedLimit(20000);
        super.setArmorPiercing(0.75F);
        super.setUnavoidable();
        super.setGravity(0.0F);
        super.onBlockImpactEvent = this::onBlockImpactEvent;
        super.onTickEvent = this::onTickEvent;
    }

    public void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(super.getThrower(), super.level, (double)hit.getX(), (double)hit.getY(), (double)hit.getZ(), 0.6F * this.getSize());
        explosion.setStaticDamage(2.0F * this.getSize());
        explosion.setStaticBlockResistance(0.25F);
        explosion.setFireAfterExplosion(true);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect((int)(0.6F * this.getSize())));
        explosion.doExplosion();
    }

    public void setTarget(Optional<LivingEntity> target) {
        this.target = target;
    }

    public void setLaunched(boolean launched) {
        this.isLaunched = launched;
    }

    public boolean isLaunched() {
        return this.isLaunched;
    }

    private void onTickEvent() {
        super.setDamage(2.0F * this.getSize());

        // Only track if the projectile has been launched
        if (this.isLaunched) {
            if (this.target != null && this.target.isPresent() && ((LivingEntity)this.target.get()).isAlive()) {
                Vector3d dist = super.position().subtract(((LivingEntity)this.target.get()).position()).add((double)0.0F, (double)-1.0F, (double)0.0F);
                double speedReduction = (double)12.0F;
                double speed = (double)0.5F;
                double xSpeed = Math.min(speed, -dist.x / speedReduction);
                double ySpeed = Math.min(speed, -dist.y / speedReduction);
                double zSpeed = Math.min(speed, -dist.z / speedReduction);
                AbilityHelper.setDeltaMovement(this, xSpeed, ySpeed, zSpeed);
            } else {
                List<LivingEntity> list = WyHelper.getNearbyLiving(super.position(), super.level, (double)16.0F, ModEntityPredicates.getEnemyFactions(super.getThrower()));
                list.remove(super.getThrower());
                list.sort(MobsHelper.ENTITY_THREAT);
                if (list.size() > 0) {
                    this.target = list.stream().findAny();
                }
            }
        }

        if (super.isEyeInFluid(FluidTags.WATER) && CommonConfig.INSTANCE.getDestroyWater()) {
            for(BlockPos blockPos : AbilityHelper.createFilledSphere(super.getCommandSenderWorld(), (int)super.getX(), (int)super.getY(), (int)super.getZ(), 9, Blocks.AIR, GRIEF_RULE)) {
                WyHelper.spawnParticles(ParticleTypes.BUBBLE, (ServerWorld)super.getCommandSenderWorld(), (double)blockPos.getX() + WyHelper.randomDouble() / (double)2.0F, (double)blockPos.getY() + 0.8, (double)blockPos.getZ() + WyHelper.randomDouble() / (double)2.0F);
                super.getCommandSenderWorld().addParticle(ParticleTypes.SMOKE, (double)blockPos.getX(), (double)blockPos.getY() + 1.1, (double)blockPos.getZ(), (double)0.0F, (double)0.0F, (double)0.0F);
            }
        }

        if (!super.level.isClientSide) {
            for(int i = 0; i < 20; ++i) {
                double offsetX = WyHelper.randomDouble();
                double offsetY = WyHelper.randomDouble();
                double offsetZ = WyHelper.randomDouble();
                SimpleParticleData data = new SimpleParticleData((ParticleType)CartParticleTypes.BLUE_FIRE.get());
                data.setLife(6);
                data.setSize(1.3F);
                WyHelper.spawnParticles(data, (ServerWorld)super.level, super.getX() + offsetX, super.getY() + offsetY, super.getZ() + offsetZ);
            }

            for(int i = 0; i < 2; ++i) {
                double offsetX = WyHelper.randomDouble();
                double offsetY = WyHelper.randomDouble();
                double offsetZ = WyHelper.randomDouble();
                SimpleParticleData data = new SimpleParticleData((ParticleType)CartParticleTypes.BLUE_FIRE.get());
                data.setLife(4);
                data.setSize(1.2F);
                WyHelper.spawnParticles(data, (ServerWorld)super.level, super.getX() + offsetX, super.getY() + offsetY, super.getZ() + offsetZ);
            }
        }

    }

    public float getSize() {
        return (float)super.getBoundingBox().getSize() * 2.0F;
    }

    public void increaseSize() {
        float size = (float)super.getBoundingBox().getSize() + 0.05F;
        super.setEntityCollisionSize((double)size);
    }

    static {
        GRIEF_RULE = (new BlockProtectionRule.Builder(new BlockProtectionRule[]{LiquidBlockProtectionRule.INSTANCE, SnowLayerBlockProtectionRule.INSTANCE})).build();
    }
}