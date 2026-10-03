package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.GuraVfxEntity;
import net.kazi.kazimod.models.abilities.GuraVfxMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public class GuraVfxRenderer extends EntityRenderer<GuraVfxEntity> {
    public GuraVfxRenderer(EntityRendererManager manager) { super(manager); }
    @Override public void render(GuraVfxEntity e, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        stack.pushPose();
        float age = e.tickCount + partial;
        if (e.getMode() == GuraVfxEntity.GROUND) stack.mulPose(Vector3f.XP.rotationDegrees(90));
        else if (e.getMode() == GuraVfxEntity.ORIENTED_CRACK) {
            stack.mulPose(Vector3f.YP.rotationDegrees(-e.getCrackYaw()));
            stack.mulPose(Vector3f.XP.rotationDegrees(e.getCrackPitch()));
        }
        else stack.mulPose(entityRenderDispatcher.cameraOrientation());
        if (e.getMode() == GuraVfxEntity.CHARGE) {
            GuraVfxMesh.bubble(KokuVfxRenderer.sink(stack, buffer), e.getSize() * Math.min(1, .25F + age / 12), age, 1);
        } else {
            GuraVfxMesh.cracks(KokuVfxRenderer.sink(stack, buffer), e.getSize(), Math.min(1, age / 5),
                    GuraVfxMesh.fade(age, e.getLife()), e.getId());
        }
        stack.popPose();
    }
    @Override public ResourceLocation getTextureLocation(GuraVfxEntity e) { return KokuVfxRenderer.UNUSED_TEXTURE; }
}
