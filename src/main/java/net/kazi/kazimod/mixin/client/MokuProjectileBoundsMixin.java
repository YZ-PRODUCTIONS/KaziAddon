package net.kazi.kazimod.mixin.client;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.moku.*;
import net.MrMagicalCart.cartaddon.entities.projectiles.mokuextra.SmokeBlastProjectile;

@Mixin(value={WhiteBlowProjectile.class,WhiteBlowRushProjectile.class,WhiteOutProjectile.class,WhiteSnakeProjectile.class,SmokeBlastProjectile.class})
public abstract class MokuProjectileBoundsMixin extends AbilityProjectileEntity {
    protected MokuProjectileBoundsMixin(EntityType<?> type,World world){super(type,world);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<128*128;}
}
