package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class LightPortalProjectile extends AbilityProjectileEntity {

    private int lifeTicks = 0;
    private static final int MAX_LIFE = 55;
    private Vector3d targetPos;

    public LightPortalProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public LightPortalProjectile(World world, LivingEntity shooter, AbilityCore<?> core, Vector3d targetPos) {
        super((EntityType<? extends AbilityProjectileEntity>) KaziEntities.LIGHT_PORTAL_PROJECTILE.get(), world, shooter, (Ability) null);
        this.targetPos = targetPos;
        this.setDamage(0.0F);
        this.setMaxLife(MAX_LIFE);
        this.setGravity(0.0F);
        this.setEntityCollisionSize(0.0D);
        this.setNoGravity(true);
        this.setPassThroughEntities();
        this.onTickEvent = () -> {
            if (!this.level.isClientSide) {
                SimpleParticleData data = new SimpleParticleData((ParticleType<?>) KaziParticleTypes.LIGHT_PORTAL.get());
                data.setLife(18);
                data.setSize(21.0F);
                WyHelper.spawnParticles(data, (ServerWorld) this.level, this.getX(), this.getY(), this.getZ());
            }
        };
    }

    @Override
    public void onHit(RayTraceResult hitResult) {
        this.remove();
    }
}
