package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.models.abilities.InfiniteVoidMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public class InfiniteVoidBarrierRenderer
extends EntityRenderer<InfiniteVoidBarrierEntity> {
    public InfiniteVoidBarrierRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public void render(InfiniteVoidBarrierEntity entity, float entityYaw, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        if (!entity.hasVoidVisual()) {
            return;
        }
        float radius = entity.getVisualRadius(partialTicks);
        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees(-entity.yRot));
        Matrix4f pose = stack.last().pose();
        IVertexBuilder vertices = buffer.getBuffer(States.SHELL);
        InfiniteVoidMesh.shell((x, y, z, color, alpha) -> vertices.vertex(pose, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255).endVertex(), radius);
        InfiniteVoidMesh.energy(KokuVfxRenderer.sink(stack, buffer), radius, entity.getVisualAge(partialTicks));
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(InfiniteVoidBarrierEntity entity) {
        return new ResourceLocation("kazimod", "textures/entity/empty.png");
    }

    private static class States
    extends RenderState {
        private static final RenderType SHELL = RenderType.create((String)"kazimod_void_shell", (VertexFormat)DefaultVertexFormats.POSITION_COLOR, (int)7, (int)65536, (boolean)false, (boolean)false, (RenderType.State)RenderType.State.builder().setCullState(NO_CULL).setWriteMaskState(COLOR_DEPTH_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));

        private States() {
            super(null, null, null);
        }
    }
}
