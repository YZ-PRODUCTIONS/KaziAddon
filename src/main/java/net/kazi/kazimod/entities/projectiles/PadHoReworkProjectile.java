//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

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
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.Objects;

public class PadHoReworkProjectile extends AbilityProjectileEntity {
    public PadHoReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public PadHoReworkProjectile(World world, LivingEntity player, Ability ability) {
        super(
                Objects.requireNonNull(
                        NikyuReworkProjectiles.PAD_HO.get(),
                        "PAD_HO entity type not yet registered"
                ),
                world, player, ability
        );
        this.setDamage(15.0F);
        this.setArmorPiercing(1.0F);
        this.setPassThroughEntities();
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onEntityImpactEvent = this::onEntityImpactEvent;
    }

    private void onEntityImpactEvent(LivingEntity target) {
        this.onBlockImpactEvent.onImpact(target.blockPosition());
        if (this.getDamage() > 10.0F) {
            Vector3d speed = target.getLookAngle().multiply((double)-1.0F, (double)-1.0F, (double)-1.0F).multiply(WyHelper.randomWithRange(4, 6), WyHelper.randomWithRange(1, 3), WyHelper.randomWithRange(4, 6));
            AbilityHelper.setDeltaMovement(target, speed.x, speed.y, speed.z);
            target.fallDistance = 0.0F;
        }

    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, (double)hit.getX(), (double)hit.getY(), (double)hit.getZ(), this.getDamage() / 5.0F);
        explosion.setStaticDamage(this.getDamage() / 3.0F);
        explosion.setExplosionSound(true);
        explosion.setDamageOwner(false);
        explosion.setDestroyBlocks(true);
        explosion.setFireAfterExplosion(false);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(2));
        explosion.setDamageEntities(false);
        explosion.doExplosion();
    }
}
