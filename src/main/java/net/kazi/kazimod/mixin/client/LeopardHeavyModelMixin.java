package net.kazi.kazimod.mixin.client;
import net.kazi.kazimod.leopard.LeopardModel;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.entities.zoan.LeopardHeavyMorphInfo;
@Mixin(value=LeopardHeavyMorphInfo.class, remap=false)
public abstract class LeopardHeavyModelMixin extends MorphInfo {
    @Inject(method="getModel", at=@At("HEAD"), cancellable=true)
    private void kazi$model(CallbackInfoReturnable<MorphModel> ci) { ci.setReturnValue(new LeopardModel<>(1)); }
    @Override public ResourceLocation getTexture() { return LeopardModel.texture(1); }
}
