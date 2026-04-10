package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.AkumaRework.DivineSlayerAbility;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class SlashWaveProjectile extends AbilityProjectileEntity {

    public SlashWaveProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public SlashWaveProjectile(World world, LivingEntity shooter) {
        super(AkumaProjectiles.SLASH_WAVE.get(), world, shooter, DivineSlayerAbility.INSTANCE);
        this.setDamage(DivineSlayerAbility.DAMAGE_VALUE);
        this.setMaxLife(30);
        this.setGravity(0.0f);
        this.setEntityCollisionSize(10.0);
        this.setPassThroughEntities();
        this.onTickEvent = () -> {
            if (!this.level.isClientSide) {
                // Wide horizontal slash particles — 10 blocks wide, 1.5 tall
                for (int i = 0; i < 14; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 10.0;
                    double offsetY = (this.random.nextDouble() - 0.5) * 1.5;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 10.0;
                    ((ServerWorld) this.level).sendParticles(ParticleTypes.SWEEP_ATTACK,
                            this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ,
                            1, 0, 0, 0, 0);
                }
                ((ServerWorld) this.level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        8, 4.0, 0.5, 4.0, 0.05);

                // Block destruction — destroy blocks in the path
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            if (this.random.nextFloat() < 0.3f) {
                                BlockPos pos = new BlockPos(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
                                BlockState state = this.level.getBlockState(pos);
                                if (!state.isAir() && state.getDestroySpeed(this.level, pos) >= 0
                                        && state.getBlock() != Blocks.BEDROCK) {
                                    this.level.destroyBlock(pos, false);
                                }
                            }
                        }
                    }
                }
            }
        };
    }
}
