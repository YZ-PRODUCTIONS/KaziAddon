package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.entities.zoans.CerberusGuardPointMorphInfo;
import net.MrMagicalCart.cartaddon.entities.zoans.CerberusHeadlessMorphInfo;
import net.kazi.kazimod.models.zoan.CerberusElbafModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

@Mixin(value={CerberusGuardPointMorphInfo.class, CerberusHeadlessMorphInfo.class},remap=false)
public abstract class CerberusGuardModelMixin {
    @Inject(method="getModel",at=@At("HEAD"),cancellable=true)
    private void kazimod$model(CallbackInfoReturnable<MorphModel> cir) {
        cir.setReturnValue(new CerberusElbafModel<>(getClass().getSimpleName().equals("CerberusHeadlessMorphInfo")));
    }
    @Inject(method="getTexture",at=@At("HEAD"),cancellable=true)
    private void kazimod$texture(CallbackInfoReturnable<ResourceLocation> cir) { cir.setReturnValue(CerberusElbafModel.TEXTURE); }
    @Inject(method="preRenderCallback",at=@At("HEAD"),cancellable=true)
    private void kazimod$scale(LivingEntity entity, MatrixStack stack,float partial,CallbackInfo ci) {
        stack.scale(CerberusElbafModel.FORM_SCALE,CerberusElbafModel.FORM_SCALE,CerberusElbafModel.FORM_SCALE);ci.cancel();
    }
}
