package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.kazi.kazimod.abilities.SakuRework.SenbonzakuraAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class PetalBladeProjectile extends AbilityProjectileEntity {

    public PetalBladeProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public PetalBladeProjectile(World world, LivingEntity shooter, float damage) {
        super(SakuProjectiles.PETAL_BLADE.get(), world, shooter, SenbonzakuraAbility.INSTANCE);
        this.setDamage(damage);
        this.setMaxLife(30);
        this.setGravity(0.01f);
        this.setEntityCollisionSize(0.6);
        this.setPassThroughEntities();
        this.onTickEvent = () -> {
            if (!this.level.isClientSide) {
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(10);
                data.setSize(3.0F);
                WyHelper.spawnParticles(data, (ServerWorld) this.level,
                        this.getX(), this.getY(), this.getZ());
            }
        };
    }
}
