package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.client.zoan.ZoanTransitions;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "xyz.pixelatedw.mineminenomi.events.MorphEvents$Client", remap = false)
public abstract class ZoanTransitionRenderMixin {
    @Inject(method = "onEntityRendered", at = @At("HEAD"), cancellable = true)
    private static void kazimod$transition(RenderLivingEvent.Pre<?, ?> event, CallbackInfo ci) {
        if (ZoanTransitions.render(event)) ci.cancel();
    }
}
