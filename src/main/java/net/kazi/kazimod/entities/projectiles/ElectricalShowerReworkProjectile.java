//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectricalShowerAbility;
import net.MrMagicalCart.cartaddon.entities.projectiles.electro.CartElectroProjectiles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ElectricalShowerReworkProjectile extends AbilityProjectileEntity {
    public ElectricalShowerReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public ElectricalShowerReworkProjectile(World world, LivingEntity entity) {
        super((EntityType) CartElectroProjectiles.ELECTRICAL_SHOWER.get(), world, entity, CartElectricalShowerAbility.INSTANCE);
        this.setMaxLife(40);
        this.setPassThroughEntities();
        this.setDamage(12.0F);
        this.setEntityCollisionSize((double)1.5F, (double)1.5F, (double)1.5F);
        this.onTickEvent = this::onTickEvent;
    }

    private void onTickEvent() {
        if (this.tickCount >= 0) {
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.ELECTRICAL_LUNA.get(), this, this.getX(), this.getY(), this.getZ());
        }

    }
}
