//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.swordsmanrework.YakkodoriRework;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.abilities.swordsman.YakkodoriAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.swordsman.SwordsmanProjectiles;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;

public class YakkodoriReworkProjectile extends AbilityProjectileEntity {
    public YakkodoriReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public YakkodoriReworkProjectile(World world, LivingEntity player) {
        super((EntityType) SwordsmanProjectiles.YAKKODORI.get(), world, player, YakkodoriRework.INSTANCE);
        this.setDamage(30.0F);
        this.setMaxLife(40);
        this.setBlocksAffectedLimit(512);
        this.setDamageSource(this.getDamageSource().setSlash());
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, (double)hit.getX(), (double)hit.getY(), (double)hit.getZ(), 1.0F);
        explosion.setStaticDamage(5.0F);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(2));
        explosion.doExplosion();
    }
}
