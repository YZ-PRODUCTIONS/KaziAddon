//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.JikiRework.GenocideRaidRework;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.abilities.jiki.GenocideRaidAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class GenocideRaidReworkProjectile extends AbilityProjectileEntity {
    public GenocideRaidReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public GenocideRaidReworkProjectile(World world, LivingEntity player) {
        super((EntityType)JikiReworkProjectiles.GENOCIDE_RAID.get(), world, player, GenocideRaidRework.INSTANCE);
        this.setEntityCollisionSize((double)1.25F);
        this.setDamage(0.0F);
        this.setFake();
        this.setMaxLife(10);
        this.setPhysical();
    }

    public void onProjectileCollision(AbilityProjectileEntity owner, AbilityProjectileEntity target) {
    }
}
