package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.abilities.TripelT.ArrowsOfLightAbility;
import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class LightArrowProjectile extends AbilityProjectileEntity {

    public LightArrowProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public LightArrowProjectile(World world, LivingEntity shooter, float damage) {
        super((EntityType<? extends AbilityProjectileEntity>) KaziEntities.LIGHT_ARROW_PROJECTILE.get(), world, shooter, ArrowsOfLightAbility.INSTANCE);
        this.setDamage(damage);
        this.setMaxLife(90);
        this.setGravity(0.0F);
        this.setEntityCollisionSize(2.8D);
        this.setPassThroughEntities();
        // Geometry and trail are rendered client-side; no per-tick particle packets.
    }
}
