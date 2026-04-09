package net.kazi.kazimod.mixin;

import net.kazi.kazimod.events.AwakeningAbilityLoginFix;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitBase;

@Mixin(value = DevilFruitBase.class, remap = false)
public abstract class DevilFruitBaseMixin {

    @Shadow
    private LivingEntity owner;

    @Inject(method = "setAwakenedFruit", at = @At("TAIL"), remap = false)
    private void kazi$syncMeraReplacementsOnAwakenToggle(boolean awakened, CallbackInfo ci) {
        if (this.owner instanceof PlayerEntity) {
            AwakeningAbilityLoginFix.syncPlayerAwakeningReplacements((PlayerEntity) this.owner);
        }
    }
}
