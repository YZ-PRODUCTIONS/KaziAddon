package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.gomu.GomuLimbClient;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;
import xyz.pixelatedw.mineminenomi.renderers.abilities.StretchingProjectileRenderer;

@Mixin(value = StretchingProjectileRenderer.class, remap = false)
public abstract class GomuLimbRendererMixin {
    @Inject(method = "render(Lxyz/pixelatedw/mineminenomi/entities/projectiles/AbilityProjectileEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V", at = @At("HEAD"), cancellable = true)
    private void kazi$connectedArm(AbilityProjectileEntity p, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light, CallbackInfo ci) {
        if (GomuLimbClient.render(p, partial, stack, buffers, light, (AbilityProjectileRenderer<?, ?>)(Object)this)) ci.cancel();
    }
}
