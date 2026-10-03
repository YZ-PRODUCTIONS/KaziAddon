package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import net.kazi.kazimod.kake.KakeVisuals;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class CasinoChipProjectile extends AbilityProjectileEntity {

    public static final float DEFAULT_DAMAGE = 6.0f;

    public CasinoChipProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public CasinoChipProjectile(World world, LivingEntity shooter) {
        super(CasinoProjectiles.CHIP.get(), world, shooter, CasinoRollAbility.INSTANCE);
        this.setDamage(DEFAULT_DAMAGE);
        this.setMaxLife(80);
        this.setGravity(0.07f);
        this.setEntityCollisionSize(4.3);
        super.setUnavoidable();

        this.onBlockImpactEvent = (pos) -> {
            if (!this.level.isClientSide) {
                KakeVisuals.impact(this,getThrower(),6);
                KakeVisuals.explode(this,1.8F);
                this.remove();
            }
        };
        this.onEntityImpactEvent = (target) -> {
            if (!this.level.isClientSide) {
                KakeVisuals.impact(this,getThrower(),6);
                KakeVisuals.explode(this,1.2F);
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).addEffect(
                            new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 100, 0, false, false));
                }
                this.remove();
            }
        };
    }
}
