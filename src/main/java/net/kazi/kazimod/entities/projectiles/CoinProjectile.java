package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import net.kazi.kazimod.particles.CoinParticleEffect;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;

public class CoinProjectile extends AbilityProjectileEntity {

    public static final float DEFAULT_DAMAGE = 4.0f;

    public CoinProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public CoinProjectile(World world, LivingEntity shooter) {
        // Pass AbilityCore so the projectile inherits SourceHakiNature/SourceElement from the core
        super(CasinoProjectiles.COIN.get(), world, shooter, CasinoRollAbility.INSTANCE);
        this.setDamage(DEFAULT_DAMAGE);
        this.setMaxLife(40);
        this.setKnockbackStrength(2);
        this.setGravity(0.01f);
        this.setEntityCollisionSize(0.8);
        this.onTickEvent = () -> {
            if (!this.level.isClientSide) {
                CoinParticleEffect.spawnAt(this, this.level, this.getX(), this.getY(), this.getZ());
            }
        };
    }
}