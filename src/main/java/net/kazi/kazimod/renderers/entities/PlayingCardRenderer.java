package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.kazi.kazimod.entities.projectiles.PlayingCardProjectile;

/**
 * Renders the playing card as a flat horizontal quad.
 * Rotated 90° on X so it lies flat on XZ plane.
 * Uses entitySolid render type with the confirmed texture path.
 */
public class PlayingCardRenderer extends EntityRenderer<PlayingCardProjectile> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/particle/playing_card.png");

    public PlayingCardRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayingCardProjectile entity) {
        return TEXTURE;
    }

    @Override
    public void render(PlayingCardProjectile entity, float entityYaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        stack.pushPose();

        // Face travel direction
        float yaw = entity.yRotO + (entity.yRot - entity.yRotO) * partialTick;
        stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));

        // Lie flat (horizontal)
        stack.mulPose(Vector3f.XP.rotationDegrees(90.0f));

        // Spin in-plane
        float spin = (entity.level.getGameTime() + partialTick) * 10.0f;
        stack.mulPose(Vector3f.ZP.rotationDegrees(spin));

        float hw = 0.45f;
        float hh = 0.30f;

        IVertexBuilder vb = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        MatrixStack.Entry pose = stack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        // Front face
        vb.vertex(m, -hw, -hh, 0).color(1f,1f,1f,1f).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,1).endVertex();
        vb.vertex(m,  hw, -hh, 0).color(1f,1f,1f,1f).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,1).endVertex();
        vb.vertex(m,  hw,  hh, 0).color(1f,1f,1f,1f).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,1).endVertex();
        vb.vertex(m, -hw,  hh, 0).color(1f,1f,1f,1f).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,1).endVertex();

        // Back face
        vb.vertex(m,  hw, -hh, 0).color(1f,1f,1f,1f).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,-1).endVertex();
        vb.vertex(m, -hw, -hh, 0).color(1f,1f,1f,1f).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,-1).endVertex();
        vb.vertex(m, -hw,  hh, 0).color(1f,1f,1f,1f).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,-1).endVertex();
        vb.vertex(m,  hw,  hh, 0).color(1f,1f,1f,1f).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(n,0,0,-1).endVertex();

        stack.popPose();
    }

    public static class Factory implements net.minecraftforge.fml.client.registry.IRenderFactory<PlayingCardProjectile> {
        @Override
        public EntityRenderer<? super PlayingCardProjectile> createRenderFor(EntityRendererManager mgr) {
            return new PlayingCardRenderer(mgr);
        }
    }
}