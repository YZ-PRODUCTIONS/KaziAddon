package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.NagiRework.SilentDeathAbility;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class SilentDeathProjectile extends AbilityProjectileEntity {

    public SilentDeathProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public SilentDeathProjectile(World world, LivingEntity shooter) {
        super(NagiProjectiles.SILENT_DEATH.get(), world, shooter, SilentDeathAbility.INSTANCE);
        this.setDamage(SilentDeathAbility.DAMAGE_VALUE);
        this.setMaxLife(80);
        this.setGravity(0.0f);
        this.setEntityCollisionSize(4.0);

        this.onEntityImpactEvent = (target) -> {
            if (target instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) target;
                // Slowness 2 for 5 seconds
                living.addEffect(new EffectInstance(
                        Effects.MOVEMENT_SLOWDOWN, 100, 1, false, false));
                // Doku Poison 2 for 5 seconds
                living.addEffect(new EffectInstance(
                        (Effect) ModEffects.DOKU_POISON.get(), 100, 1, false, false));
                // Blindness 2 for 5 seconds
                living.addEffect(new EffectInstance(
                        Effects.BLINDNESS, 100, 1, false, false));
            }
        };
    }

}
