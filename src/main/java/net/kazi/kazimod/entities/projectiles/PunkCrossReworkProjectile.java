//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.JikiRework.PunkCrossRework;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.abilities.jiki.PunkCrossAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class PunkCrossReworkProjectile extends AbilityProjectileEntity {
    public PunkCrossReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public PunkCrossReworkProjectile(World world, LivingEntity player) {
        super((EntityType)JikiReworkProjectiles.PUNK_CROSS.get(), world, player, PunkCrossRework.INSTANCE);
        this.setEntityCollisionSize((double)1.25F);
        this.setDamage(0.0F);
        this.setFake();
        this.setMaxLife(5);
        this.setPhysical();
    }

    public void remove() {
        super.remove();
    }

    public void onProjectileCollision(AbilityProjectileEntity owner, AbilityProjectileEntity target) {
    }
}
