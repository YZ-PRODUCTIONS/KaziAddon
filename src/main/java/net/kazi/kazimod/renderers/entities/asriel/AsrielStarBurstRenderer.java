package net.kazi.kazimod.renderers.entities.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.asriel.AsrielStarBurstEntity;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

/** Screen-readable star outlines and mini-star rings, backed by one short-lived effect entity. */
public final class AsrielStarBurstRenderer extends EntityRenderer<AsrielStarBurstEntity> {
    private static final ResourceLocation UNUSED=new ResourceLocation("minecraft","textures/block/white_wool.png");
    public AsrielStarBurstRenderer(EntityRendererManager manager){super(manager);}
    @Override public ResourceLocation getTextureLocation(AsrielStarBurstEntity entity){return UNUSED;}
    @Override public boolean shouldRender(AsrielStarBurstEntity e,ClippingHelper f,double x,double y,double z) {
        double extent=e.getVisualSize()*2.3;
        return AsrielRenderSupport.visible(e,f,x,y,z,extent,extent);
    }
    @Override public void render(AsrielStarBurstEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        int detail=VfxDetail.level(e,entityRenderDispatcher,e.getVisualSize()*2.3);
        stack.pushPose();
        stack.mulPose(entityRenderDispatcher.cameraOrientation());
        AsrielVisualGeometry.starBurst(AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),2.4F/e.getVisualSize()),
                e.getVisualSize(),e.getVisualAge(partial),e.isEnhanced(),e.isFinisher(),detail);
        stack.popPose();
    }
}
