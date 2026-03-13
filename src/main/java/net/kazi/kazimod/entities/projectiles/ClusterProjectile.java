package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ClusterProjectile extends AbilityProjectileEntity {

    public ClusterProjectile(EntityType type, World world) {
        super(type, world);
    }

    public ClusterProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) BomuProjectiles.CLUSTER.get(), world, player, ability);
        this.setDamage(12.0F);
        this.setPassThroughEntities();
        this.setMaxLife(30);
        this.setHurtTime(5);
        this.setEntityCollisionSize((double) 3.0F, (double) 3.0F, (double) 3.0F);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent = this::onTickEvent;
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        AbilityHelper.setSecondsOnFireBy(hitEntity, 4, this.getThrower());
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level,
                (double) hit.getX(), (double) hit.getY(), (double) hit.getZ(), 2.0F);
        explosion.setStaticDamage(5.0F);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(2));
        explosion.doExplosion();
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {
            WyHelper.spawnParticleEffect((ParticleEffect) KaziParticleEffects.BAKUGO.get(), this,
                    this.getX(), this.getY(), this.getZ());
        }
    }
}