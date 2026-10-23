package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.seiryu.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRendererManager.class)
public abstract class SeiryuDebugHitboxMixin {
    @Inject(method="renderHitbox",at=@At("HEAD"),cancellable=true)
    private void kazimod$hideOwnBoxes(MatrixStack stack,IVertexBuilder buffer,Entity entity,float partial,CallbackInfo ci) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.getCameraEntity()!=mc.player||!mc.options.getCameraType().isFirstPerson())return;
        if(entity==mc.player&&SeiryuFlight.active(mc.player)||entity instanceof SeiryuPartEntity&&((SeiryuPartEntity)entity).owner()==mc.player)ci.cancel();
    }
}
