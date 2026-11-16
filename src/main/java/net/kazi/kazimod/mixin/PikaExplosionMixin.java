package net.kazi.kazimod.mixin;

import net.kazi.kazimod.pika.PikaVisuals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.pika.*;

@Mixin(value={AmaterasuProjectile.class,YasakaniNoMagatamaProjectile.class},remap=false)
public abstract class PikaExplosionMixin {
    @Redirect(method="onBlockImpactEvent",at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/api/abilities/ExplosionAbility;doExplosion()V"))
    private void kazi$goldImpact(ExplosionAbility explosion){PikaVisuals.explode(explosion,(Entity)(Object)this);}
}
