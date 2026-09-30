package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class RedProjectile extends AbilityProjectileEntity {

    public RedProjectile(EntityType type, World world) {
        super(type, world);
    }

    public RedProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) GojoProjectiles.RED.get(), world, player, ability);
        super.setPassThroughEntities();
        this.setDamage(50.0F);
        this.setMaxLife(80);
        this.setHurtTime(5);
        this.setUnavoidable();
        this.setBlocksAffectedLimit(2048);
        this.setEntityCollisionSize((double) 5.0F, (double) 5.0F, (double) 5.0F);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent = this::onTickEvent;
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        Vector3d direction = this.getDeltaMovement().normalize();
        AbilityHelper.setDeltaMovement(hitEntity, direction.x * 1.40, 1.25, direction.z * 1.40);
        net.kazi.kazimod.entities.KokuVfxEntity.impact(this.level, hitEntity.position().add(0.0D, 1.0D, 0.0D),
                net.kazi.kazimod.entities.KokuVfxEntity.RED_IMPACT, 3.0F);
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level,
                (double) hit.getX(), (double) hit.getY(), (double) hit.getZ(), 4.0F);
        explosion.setStaticDamage(3.0F);
        explosion.setSmokeParticles(null);
        explosion.doExplosion();
            net.kazi.kazimod.entities.KokuVfxEntity.impact(this.level, Vector3d.atCenterOf(hit),
                    net.kazi.kazimod.entities.KokuVfxEntity.RED_IMPACT, 4.0F);
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {
            // Flight visuals are handled by KokuProjectileRenderer.
        }
    }
}
