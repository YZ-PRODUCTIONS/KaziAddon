package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.mochi.MochiVisuals;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MochiProjectileBoundsMixin {
    @Inject(method="shouldRenderAtSqrDistance",at=@At("HEAD"),cancellable=true)
    private void kazi$visible(double distance,CallbackInfoReturnable<Boolean> cir) {
        if(MochiVisuals.projectile((Entity)(Object)this))cir.setReturnValue(distance<128*128);
    }
}
