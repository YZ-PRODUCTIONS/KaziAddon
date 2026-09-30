package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.GaeBolgVfxEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public final class GaeBolgVfxRenderer extends EntityRenderer<GaeBolgVfxEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    public GaeBolgVfxRenderer(EntityRendererManager manager) { super(manager); }
    @Override public ResourceLocation getTextureLocation(GaeBolgVfxEntity entity) { return TEXTURE; }
    @Override public boolean shouldRender(GaeBolgVfxEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, entity.isImpact() ? 26 : 7);
    }
    @Override public void render(GaeBolgVfxEntity entity, float yaw, float partial,
                                  MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, entity.isImpact() ? 24 : 5);
        float age = entity.getVisualAge(partial);
        stack.pushPose();
        if (!entity.isImpact()) {
            LivingEntity caster = entity.getCaster();
            if (caster == null) { stack.popPose(); return; }
            // Follow the actual interpolated player, not delayed server movement packets.
            stack.translate(MathHelper.lerp(partial,caster.xOld,caster.getX()) - MathHelper.lerp(partial,entity.xOld,entity.getX()),
                    MathHelper.lerp(partial,caster.yOld,caster.getY()) - MathHelper.lerp(partial,entity.yOld,entity.getY()) + caster.getEyeHeight() + 0.55,
                    MathHelper.lerp(partial,caster.zOld,caster.getZ()) - MathHelper.lerp(partial,entity.zOld,entity.getZ()));
            orient(stack, caster.getLookAngle());
            stack.translate(-0.7, 0, -0.3);
        }
        IVertexBuilder vertices = buffers.getBuffer(CaladbolgVisualGeometry.ENERGY);
        Matrix4f pose = stack.last().pose();
        GaeBolgGeometry.Sink sink = (x,y,z,r,g,b,a) -> vertices.vertex(pose,(float)x,(float)y,(float)z).color(r,g,b,a).endVertex();
        if (entity.isImpact()) {
            GaeBolgGeometry.impact(age,detail,sink);
            // A short incoming afterimage joins the airborne streak to the ground burst.
            stack.pushPose();
            orient(stack, entity.getImpactDirection());
            Matrix4f arrivalPose = stack.last().pose();
            GaeBolgGeometry.arrival(age,detail,(x,y,z,r,g,b,a) ->
                    vertices.vertex(arrivalPose,(float)x,(float)y,(float)z).color(r,g,b,a).endVertex());
            stack.popPose();
        } else GaeBolgGeometry.charge(age,detail,sink);
        stack.popPose();
    }
    static void orient(MatrixStack stack, Vector3d forward) {
        if (forward.lengthSqr() < 0.0001) return;
        forward = forward.normalize();
        stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(forward.x,forward.z))));
        stack.mulPose(Vector3f.XP.rotationDegrees((float)-Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,forward.y))))));
    }
}
