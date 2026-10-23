package net.kazi.kazimod.renderers.entities.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.entities.projectiles.inucerberus.HellHeadMotorProjectile;
import net.kazi.kazimod.models.zoan.CerberusElbafModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public final class CerberusHeadsRenderer extends EntityRenderer<HellHeadMotorProjectile> {
    public CerberusHeadsRenderer(EntityRendererManager manager) { super(manager); }
    @Override public void render(HellHeadMotorProjectile entity,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffer,int light) {
        Vector3d velocity=entity.getDeltaMovement();
        double horizontal=Math.sqrt(velocity.x*velocity.x+velocity.z*velocity.z);
        stack.pushPose();
        if(velocity.lengthSqr()>.000001) {
            stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(-velocity.x,-velocity.z))));
            stack.mulPose(Vector3f.XP.rotationDegrees((float)Math.toDegrees(Math.atan2(velocity.y,horizontal))));
        } else stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));
        stack.mulPose(Vector3f.ZP.rotationDegrees((entity.tickCount+partial)*18));
        float scale=CerberusElbafModel.FORM_SCALE/16;
        // EntityRenderer has no LivingRenderer axis flip; Blockbench's positive Y is already up.
        stack.scale(scale,scale,scale);
        stack.translate(0,-59,14);
        CerberusElbafModel.renderHeads(stack,buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),light,
                OverlayTexture.NO_OVERLAY,entity.tickCount+partial);
        stack.popPose();
    }
    @Override public boolean shouldRender(HellHeadMotorProjectile entity,ClippingHelper frustum,double x,double y,double z) {
        return frustum.isVisible(entity.getBoundingBox().inflate(3));
    }
    @Override public ResourceLocation getTextureLocation(HellHeadMotorProjectile entity) { return CerberusElbafModel.TEXTURE; }
}
