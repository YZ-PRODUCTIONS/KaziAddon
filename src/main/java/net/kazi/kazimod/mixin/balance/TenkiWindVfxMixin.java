package net.kazi.kazimod.mixin.balance;

import net.minecraft.entity.Entity;
import net.kazi.kazimod.preserved.tenki.TenkiEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.kazi.kazimod.entities.projectiles.WindGustProjectile",remap=false)
public abstract class TenkiWindVfxMixin {
    @Inject(method="onTickEvent",at=@At("HEAD"))
    private void kazimod$gust(CallbackInfo ci){TenkiEffects.attach((Entity)(Object)this,3,6);}
}
