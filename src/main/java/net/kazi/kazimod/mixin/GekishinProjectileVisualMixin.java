package net.kazi.kazimod.mixin;

import net.minecraft.particles.IParticleData;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gura.GekishinProjectile;

@Mixin(value = {GekishinProjectile.class,
        net.MrMagicalCart.cartaddon.entities.projectiles.guraextra.NewGekishinProjectile.class,
        net.MrMagicalCart.cartaddon.entities.projectiles.guraextra.NewShingenNoIchigekiProjectile.class}, remap = false)
public abstract class GekishinProjectileVisualMixin {
    @Redirect(method = "onTickEvent", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/wypi/WyHelper;spawnParticles(Lnet/minecraft/particles/IParticleData;Lnet/minecraft/world/server/ServerWorld;DDD)V"))
    private void kazimod$removeSmoke(IParticleData data, ServerWorld world, double x, double y, double z) {}

    @Redirect(method = "onTickEvent", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/server/ServerWorld;sendParticles(Lnet/minecraft/particles/IParticleData;DDDIDDDD)I", remap = true))
    private <T extends IParticleData> int kazimod$removeExplosionSprites(ServerWorld world, T data,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed) { return 0; }
}
