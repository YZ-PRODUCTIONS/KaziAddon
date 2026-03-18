package net.kazi.kazimod.entities.projectiles;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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

public class CruelSunProjectile extends AbilityProjectileEntity implements IFlexibleSizeProjectile {

    private static final BlockProtectionRule GRIEF_RULE =
            (new BlockProtectionRule.Builder(new BlockProtectionRule[]{
                    LiquidBlockProtectionRule.INSTANCE,
                    SnowLayerBlockProtectionRule.INSTANCE
            })).build();

    public CruelSunProjectile(EntityType<Entity> type, World world) {
        super(type, world);
    }

    public CruelSunProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) KachiProjectiles.CRUEL_SUN.get(), world, player, ability.getCore());
        super.setEntityCollisionSize(2.0); // start small, grows during charge
        super.setArmorPiercing(0.75F);
        super.setUnavoidable();
        super.onBlockImpactEvent = this::onBlockImpactEvent;
        super.onTickEvent        = this::onTickEvent;
    }

    // ── Grow during charge (called from ability's charge tick) ────────────────

    public void grow(float chargeProgress) {
        // 2.0 at charge start → 3.5 at full charge
        double size = 2.0 + (1.5 * chargeProgress);
        super.setEntityCollisionSize(size);
    }

    // ── Block impact ──────────────────────────────────────────────────────────

    public void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(
                super.getThrower(), super.level,
                hit.getX(), hit.getY(), hit.getZ(),
                0.5F * this.getSize());
        explosion.setStaticDamage(2.5F * this.getSize());
        explosion.setStaticBlockResistance(0.25F);
        explosion.setFireAfterExplosion(true);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect((int)(0.5F * this.getSize())));
        explosion.doExplosion();
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    private void onTickEvent() {
        super.setDamage(2.5F * this.getSize());

        // Evaporate water
        if (super.isEyeInFluid(FluidTags.WATER) && CommonConfig.INSTANCE.getDestroyWater()) {
            for (BlockPos pos : AbilityHelper.createFilledSphere(
                    super.getCommandSenderWorld(),
                    (int) super.getX(), (int) super.getY(), (int) super.getZ(),
                    7, Blocks.AIR, GRIEF_RULE)) {
                WyHelper.spawnParticles(ParticleTypes.BUBBLE,
                        (ServerWorld) super.getCommandSenderWorld(),
                        pos.getX() + WyHelper.randomDouble() / 2.0,
                        pos.getY() + 0.8,
                        pos.getZ() + WyHelper.randomDouble() / 2.0);
            }
        }

        if (!super.level.isClientSide) {
            ServerWorld sw = (ServerWorld) super.level;

            // Core fire particles
            for (int i = 0; i < 18; i++) {
                SimpleParticleData fire = new SimpleParticleData(
                        (net.minecraft.particles.ParticleType) ModParticleTypes.MERA.get());
                fire.setLife(8);
                fire.setSize(1.5F);
                WyHelper.spawnParticles(fire, sw,
                        super.getX() + WyHelper.randomDouble(),
                        super.getY() + WyHelper.randomDouble(),
                        super.getZ() + WyHelper.randomDouble());
            }

            // Smoke wisps
            for (int i = 0; i < 2; i++) {
                SimpleParticleData smoke = new SimpleParticleData(
                        (net.minecraft.particles.ParticleType) ModParticleTypes.MOKU.get());
                smoke.setLife(5);
                smoke.setSize(1.1F);
                WyHelper.spawnParticles(smoke, sw,
                        super.getX() + WyHelper.randomDouble(),
                        super.getY() + WyHelper.randomDouble(),
                        super.getZ() + WyHelper.randomDouble());
            }

            // Outer corona — flame particles orbiting the equator
            for (int i = 0; i < 8; i++) {
                double angle  = (i / 8.0) * Math.PI * 2;
                double radius = this.getSize() * 0.6;
                WyHelper.spawnParticles(ParticleTypes.FLAME, sw,
                        super.getX() + Math.cos(angle) * radius,
                        super.getY() + WyHelper.randomDouble() * 0.5,
                        super.getZ() + Math.sin(angle) * radius);
            }

            // Rotating golden sparkle ring
            for (int i = 0; i < 6; i++) {
                double angle  = (i / 6.0) * Math.PI * 2 + (super.tickCount * 0.1);
                double radius = this.getSize() * 0.8;
                WyHelper.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, sw,
                        super.getX() + Math.cos(angle) * radius,
                        super.getY() + WyHelper.randomDouble() * 0.3,
                        super.getZ() + Math.sin(angle) * radius);
            }
        }
    }

    // ── IFlexibleSizeProjectile ───────────────────────────────────────────────

    @Override
    public float getSize() {
        return (float) super.getBoundingBox().getSize() * 4.0F;
    }

    public void increaseSize() {
        super.setEntityCollisionSize(super.getBoundingBox().getSize() + 0.0625);
    }
}