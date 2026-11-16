package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.NagiRework.SilentSliceAbility;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class SilentSliceProjectile extends AbilityProjectileEntity {

    public SilentSliceProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public SilentSliceProjectile(World world, LivingEntity shooter) {
        super(NagiProjectiles.SILENT_SLICE.get(), world, shooter, SilentSliceAbility.INSTANCE);
        this.setDamage(SilentSliceAbility.DAMAGE_VALUE);
        this.setMaxLife(30);
        this.setGravity(0.0f);
        this.setEntityCollisionSize(3.0);
        this.setPassThroughEntities();

        // Invisible projectile — no particles, no rendering
        this.setInvisible(true);

        this.onTickEvent = () -> {
            // Completely silent and invisible — no particles
        };
    }
}
