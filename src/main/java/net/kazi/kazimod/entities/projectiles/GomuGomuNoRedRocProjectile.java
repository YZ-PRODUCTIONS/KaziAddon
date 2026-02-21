//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuGomuNoPistolAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuProjectiles;

public class GomuGomuNoRedRocProjectile extends AbilityProjectileEntity {
    public GomuGomuNoRedRocProjectile(EntityType type, World world) {
        super(type, world);
    }

    public GomuGomuNoRedRocProjectile(World world, LivingEntity player) {
        super((EntityType) GomuProjectiles.GOMU_GOMU_NO_PISTOL.get(), world, player, GomuGomuNoPistolAbility.INSTANCE);
        this.setDamage(6.0F);
        this.setMaxLife(9);
        super.setFist();
        this.setEntityCollisionSize((double)1.0F);
        this.setDamageSource(this.getDamageSource().setSourceElement(SourceElement.RUBBER));
    }
}
