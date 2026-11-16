package net.kazi.kazimod.mixin;

import net.kazi.kazimod.yami.DarknessLaunch;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class YamiDarknessLaunchTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void kazi$applyDarknessLaunch(CallbackInfo ci) {
        DarknessLaunch.finishTick((LivingEntity) (Object) this);
    }
}
