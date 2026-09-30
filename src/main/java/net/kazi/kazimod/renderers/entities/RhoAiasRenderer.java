package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.RhoAiasEntity;
import net.kazi.kazimod.models.abilities.RhoAiasMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

/** Renders the alternate version's layered, procedural Rho Aias flower. */
public class RhoAiasRenderer extends EntityRenderer<RhoAiasEntity> {
    private static final ResourceLocation ICON =
            new ResourceLocation("kazimod", "textures/abilities/supa_rho_aias.png");
    private static final RenderType GLOW = States.create();

    public RhoAiasRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(RhoAiasEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, 4);
    }

    @Override
    public void render(RhoAiasEntity entity, float entityYaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        float yaw = entity.yRotO + (entity.yRot - entity.yRotO) * partialTick;
        float pitch = entity.xRotO + (entity.xRot - entity.xRotO) * partialTick;
        float progress = Math.min(1.0F, (entity.tickCount + partialTick) / 40.0F);
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, 3);

        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));
        stack.mulPose(Vector3f.XP.rotationDegrees(pitch));
        IVertexBuilder vertices = buffer.getBuffer(GLOW);
        Matrix4f pose = stack.last().pose();
        RhoAiasMesh.render((x, y, z, color, alpha) -> vertices.vertex(pose, x, y, z)
                        .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF,
                                Math.max(0, Math.min(255, (int) (alpha * 255.0F))))
                        .endVertex(),
                progress, entity.tickCount + partialTick, entity.getOpacity(partialTick), detail);
        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RhoAiasEntity entity) {
        return ICON;
    }

    private static final class States extends RenderState {
        private States() {
            super(null, null, null);
        }

        private static RenderType create() {
            return RenderType.create("kazimod_rho_aias", DefaultVertexFormats.POSITION_COLOR,
                    7, 65536, false, false, RenderType.State.builder()
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .createCompositeState(false));
        }
    }
}
