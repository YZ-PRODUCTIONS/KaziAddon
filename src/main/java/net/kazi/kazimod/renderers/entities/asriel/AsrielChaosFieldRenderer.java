package net.kazi.kazimod.renderers.entities.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterPattern;
import net.kazi.kazimod.entities.asriel.AsrielChaosFieldEntity;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public final class AsrielChaosFieldRenderer extends EntityRenderer<AsrielChaosFieldEntity> {
    private static final ResourceLocation UNUSED=new ResourceLocation("minecraft","textures/block/white_wool.png");
    public AsrielChaosFieldRenderer(EntityRendererManager manager){super(manager);}
    @Override public ResourceLocation getTextureLocation(AsrielChaosFieldEntity entity){return UNUSED;}
    @Override public boolean shouldRender(AsrielChaosFieldEntity e,ClippingHelper f,double x,double y,double z) {
        if(!e.shouldRender(x,y,z))return false;
        double visibleLength=e.getKind()==AsrielChaosFieldEntity.SABER?
                e.getLength()*AsrielVisualGeometry.SABER_VISUAL_SCALE:e.getLength();
        double radius=e.getKind()==AsrielChaosFieldEntity.SABER?visibleLength+3:e.getRadius()+3;
        if(e.getKind()==AsrielChaosFieldEntity.BEAM)radius=ChaosBeamVisuals.cullingRadius((float)e.getRadius());
        if(e.getKind()==AsrielChaosFieldEntity.CHARGE)
            radius=Math.max(ChaosCannonVisuals.cullingRadius(),Math.max(radius,e.getLength()*.35+1));
        Vector3d end=e.position().add(e.getAttackDirection().scale(visibleLength));
        return f.isVisible(new AxisAlignedBB(e.position(),end).inflate(radius));
    }
    @Override public void render(AsrielChaosFieldEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        int detail=VfxDetail.level(e,entityRenderDispatcher,
                e.getKind()==AsrielChaosFieldEntity.SABER?
                        e.getLength()*AsrielVisualGeometry.SABER_VISUAL_SCALE:e.getLength());
        if(e.getKind()==AsrielChaosFieldEntity.BEAM)
            detail=AsrielRenderSupport.lineDetail(e,entityRenderDispatcher,e.getAttackDirection(),e.getLength(),
                    ChaosBeamVisuals.cullingRadius((float)e.getRadius()));
        stack.pushPose();
        AsrielRenderSupport.orient(stack,e.getAttackDirection());
        float patternScale=e.getKind()==AsrielChaosFieldEntity.SABER
                ? 2.4F/(float)(e.getLength()*AsrielVisualGeometry.SABER_VISUAL_SCALE)
                : 6F/(float)Math.max(1,e.getLength());
        AsrielVisualGeometry.Sink sink=AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),patternScale);
        float age=e.getVisualAge(partial);
        if(e.getKind()==AsrielChaosFieldEntity.SABER)
            AsrielVisualGeometry.sabers(sink,age,(float)e.getLength(),(float)e.getRadius(),detail);
        else if(e.getKind()==AsrielChaosFieldEntity.CHARGE) {
            int warning=e.isEnhanced()?-1:ChaosBusterPattern.telegraphGroup((int)age);
            if(warning>=0) {
                ChaosBusterPattern.Group group=ChaosBusterPattern.group(warning);
                float progress=(age-group.warningTick)/Math.max(1,group.firstShotTick-group.warningTick);
                AsrielVisualGeometry.busterTelegraph(sink,age,progress,(float)e.getLength(),group.warningAnglesRadians(),detail);
            }
            stack.pushPose();
            if(!e.isEnhanced())stack.mulPose(Vector3f.ZP.rotationDegrees(ChaosBusterPattern.cannonSpinDegrees(age)));
            AsrielVisualGeometry.Sink cannon=AsrielRenderSupport.sink(buffers,stack,e.hasDarkPalette(),e.getVisualAge(partial),.8F);
            AsrielVisualGeometry.busterCharge(cannon,age,ChaosBusterPattern.chargeFraction(age,e.isEnhanced()),
                    (float)e.getRadius(),e.isEnhanced(),detail);
            stack.popPose();
        } else if(e.getKind()==AsrielChaosFieldEntity.BEAM)
            AsrielVisualGeometry.rainbowBeam(sink,age,e.getDuration(),(float)e.getLength(),
                    (float)e.getCurrentRadius(partial),e.isEnhanced(),detail);
        stack.popPose();
    }
}
