package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.EnteiBlastEntity;
import net.kazi.kazimod.models.abilities.EnteiVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

public class EnteiBlastRenderer
extends EntityRenderer<EnteiBlastEntity> {
    private static final RenderType SURFACE = States.access$000();

    public EnteiBlastRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public static void fireball(MatrixStack stack, IRenderTypeBuffer buffer, float age, float radius, float opacity) {
        if (radius <= 0.0f || opacity <= 0.0f) {
            return;
        }
        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees(age * 0.45f));
        IVertexBuilder vertices = buffer.getBuffer(SURFACE);
        Matrix4f pose = stack.last().pose();
        EnteiVfxMesh.fireball((x, y, z, color, alpha) -> vertices.vertex(pose, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, MathHelper.clamp((int)((int)(alpha * 255.0f)), (int)0, (int)255)).endVertex(), age, radius, opacity);
        EnteiVfxMesh.corona(KokuVfxRenderer.sink(stack, buffer), age, radius, opacity);
        stack.popPose();
    }

    public void render(EnteiBlastEntity entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        float age = entity.getAge(partial);
        float opacity = entity.getOpacity(partial);
        float radius = entity.getWaveRadius(partial);
        EnteiBlastRenderer.fireball(stack, buffer, age, radius, opacity);
        if (age >= 12.0f) {
            EnteiVfxMesh.shockwave(KokuVfxRenderer.sink(stack, buffer), age, radius, opacity);
        }
    }

    public ResourceLocation getTextureLocation(EnteiBlastEntity entity) {
        return KokuVfxRenderer.UNUSED_TEXTURE;
    }

    private static final class States
    extends RenderState {
        private States() {
            super(null, null, null);
        }

        private static RenderType create() {
            return RenderType.create((String)"kazimod_entei_surface", (VertexFormat)DefaultVertexFormats.POSITION_COLOR, (int)7, (int)131072, (boolean)false, (boolean)false, (RenderType.State)RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).createCompositeState(false));
        }

        static /* synthetic */ RenderType access$000() {
            return States.create();
        }
    }
}
