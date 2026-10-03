package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.renderers.layers.morphs.KaishinLayer;
import net.MrMagicalCart.cartaddon.renderers.layers.morphs.ShingenLeftLayer;
import net.MrMagicalCart.cartaddon.renderers.layers.morphs.ShingenRightLayer;
import net.kazi.kazimod.models.abilities.GuraVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ShingenLeftLayer.class, ShingenRightLayer.class, KaishinLayer.class}, remap = false)
public abstract class CartGuraChargeLayerMixin extends LayerRenderer<LivingEntity, EntityModel<LivingEntity>> {
    protected CartGuraChargeLayerMixin(IEntityRenderer<LivingEntity, EntityModel<LivingEntity>> parent) { super(parent); }

    @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void kazimod$fracturedHands(MatrixStack stack, IRenderTypeBuffer buffer, int light, LivingEntity entity,
            float limbSwing, float limbAmount, float partial, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (!entity.isInvisible() && getParentModel() instanceof BipedModel) {
            BipedModel<?> model = (BipedModel<?>) getParentModel();
            boolean both = (Object) this instanceof KaishinLayer;
            if (both || (Object) this instanceof ShingenLeftLayer) kazimod$hand(stack, buffer, model.leftArm, .0625F, age);
            if (both || (Object) this instanceof ShingenRightLayer) kazimod$hand(stack, buffer, model.rightArm, -.0625F, age);
        }
        ci.cancel();
    }
    @Unique private static void kazimod$hand(MatrixStack stack, IRenderTypeBuffer buffer, ModelRenderer arm, float x, float age) {
        stack.pushPose();
        arm.translateAndRotate(stack);
        stack.translate(x, .5, 0);
        GuraVfxMesh.bubble(KokuVfxRenderer.sink(stack, buffer), .48F, age, 1);
        stack.popPose();
    }
}
