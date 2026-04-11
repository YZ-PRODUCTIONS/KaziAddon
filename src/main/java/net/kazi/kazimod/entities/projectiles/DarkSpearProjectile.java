package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.AkumaRework.TrillionDarkAbility;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class DarkSpearProjectile extends AbilityProjectileEntity {

    public DarkSpearProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public DarkSpearProjectile(World world, LivingEntity shooter) {
        super(AkumaProjectiles.DARK_SPEAR.get(), world, shooter, TrillionDarkAbility.INSTANCE);
        this.setDamage(TrillionDarkAbility.DAMAGE_VALUE);
        this.setMaxLife(40);
        this.setGravity(0.05f);
        this.setEntityCollisionSize(1.2);
        this.onTickEvent = () -> {
            if (!this.level.isClientSide) {
                ((ServerWorld) this.level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        3, 0.15, -0.3, 0.15, 0.01);
            }
        };
        this.onEntityImpactEvent = (target) -> {
            if (target instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) target;
                living.addEffect(new EffectInstance((Effect) ModEffects.BLEEDING.get(), 80, 0, false, false));
            }
        };
    }
}
