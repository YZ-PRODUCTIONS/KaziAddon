package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.abilities.GoruRework.ShaNaqbaImuruAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.renderers.layers.AuraLayer;

/** Reuses the original synced icon cache, decorations and fade timing; skips aura geometry. */
@Mixin(value = AuraLayer.class, remap = false)
public abstract class ShaNaqbaImuruAuraMixin {
    @Unique private boolean kazi$passive;
    @Unique private boolean kazi$namesOnly;
    @Unique private static final String RENDER = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void kazi$begin(MatrixStack stack, IRenderTypeBuffer buffers, int light, LivingEntity target,
                            float a, float b, float partial, float age, float yaw, float pitch, CallbackInfo ci) {
        kazi$passive = target instanceof PlayerEntity && ShaNaqbaImuruAbility.active(Minecraft.getInstance().player);
        kazi$namesOnly = false;
    }

    @ModifyVariable(method = RENDER, at = @At("STORE"), ordinal = 0)
    private boolean kazi$enableDisplay(boolean auraActive) {
        kazi$namesOnly = kazi$passive && !auraActive;
        return auraActive || kazi$passive;
    }

    @Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lxyz/pixelatedw/mineminenomi/data/entity/haki/IHakiData;getKenbunshokuHakiExp()F"))
    private float kazi$displayWithoutHakiTraining(IHakiData haki) {
        return kazi$passive ? Math.max(50, haki.getKenbunshokuHakiExp()) : haki.getKenbunshokuHakiExp();
    }

    @Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V", remap = true))
    private void kazi$skipPassiveGlow(EntityModel<?> model, MatrixStack stack, IVertexBuilder vertices,
                                      int light, int overlay, float red, float green, float blue, float alpha) {
        if (!kazi$namesOnly) model.renderToBuffer(stack, vertices, light, overlay, red, green, blue, alpha);
    }
}
