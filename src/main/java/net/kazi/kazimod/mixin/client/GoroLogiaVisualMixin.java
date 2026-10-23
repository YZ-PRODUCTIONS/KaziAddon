package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.goro.GoroAmbientEffects;
import net.minecraft.entity.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.goro.*;

@Mixin(value={GenericUseLightningEffect.class,GenericUseYellowLightningEffect.class},remap=false)
public abstract class GoroLogiaVisualMixin {
    @Inject(method="spawn(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;DDDLxyz/pixelatedw/mineminenomi/particles/effects/ParticleEffect$NoDetails;)V",at=@At("HEAD"),cancellable=true)
    private void kazi$logiaFlash(Entity entity,World world,double x,double y,double z,ParticleEffect.NoDetails details,CallbackInfo ci) {
        if(entity instanceof LivingEntity && DevilFruitCapability.get((LivingEntity)entity).hasDevilFruit(ModAbilities.GORO_GORO_NO_MI)) {
            GoroAmbientEffects.flash(entity);ci.cancel();
        }
    }
}
