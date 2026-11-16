package net.kazi.kazimod.mixin;

import net.kazi.kazimod.mochi.*;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

@Mixin(value=AbilityProjectileEntity.class,remap=false)
public abstract class MochiImpactMixin {
    @Inject(method="onModHit",at=@At("TAIL"))
    private void kazi$splash(RayTraceResult hit,CallbackInfo ci) {
        AbilityProjectileEntity p=(AbilityProjectileEntity)(Object)this;
        if(MochiVisuals.projectile(p)&&hit.getType()!=RayTraceResult.Type.MISS)
            MochiVisualPacket.send(p,MochiVisuals.IMPACT,hit.getLocation(),Math.max(.7F,Math.min(6,p.getBbWidth()*1.2F)));
    }
}
