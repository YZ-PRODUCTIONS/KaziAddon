package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.gomu.GomuLimbClient;
import net.kazi.kazimod.gomu.GomuLimbRenderer;
import net.kazi.kazimod.gomu.GomuLimbs;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

@Mixin(EntityRendererManager.class)
public abstract class GomuRenderContextMixin {
    @Unique private GomuLimbRenderer kazi$limbRenderer;
    @Inject(method = "getRenderer", at = @At("RETURN"), cancellable = true)
    private void kazi$limbRenderer(Entity entity, CallbackInfoReturnable<EntityRenderer<?>> cir) {
        if (!(entity instanceof AbilityProjectileEntity) || !GomuLimbs.supports((AbilityProjectileEntity)entity)) return;
        if (kazi$limbRenderer == null) kazi$limbRenderer = new GomuLimbRenderer((EntityRendererManager)(Object)this);
        cir.setReturnValue(kazi$limbRenderer);
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void kazi$begin(Entity entity, double x, double y, double z, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light, CallbackInfo ci) {
        GomuLimbClient.begin(entity, x, y, z, partial, stack);
    }
    @Inject(method = "render", at = @At("RETURN"))
    private void kazi$end(Entity entity, double x, double y, double z, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light, CallbackInfo ci) {
        GomuLimbClient.end();
    }
}
