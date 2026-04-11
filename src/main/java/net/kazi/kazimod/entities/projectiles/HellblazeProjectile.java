package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.AkumaRework.HellblazeAbility;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class HellblazeProjectile extends AbilityProjectileEntity {

    public HellblazeProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public HellblazeProjectile(World world, LivingEntity shooter) {
        super(AkumaProjectiles.HELLBLAZE.get(), world, shooter, HellblazeAbility.INSTANCE);
        this.setDamage(HellblazeAbility.DAMAGE_VALUE);
        this.setMaxLife(40);
        this.setGravity(0.0f);
        this.setEntityCollisionSize(4.0);
        this.setPassThroughEntities();
        this.onTickEvent = () -> {
            if (!this.level.isClientSide) {
                ServerWorld sw = (ServerWorld) this.level;

                // Mera fire particles matching 4x4 hitbox
                for (int i = 0; i < 14; i++) {
                    SimpleParticleData fire = new SimpleParticleData(
                            (net.minecraft.particles.ParticleType) ModParticleTypes.MERA.get());
                    fire.setLife(8);
                    fire.setSize(1.5F);
                    WyHelper.spawnParticles(fire, sw,
                            this.getX() + (this.random.nextDouble() - 0.5) * 3.0,
                            this.getY() + (this.random.nextDouble() - 0.5) * 3.0,
                            this.getZ() + (this.random.nextDouble() - 0.5) * 3.0);
                }

                // Smoke wisps
                for (int i = 0; i < 3; i++) {
                    SimpleParticleData smoke = new SimpleParticleData(
                            (net.minecraft.particles.ParticleType) ModParticleTypes.MOKU.get());
                    smoke.setLife(5);
                    smoke.setSize(1.1F);
                    WyHelper.spawnParticles(smoke, sw,
                            this.getX() + (this.random.nextDouble() - 0.5) * 2.0,
                            this.getY() + (this.random.nextDouble() - 0.5) * 2.0,
                            this.getZ() + (this.random.nextDouble() - 0.5) * 2.0);
                }

                // Destroy ground blocks below the projectile
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos pos = new BlockPos(this.getX() + dx, this.getY() - 1, this.getZ() + dz);
                        BlockState state = this.level.getBlockState(pos);
                        if (!state.isAir() && state.getDestroySpeed(this.level, pos) >= 0
                                && state.getBlock() != Blocks.BEDROCK) {
                            this.level.destroyBlock(pos, false);
                        }
                    }
                }
            }
        };
        this.onEntityImpactEvent = (target) -> {
            if (target instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) target;
                living.setSecondsOnFire(6);
                living.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 120, 1, false, false));
            }
        };
        this.onBlockImpactEvent = (pos) -> {
            if (!this.level.isClientSide) {
                // Destroy blocks in a 3x3x3 area on impact
                BlockPos hitPos = new BlockPos(pos);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            BlockPos breakPos = hitPos.offset(dx, dy, dz);
                            BlockState state = this.level.getBlockState(breakPos);
                            if (!state.isAir() && state.getDestroySpeed(this.level, breakPos) >= 0
                                    && state.getBlock() != Blocks.BEDROCK) {
                                this.level.destroyBlock(breakPos, false);
                            }
                        }
                    }
                }
                // Impact particles
                ((ServerWorld) this.level).sendParticles(ParticleTypes.EXPLOSION,
                        this.getX(), this.getY(), this.getZ(),
                        3, 1.0, 0.5, 1.0, 0);
            }
        };
    }
}
