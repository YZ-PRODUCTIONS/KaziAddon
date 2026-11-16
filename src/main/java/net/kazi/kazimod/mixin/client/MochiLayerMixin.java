package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.mochi.MochiLayers;
import net.MrMagicalCart.cartaddon.renderers.layers.morphs.*;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.*;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CarvedMochiLayer.class,ChikaraMochiLayer.class,GaltingMochiLayer.class,KakuMochiLayer.class,KashiwaMochiLayer.class,
        KuriMochiLayer.class,MochiTsukiLayer.class,MusoDonutSlamLayer.class,MusoMochiHadanLayer.class,
        SupremeSmashingMochiLayer.class,YakiMochiLayer.class},remap=false)
public abstract class MochiLayerMixin extends LayerRenderer<LivingEntity,EntityModel<LivingEntity>> {
    protected MochiLayerMixin(IEntityRenderer<LivingEntity,EntityModel<LivingEntity>> renderer){super(renderer);}
    @Inject(method="render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",at=@At("HEAD"),cancellable=true)
    private void kazi$fluid(MatrixStack s,IRenderTypeBuffer b,int light,LivingEntity e,float limb,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci) {
        if(!(getParentModel() instanceof BipedModel))return;
        if(!e.isInvisible())MochiLayers.render(getClass().getSimpleName(),(BipedModel<?>)getParentModel(),e,s,b,age,light);
        ci.cancel();
    }
}
