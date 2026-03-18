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

public class HollowPurpleProjectile extends AbilityProjectileEntity {

    public HollowPurpleProjectile(EntityType type, World world) {
        super(type, world);
    }

    public HollowPurpleProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) GojoProjectiles.HOLLOW_PURPLE.get(), world, player, ability);
        super.setPassThroughEntities();
        this.setDamage(300.0F);
        this.setMaxLife(200);
        this.setHurtTime(5);
        this.setUnavoidable();
        this.setBlocksAffectedLimit(40960);
        this.setEntityCollisionSize(10.0F, 10.0F, 10.0F);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent = this::onTickEvent;
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        Vector3d direction = this.getDeltaMovement().normalize();
        // Much stronger knockback than red
        AbilityHelper.setDeltaMovement(hitEntity,
                direction.x * 5.0, 2.5, direction.z * 5.0);
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level,
                (double) hit.getX(), (double) hit.getY(), (double) hit.getZ(), 8.0F);
        explosion.setStaticDamage(3.0F);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.doExplosion();
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) KaziParticleEffects.GOJO_PURPLE.get(), this,
                    this.getX(), this.getY(), this.getZ()
            );
        }
    }
}