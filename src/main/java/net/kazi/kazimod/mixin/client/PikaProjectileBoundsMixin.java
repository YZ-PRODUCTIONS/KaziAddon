package net.kazi.kazimod.mixin.client;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.pika.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.suna.DesertSpadaProjectile;
import net.MrMagicalCart.cartaddon.entities.projectiles.pikaclone.PikaExtraProjectiles;

@Mixin(value={AmaterasuProjectile.class,YasakaniNoMagatamaProjectile.class,DesertSpadaProjectile.class})
public abstract class PikaProjectileBoundsMixin extends AbilityProjectileEntity {
    protected PikaProjectileBoundsMixin(EntityType<?> type,World world){super(type,world);}
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        if(getType()==PikaProjectiles.AMATERASU.get()||getType()==PikaProjectiles.YASAKANI_NO_MAGATAMA.get()
                ||getType()==PikaExtraProjectiles.DIVINE_CUT.get())return distance<160*160;
        return super.shouldRenderAtSqrDistance(distance);
    }
}
