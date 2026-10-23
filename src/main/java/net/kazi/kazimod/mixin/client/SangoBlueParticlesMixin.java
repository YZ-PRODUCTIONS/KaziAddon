package net.kazi.kazimod.mixin.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.goro.SangoParticleEffect;

@Mixin(value = SangoParticleEffect.class, remap = false)
public abstract class SangoBlueParticlesMixin {
    @Redirect(method = "spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$NoDetails;)V",
            at = @At(value = "INVOKE", target = "Lxyz/pixelatedw/mineminenomi/config/ClientConfig;isGoroBlue()Z"))
    private boolean kazi$blueCharge(ClientConfig config, Entity owner, World world,
                                     double x, double y, double z, ParticleEffect.NoDetails details) {
        return owner instanceof LivingEntity
                && DevilFruitCapability.get((LivingEntity) owner).hasDevilFruit(ModAbilities.GORO_GORO_NO_MI)
                || config.isGoroBlue();
    }
}
