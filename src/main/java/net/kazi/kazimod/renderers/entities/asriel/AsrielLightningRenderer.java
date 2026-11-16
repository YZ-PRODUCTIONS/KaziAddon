package net.kazi.kazimod.renderers.entities.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.asriel.AsrielLightningEntity;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerDimensions;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

public final class AsrielLightningRenderer extends EntityRenderer<AsrielLightningEntity> {
    private static final ResourceLocation UNUSED=new ResourceLocation("minecraft","textures/block/white_wool.png");
    private static final Vector3d UP=new Vector3d(0,1,0);
    public AsrielLightningRenderer(EntityRendererManager manager){super(manager);}
    @Override public ResourceLocation getTextureLocation(AsrielLightningEntity entity){return UNUSED;}
    @Override public boolean shouldRender(AsrielLightningEntity e,ClippingHelper f,double x,double y,double z) {
        return AsrielRenderSupport.visible(e,f,x,y,z,
                e.isChargeAura()?3*ShockerBreakerDimensions.VISUAL_SCALE:ShockerBreakerDimensions.visualRadius(e.isEnhanced())*1.5,
                e.isChargeAura()?4*ShockerBreakerDimensions.VISUAL_SCALE:ShockerBreakerDimensions.CULL_HEIGHT);
    }
    @Override public void render(AsrielLightningEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        float age=e.getVisualAge(partial);
        if(e.isChargeAura()) {
            stack.pushPose();
            try {
                stack.scale(ShockerBreakerDimensions.VISUAL_SCALE,ShockerBreakerDimensions.VISUAL_SCALE,ShockerBreakerDimensions.VISUAL_SCALE);
                ShockerBreakerVisuals.chargeAura(AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),1F),
                        age,VfxDetail.level(e,entityRenderDispatcher,3*ShockerBreakerDimensions.VISUAL_SCALE));
            } finally { stack.popPose(); }
            return;
        }
        // Only the authoritative strike flag may turn a warning into a damaging bolt.
        age=e.hasStruck()?Math.max(ShockerBreakerVisualTiming.CHARGE_TICKS,age)
                :Math.min(ShockerBreakerVisualTiming.CHARGE_TICKS-.05F,age);
        if(!e.hasStruck()) {
            ShockerBreakerWarningVisuals.ground(AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),2.4F/e.getRadius()),
                    age,e.getRadius());
            stack.pushPose();
            try {
                stack.translate(0,.25,0);
                stack.mulPose(entityRenderDispatcher.cameraOrientation());
                float iconScale=e.getRadius()/4.5F;
                stack.scale(iconScale,iconScale,iconScale);
                ShockerBreakerWarningVisuals.icon(AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),1F),age);
            } finally { stack.popPose(); }
            return;
        }
        stack.pushPose();
        try {
            stack.scale(ShockerBreakerDimensions.VISUAL_SCALE,ShockerBreakerDimensions.VISUAL_SCALE,ShockerBreakerDimensions.VISUAL_SCALE);
            AsrielVisualGeometry.lightning(AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),.30F),
                    age,ShockerBreakerDimensions.originalRadius(e.isEnhanced()),e.isEnhanced(),
                    AsrielRenderSupport.lineDetail(e,entityRenderDispatcher,UP,
                            ShockerBreakerDimensions.VISUAL_HEIGHT,ShockerBreakerDimensions.visualRadius(e.isEnhanced())*1.5));
        } finally { stack.popPose(); }
    }
}
