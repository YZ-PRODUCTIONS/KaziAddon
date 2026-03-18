package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import net.kazi.kazimod.particles.CasinoChipParticleEffect;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;

public class CasinoChipProjectile extends AbilityProjectileEntity {

    public static final float DEFAULT_DAMAGE = 6.0f;

    public CasinoChipProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public CasinoChipProjectile(World world, LivingEntity shooter) {
        super(CasinoProjectiles.COIN.get(), world, shooter, CasinoRollAbility.INSTANCE);
        this.setDamage(DEFAULT_DAMAGE);
        this.setMaxLife(80);
        this.setGravity(0.07f);
        this.setEntityCollisionSize(6.3);
        super.setUnavoidable();

        this.onTickEvent = () -> {
            if (!this.level.isClientSide) {
                CasinoChipParticleEffect.spawnAt(this, this.level, this.getX(), this.getY(), this.getZ());
            }
        };
        this.onBlockImpactEvent = (pos) -> {
            if (!this.level.isClientSide) {
                this.level.explode(this, this.getX(), this.getY(), this.getZ(), 1.8f, false, Explosion.Mode.NONE);
                this.remove();
            }
        };
        this.onEntityImpactEvent = (target) -> {
            if (!this.level.isClientSide) {
                this.level.explode(this, this.getX(), this.getY(), this.getZ(), 1.2f, false, Explosion.Mode.NONE);
                this.remove();
            }
        };
    }
}