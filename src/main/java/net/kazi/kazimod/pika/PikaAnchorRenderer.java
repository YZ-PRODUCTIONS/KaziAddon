package net.kazi.kazimod.pika;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;

public final class PikaAnchorRenderer extends EntityRenderer<PikaRenderAnchor> {
    private ClippingHelper frustum;
    public PikaAnchorRenderer(EntityRendererManager manager){super(manager);}
    @Override public void render(PikaRenderAnchor anchor,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        PikaEffects.render(stack,buffers,partial,PikaEffects.interpolate(anchor,partial),frustum);
    }
    @Override public boolean shouldRender(PikaRenderAnchor anchor,ClippingHelper frustum,double x,double y,double z){this.frustum=frustum;return true;}
    @Override public ResourceLocation getTextureLocation(PikaRenderAnchor anchor){return net.minecraft.client.renderer.texture.AtlasTexture.LOCATION_BLOCKS;}
}
