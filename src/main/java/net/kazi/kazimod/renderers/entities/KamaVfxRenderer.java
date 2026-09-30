package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.KamaVfxEntity;
import net.kazi.kazimod.models.abilities.KamaVfxMesh;
import net.kazi.kazimod.models.abilities.KokuVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
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

public class KamaVfxRenderer
extends EntityRenderer<KamaVfxEntity> {
    private static final RenderType SLASH = States.access$000();
    private final net.kazi.kazimod.models.abilities.MalevolentShrineModel shrineModel = new net.kazi.kazimod.models.abilities.MalevolentShrineModel();
    private static final ResourceLocation SHRINE_TEXTURE = new ResourceLocation("kazimod", "textures/models/malevolent_shrine.png");

    public static KokuVfxMesh.Sink slashSink(MatrixStack stack, IRenderTypeBuffer buffer) {
        IVertexBuilder vertices = buffer.getBuffer(SLASH);
        Matrix4f pose = stack.last().pose();
        return (x, y, z, color, alpha) -> vertices.vertex(pose, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, MathHelper.clamp((int)((int)(alpha * 255.0f)), (int)0, (int)255)).endVertex();
    }

    public KamaVfxRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public void render(KamaVfxEntity entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        stack.pushPose();
        LivingEntity owner = entity.getOwner();
        if (owner != null) {
            Vector3d p = entity.visualOrigin(owner, partial);
            stack.translate(p.x - MathHelper.lerp((double)partial, (double)entity.xOld, (double)entity.getX()), p.y - MathHelper.lerp((double)partial, (double)entity.yOld, (double)entity.getY()), p.z - MathHelper.lerp((double)partial, (double)entity.zOld, (double)entity.getZ()));
        }
        if (entity.getMode() == KamaVfxEntity.SHRINE_CHARGE) {
            float facing = owner == null ? entity.yRot : MathHelper.rotLerp(partial, owner.yRotO, owner.yRot);
            stack.scale(5.0F, 5.0F, 5.0F);
            stack.mulPose(Vector3f.YP.rotationDegrees(-facing));
            stack.scale(-1.0F, -1.0F, 1.0F);
            stack.translate(0.0D, -1.501D, 0.0D);
            this.shrineModel.setConstruction(entity.getProgress(partial));
            this.shrineModel.renderToBuffer(stack, buffer.getBuffer(this.shrineModel.renderType(SHRINE_TEXTURE)),
                    light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            stack.popPose();
            return;
        }
        if (entity.getMode() == 0 || entity.getMode() == 3) {
            stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        } else if (entity.getMode() != 1) {
            stack.mulPose(Vector3f.YP.rotationDegrees(-(owner == null ? entity.yRot : MathHelper.rotLerp((float)partial, (float)owner.yRotO, (float)owner.yRot))));
            stack.mulPose(Vector3f.XP.rotationDegrees(owner == null ? entity.xRot : MathHelper.lerp((float)partial, (float)owner.xRotO, (float)owner.xRot)));
        }
        if (entity.getMode() == 0) {
            KamaVfxMesh.slash(KamaVfxRenderer.slashSink(stack, buffer), entity.getAge(partial), entity.getLife(), entity.getSize(), entity.getId());
        } else if (entity.getMode() == 3) {
            KamaVfxMesh.dismantle(KamaVfxRenderer.slashSink(stack, buffer), entity.getAge(partial), entity.getLife(), entity.getSize(), entity.getId());
        } else if (entity.getMode() == 1) {
            KamaVfxMesh.web(KamaVfxRenderer.slashSink(stack, buffer), entity.getAge(partial), entity.getLife(), entity.getSize());
        } else {
            KamaVfxMesh.charge(KokuVfxRenderer.sink(stack, buffer), entity.getAge(partial), entity.getProgress(partial));
        }
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(KamaVfxEntity entity) {
        return KokuVfxRenderer.UNUSED_TEXTURE;
    }

    private static final class States
    extends RenderState {
        private States() {
            super(null, null, null);
        }

        private static RenderType create() {
            return RenderType.create((String)"kazimod_kama_slash", (VertexFormat)DefaultVertexFormats.POSITION_COLOR, (int)7, (int)65536, (boolean)false, (boolean)true, (RenderType.State)RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
        }

        static /* synthetic */ RenderType access$000() {
            return States.create();
        }
    }
}
