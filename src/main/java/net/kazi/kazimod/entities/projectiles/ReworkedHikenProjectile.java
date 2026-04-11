package net.kazi.kazimod.entities.projectiles;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.LiquidBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.SnowLayerBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ReworkedHikenProjectile extends AbilityProjectileEntity {
    private static final int MAX_LIFE = 48;

    private static final BlockProtectionRule GRIEF_RULE =
            new BlockProtectionRule.Builder(new BlockProtectionRule[]{
                    LiquidBlockProtectionRule.INSTANCE,
                    SnowLayerBlockProtectionRule.INSTANCE
            }).build();

    public ReworkedHikenProjectile(EntityType<ReworkedHikenProjectile> type, World world) {
        super(type, world);
    }

    public ReworkedHikenProjectile(World world, LivingEntity thrower) {
        super((EntityType) MeraProjectiles.REWORKED_HIKEN.get(), world, thrower, HikenRework.INSTANCE);
        this.setDamage(50.0F);
        this.setCanGetStuckInGround();
        this.setPassThroughEntities();
        this.setPassThroughBlocks();
        this.setMaxLife(MAX_LIFE);
        this.setArmorPiercing(0.75F);
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent = this::onTickEvent;
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = this.createExplosion(this.getThrower(), this.level,
                hit.getX(), hit.getY(), hit.getZ(), 4.0F);
        explosion.setStaticDamage(25.0F);
        explosion.setHeightDifference(30);
        explosion.disableExplosionKnockback();
        explosion.setFireAfterExplosion(true);
        explosion.setExplosionSound(false);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(2));
        explosion.doExplosion();
    }

    private void onTickEvent() {
        if (this.tickCount >= MAX_LIFE - 1) {
            this.onBlockImpactEvent(this.blockPosition());
            this.remove();
            return;
        }

        if (this.isEyeInFluid(FluidTags.WATER) && CommonConfig.INSTANCE.getDestroyWater()) {
            for (BlockPos blockPos : AbilityHelper.createFilledSphere(this.getCommandSenderWorld(),
                    (int) this.getX(), (int) this.getY(), (int) this.getZ(), 2, Blocks.AIR, GRIEF_RULE)) {
                WyHelper.spawnParticles(ParticleTypes.BUBBLE, (ServerWorld) this.getCommandSenderWorld(),
                        blockPos.getX() + WyHelper.randomDouble() / 2.0,
                        blockPos.getY() + 0.8,
                        blockPos.getZ() + WyHelper.randomDouble() / 2.0);
                this.getCommandSenderWorld().addParticle(ParticleTypes.SMOKE,
                        blockPos.getX(), blockPos.getY() + 1.1, blockPos.getZ(),
                        0.0, 0.0, 0.0);
            }
        }

        if (!this.level.isClientSide) {
            ServerWorld serverWorld = (ServerWorld) this.level;
            for (int i = 0; i < 20; i++) {
                SimpleParticleData data = new SimpleParticleData((ParticleType) ModParticleTypes.MERA.get());
                data.setLife(30);
                data.setSize(3.0F);
                WyHelper.spawnParticles(data, serverWorld,
                        this.getX() + WyHelper.randomDouble() * 2.0,
                        this.getY() + WyHelper.randomDouble() * 2.0,
                        this.getZ() + WyHelper.randomDouble() * 2.0);
            }

            for (int i = 0; i < 10; i++) {
                SimpleParticleData data = new SimpleParticleData((ParticleType) ModParticleTypes.MOKU.get());
                data.setLife(7);
                data.setSize(1.2F);
                WyHelper.spawnParticles(data, serverWorld,
                        this.getX() + WyHelper.randomDouble() / 2.0,
                        this.getY() + WyHelper.randomDouble() / 2.0,
                        this.getZ() + WyHelper.randomDouble() / 2.0);
            }
        }
    }
}
