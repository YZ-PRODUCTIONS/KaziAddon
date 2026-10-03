package net.kazi.kazimod.mixin.balance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.renderers.entities.LightningEntityRenderer;

@Mixin(value = LightningEntityRenderer.class, remap = false, priority = 2000)
public abstract class TripleTHeavenlyBeamMixin {
    @Inject(method = "renderLightning", at = @At("HEAD"), cancellable = true)
    private static void kazimod$hiddenGameplayBeam(LightningEntity entity, float partial, MatrixStack stack,
                                                  IRenderTypeBuffer buffers, int light, CallbackInfo ci) {
        if (entity.isInvisible()) ci.cancel();
    }
}
