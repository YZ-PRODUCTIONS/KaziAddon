//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.renderers.entities.projectiles;

import net.MrMagicalCart.cartaddon.entities.projectiles.nitoryu.NitoryuProjectiles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class TakaNamiReworkProjectile extends AbilityProjectileEntity {
    public TakaNamiReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public TakaNamiReworkProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) NitoryuProjectiles.TAKA_NAMI.get(), world, player, ability);
        this.setDamage(30.0F);
        this.setPassThroughEntities();
        this.setCanGetStuckInGround();
        this.setMaxLife(60);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onHitBlock;
    }

    private void onHitBlock(BlockPos blockPos) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 5.0F);
        explosion.setStaticDamage(0.0F);
        explosion.setFireAfterExplosion(false);
        explosion.setDamageEntities(false);
        explosion.doExplosion();
        this.setLife(1);
    }

    private void onEntityImpactEvent(LivingEntity entity) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.BLEEDING.get(), 60, 0));
    }
}
