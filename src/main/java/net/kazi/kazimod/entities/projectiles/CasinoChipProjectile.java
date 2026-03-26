package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import net.kazi.kazimod.particles.CasinoChipParticleEffect;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

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
        this.setEntityCollisionSize(4.3);
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
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).addEffect(
                            new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 100, 0, false, true));
                }
                this.remove();
            }
        };
    }
}