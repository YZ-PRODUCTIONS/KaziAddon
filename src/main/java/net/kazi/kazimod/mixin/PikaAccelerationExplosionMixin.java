package net.kazi.kazimod.mixin;

import net.kazi.kazimod.pika.PikaVisuals;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;

@Mixin(targets={"net.MrMagicalCart.cartaddon.abilities.pikaextra.MaxAccelerationAbility","net.kazi.kazimod.abilities.PikaRework.MaxAccelerationRework"},remap=false)
public abstract class PikaAccelerationExplosionMixin {
    @Redirect(method={"duringContinuousEvent","duringChargeEvent"},at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;doExplosion()V"))
    private void kazi$goldRush(ExplosionAbility explosion,LivingEntity owner,IAbility ability){
        explosion.setSmokeParticles(null);
        net.kazi.kazimod.pika.PikaVisualPacket.send(owner,PikaVisuals.LOGIA,owner.position());
        explosion.doExplosion();
    }
}
