package net.kazi.kazimod.renderers.entities.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.asriel.AsrielChaosProjectile;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public final class AsrielChaosProjectileRenderer extends EntityRenderer<AsrielChaosProjectile> {
    private static final ResourceLocation UNUSED=new ResourceLocation("minecraft","textures/block/white_wool.png");
    public AsrielChaosProjectileRenderer(EntityRendererManager manager){super(manager);}
    @Override public ResourceLocation getTextureLocation(AsrielChaosProjectile entity){return UNUSED;}
    @Override public boolean shouldRender(AsrielChaosProjectile e,ClippingHelper f,double x,double y,double z) {
        if(e.getKind()==AsrielChaosProjectile.BUSTER) return AsrielRenderSupport.visible(e,f,x,y,z,4,4);
        if(!e.shouldRender(x,y,z)) return false;
        AxisAlignedBB bounds=new AxisAlignedBB(e.position(),e.position());
        for(Vector3d point:e.getAngelTrail(1)) bounds=bounds.minmax(new AxisAlignedBB(point,point));
        return f.isVisible(bounds.inflate(AngelOfDeathVisuals.PADDING));
    }
    @Override public void render(AsrielChaosProjectile e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        stack.pushPose();
        if(e.getKind()==AsrielChaosProjectile.BUSTER) {
            stack.mulPose(entityRenderDispatcher.cameraOrientation());
            AsrielVisualGeometry.comet(AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),6F),
                    e.getVisualAge(partial),e.getTintIndex(),true,VfxDetail.level(e,entityRenderDispatcher,4));
        } else {
            Vector3d origin=new Vector3d(MathHelper.lerp(partial,e.xOld,e.getX()),
                    MathHelper.lerp(partial,e.yOld,e.getY()),MathHelper.lerp(partial,e.zOld,e.getZ()));
            Vector3d camera=entityRenderDispatcher.camera.getPosition(), view=camera.subtract(origin);
            Vector3d[] history=e.getAngelTrail(partial);
            double[][] trail=new double[history.length][3];
            double nearest=camera.distanceToSqr(origin);
            for(int i=0;i<history.length;i++) {
                Vector3d local=history[i].subtract(origin);
                trail[i][0]=local.x; trail[i][1]=local.y; trail[i][2]=local.z;
                nearest=Math.min(nearest,camera.distanceToSqr(history[i]));
            }
            // Chaos Bullets retain their bright pastel colors at every health level.
            AngelOfDeathVisuals.render(AsrielRenderSupport.sink(buffers.getBuffer(AsrielRenderSupport.GLOW),stack),
                    trail,e.getVisualAge(partial),e.getTintIndex(),
                    VfxDetail.levelForDistance(Math.max(0,Math.sqrt(nearest)-AngelOfDeathVisuals.PADDING)),view.x,view.y,view.z);
        }
        stack.popPose();
    }
}
