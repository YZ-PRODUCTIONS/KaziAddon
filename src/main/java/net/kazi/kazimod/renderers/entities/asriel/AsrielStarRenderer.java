package net.kazi.kazimod.renderers.entities.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.asriel.AsrielStarEntity;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public final class AsrielStarRenderer extends EntityRenderer<AsrielStarEntity> {
    private static final ResourceLocation UNUSED=new ResourceLocation("minecraft","textures/block/white_wool.png");
    public AsrielStarRenderer(EntityRendererManager manager){super(manager);}
    @Override public ResourceLocation getTextureLocation(AsrielStarEntity entity){return UNUSED;}
    @Override public boolean shouldRender(AsrielStarEntity e,ClippingHelper f,double x,double y,double z) {
        double radius=e.getVisualSize();
        return AsrielRenderSupport.visible(e,f,x,y,z,radius*(e.isSmallStar()?1.5:3.5),radius*(e.isSmallStar()?1.5:3.5));
    }
    @Override public void render(AsrielStarEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        int detail=VfxDetail.level(e,entityRenderDispatcher,e.getVisualSize()*3.5);
        stack.pushPose();
        // Keep the reference's five-point silhouette readable even while circling the impact area.
        stack.mulPose(entityRenderDispatcher.cameraOrientation());
        AsrielVisualGeometry.star(AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),2.4F/e.getVisualSize()),
                e.getVisualSize(),e.getVisualAge(partial),e.isEnhanced(),e.isSmallStar(),e.isFinisher(),detail);
        stack.popPose();
    }
}
