package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.effects.LaserBeamVisualData;
import net.kazi.kazimod.renderers.entities.LaserBeamVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.renderers.entities.LightningEntityRenderer;

@Mixin(value={LightningEntityRenderer.class}, remap=false)
public abstract class LaserBeamRendererMixin {
    @Inject(method={"renderLightning"}, at={@At(value="HEAD")}, cancellable=true)
    private static void kazimod$laser(LightningEntity entity, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light, CallbackInfo ci) {
        if (((LaserBeamVisualData)entity).kazimod$isLaser()) {
            LaserBeamVfxRenderer.render(entity, partial, stack, buffer);
            ci.cancel();
        }
    }
}

