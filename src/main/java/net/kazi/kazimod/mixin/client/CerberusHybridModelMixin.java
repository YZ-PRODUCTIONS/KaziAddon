package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.entities.zoans.CerberusHeavyPointMorphInfo;
import net.kazi.kazimod.models.zoan.CerberusElbafModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

@Mixin(value=CerberusHeavyPointMorphInfo.class,remap=false)
public abstract class CerberusHybridModelMixin {
    @Inject(method="getModel",at=@At("HEAD"),cancellable=true)
    private void kazimod$model(CallbackInfoReturnable<MorphModel> cir){cir.setReturnValue(new CerberusElbafModel<>(false,true));}
    @Inject(method="getTexture",at=@At("HEAD"),cancellable=true)
    private void kazimod$texture(CallbackInfoReturnable<ResourceLocation> cir){cir.setReturnValue(new ResourceLocation("kazimod","textures/models/zoan/cerberus_hybrid.png"));}
    @Inject(method="preRenderCallback",at=@At("HEAD"),cancellable=true)
    private void kazimod$scale(LivingEntity e,MatrixStack stack,float partial,CallbackInfo ci){stack.scale(1.04F,1.04F,1.04F);ci.cancel();}
}
