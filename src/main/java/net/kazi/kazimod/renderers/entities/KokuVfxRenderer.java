package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.KokuVfxEntity;
import net.kazi.kazimod.models.abilities.KokuVfxMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class KokuVfxRenderer
extends EntityRenderer<KokuVfxEntity> {
    public static final ResourceLocation UNUSED_TEXTURE = new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    private static final RenderType ENERGY = States.access$000();

    public static KokuVfxMesh.Sink sink(MatrixStack stack, IRenderTypeBuffer buffer) {
        IVertexBuilder vertices = buffer.getBuffer(ENERGY);
        Matrix4f pose = stack.last().pose();
        return (x, y, z, color, alpha) -> vertices.vertex(pose, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, MathHelper.clamp((int)((int)(alpha * 255.0f)), (int)0, (int)255)).endVertex();
    }

    public KokuVfxRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.0f;
    }

    private static double visualExtent(KokuVfxEntity entity) {
        return entity.getMode() == 3 || entity.getMode() == 4 || KokuVfxEntity.isNukeMode(entity.getMode())
                ? Math.max(8.0D, entity.getSize() * 2.0D + Math.abs(entity.getGroundOffset())) : 8.0D;
    }

    @Override
    public boolean shouldRender(KokuVfxEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, visualExtent(entity));
    }

    public void render(KokuVfxEntity entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        stack.pushPose();
        LivingEntity owner = entity.getOwner();
        if (owner != null) {
            Vector3d origin = KokuVfxEntity.castOrigin(owner, partial, entity.getMode());
            stack.translate(origin.x - MathHelper.lerp((double)partial, (double)entity.xOld, (double)entity.getX()), origin.y - MathHelper.lerp((double)partial, (double)entity.yOld, (double)entity.getY()), origin.z - MathHelper.lerp((double)partial, (double)entity.zOld, (double)entity.getZ()));
            stack.mulPose(Vector3f.YP.rotationDegrees(-MathHelper.rotLerp((float)partial, (float)owner.yRotO, (float)owner.yRot)));
            stack.mulPose(Vector3f.XP.rotationDegrees(MathHelper.lerp((float)partial, (float)owner.xRotO, (float)owner.xRot)));
        }
        KokuVfxMesh.Sink out = KokuVfxMesh.withDetail(sink(stack, buffer),
                VfxDetail.level(entity, this.entityRenderDispatcher, visualExtent(entity)));
        if (entity.getMode() == KokuVfxEntity.RED_NUKE_IMPACT) {
            KokuVfxMesh.redNuke(out, entity.getNukeAge(partial), entity.getWaveRadius(partial), entity.getVfxOpacity(partial), entity.getGroundOffset());
        } else if (entity.getMode() == KokuVfxEntity.NUKE_IMPACT) {
            KokuVfxMesh.nuke(out, entity.getNukeAge(partial), entity.getWaveRadius(partial), entity.getVfxOpacity(partial), entity.getGroundOffset());
        } else if (entity.getMode() == 3 || entity.getMode() == 4) {
            KokuVfxMesh.impact(out, entity.getMode() == 3 ? 0 : 2, entity.getProgress(partial), entity.getAge(partial), entity.getSize());
        } else {
            KokuVfxMesh.charge(out, entity.getMode(), entity.getProgress(partial), entity.getAge(partial));
        }
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(KokuVfxEntity entity) {
        return UNUSED_TEXTURE;
    }

    private static final class States
    extends RenderState {
        private States() {
            super(null, null, null);
        }

        private static RenderType create() {
            return RenderType.create((String)"kazimod_koku_energy", (VertexFormat)DefaultVertexFormats.POSITION_COLOR, (int)7, (int)262144, (boolean)false, (boolean)false, (RenderType.State)RenderType.State.builder().setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
        }

        static /* synthetic */ RenderType access$000() {
            return States.create();
        }
    }
}
