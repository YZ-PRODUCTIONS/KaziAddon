package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;

@Mixin(HeldItemLayer.class)
public abstract class MammothHeldItemMixin {
    @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
    private void kazimod$hideFullFormItem(LivingEntity entity,ItemStack item,ItemCameraTransforms.TransformType type,HandSide side,
                                        MatrixStack stack,IRenderTypeBuffer buffer,int light,CallbackInfo ci) {
        MorphInfo info=MorphHelper.getZoanInfo(entity);
        if(info!=null&&info.getForm().equals("mammoth_guard"))ci.cancel();
    }
}
