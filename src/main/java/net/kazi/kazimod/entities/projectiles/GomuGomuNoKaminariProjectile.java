package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoKaminariAbility;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GomuGomuNoKaminariProjectile extends AbilityProjectileEntity {

    public GomuGomuNoKaminariProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public GomuGomuNoKaminariProjectile(World world, LivingEntity player) {
        super((EntityType<?>) GomuReworkProjectiles.GOMU_GOMU_NO_KAMINARI.get(), world, player, GomuGomuNoKaminariAbility.INSTANCE);
        this.setDamage(0.0F);
        this.setArmorPiercing(0.0F);
        this.setPassThroughEntities();
        this.setBlocksAffectedLimit(2000);
        this.setMaxLife(60);
        this.onTickEvent = this::onTickEvent;
        this.onBlockImpactEvent = this::onHitBlock;
    }

    private void onHitBlock(BlockPos blockPos) {
        ExplosionAbility explosion = super.createExplosion(
                this.getThrower(), this.level,
                (double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ(), 5.5F);
        explosion.setFireAfterExplosion(false);
        explosion.doExplosion();
    }

    private void onTickEvent() {
        if (!super.level.isClientSide) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) ModParticleEffects.SANGO.get(),
                    this,
                    super.getX(),
                    super.getY(),
                    super.getZ());
        }
    }
}