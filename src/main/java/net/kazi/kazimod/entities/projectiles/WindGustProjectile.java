package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.Tenki.WindGustAbility;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class WindGustProjectile extends AbilityProjectileEntity {
    Vector3d look;
    boolean canGrief = false;

    public WindGustProjectile(EntityType type, World world) {
        super(type, world);
        this.look = Vector3d.ZERO;
    }

    public WindGustProjectile(World world, LivingEntity player) {
        super((EntityType) TenkiProjectiles.WIND_GUST.get(), world, player, WindGustAbility.INSTANCE);
        this.setDamage(15.0F);
        this.setMaxLife(15);
        this.look = player.getLookAngle();
        this.setPassThroughBlocks();
        this.setPassThroughEntities();
        if (player instanceof PlayerEntity) {
            this.canGrief = true;
        }

        this.setDamageSource(this.getDamageSource().setSlash());
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onEntityImpactEvent = this::onHitEntity;
        this.onTickEvent = this::onTickEvent;
    }

    private void onTickEvent() {
        WyHelper.spawnParticleEffect((ParticleEffect) KaziParticleEffects.WIND_GUST.get(), this, this.getX(), this.getY(), this.getZ());
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, this.getX(), this.getY(), this.getZ(), 6.0F);
        explosion.setExplosionSound(false);
        explosion.setDestroyBlocks(this.canGrief);
        explosion.doExplosion();
        if ((float) this.tickCount % 5.0F == 0.0F) {
            this.level.playSound((PlayerEntity) null, this.blockPosition(), (SoundEvent) ModSounds.SPIN.get(), SoundCategory.PLAYERS, 2.0F, 2.0F);
        }
    }

    private void onHitEntity(LivingEntity hitEntity) {
        Vector3d speed = this.look.normalize().multiply((double) 5.0F, (double) 1.0F, (double) 5.0F).add((double) 0.0F, 0.15, (double) 0.0F);
        hitEntity.setDeltaMovement(speed);
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, (double) hit.getX(), (double) hit.getY(), (double) hit.getZ(), 1.0F);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.doExplosion();
    }
}