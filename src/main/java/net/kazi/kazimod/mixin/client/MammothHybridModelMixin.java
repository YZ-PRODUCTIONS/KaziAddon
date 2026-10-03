package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.models.morphs.MammothModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.entities.zoan.MammothHeavyMorphInfo;

@Mixin(value=MammothHeavyMorphInfo.class,remap=false)
public abstract class MammothHybridModelMixin extends MorphInfo {
    @Inject(method="getModel",at=@At("HEAD"),cancellable=true)
    private void kazimod$model(CallbackInfoReturnable<MorphModel> ci) { ci.setReturnValue(new MammothModel<>(true)); }
    @Override public ResourceLocation getTexture() { return MammothModel.HYBRID_TEXTURE; }
    @Override public boolean shouldRenderFirstPersonHand() { return false; }
    @Override public boolean shouldRenderFirstPersonLeg() { return false; }
    @Override public double getCameraZoom(LivingEntity entity) { return 7; }
    @Inject(method="preRenderCallback",at=@At("HEAD"),cancellable=true)
    private void kazimod$scale(LivingEntity e,MatrixStack s,float partial,CallbackInfo ci) { s.scale(2.9F,2.9F,2.9F);ci.cancel(); }
}
