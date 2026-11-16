package net.kazi.kazimod.yami;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;

public final class YamiAnchorRenderer extends EntityRenderer<YamiRenderAnchor> {
    private ClippingHelper frustum;
    public YamiAnchorRenderer(EntityRendererManager manager){super(manager);}
    @Override public void render(YamiRenderAnchor anchor,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        YamiEffects.render(stack,buffers,partial,YamiEffects.interpolate(anchor,partial),frustum);
    }
    @Override public boolean shouldRender(YamiRenderAnchor anchor,ClippingHelper frustum,double x,double y,double z){this.frustum=frustum;return true;}
    @Override public ResourceLocation getTextureLocation(YamiRenderAnchor anchor){return YamiMesh.SMOKE;}
}
