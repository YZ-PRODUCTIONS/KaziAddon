package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.ZushiVfxEntity;
import net.kazi.kazimod.models.abilities.KokuVfxMesh;
import net.kazi.kazimod.models.abilities.ZushiVfxMesh;
import net.kazi.kazimod.renderers.entities.KamaVfxRenderer;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;

public class ZushiVfxRenderer
extends EntityRenderer<ZushiVfxEntity> {
    private static final RenderType ROCK = States.access$000();

    public static KokuVfxMesh.Sink solidSink(MatrixStack stack, IRenderTypeBuffer buffer) {
        IVertexBuilder vertices = buffer.getBuffer(ROCK);
        Matrix4f pose = stack.last().pose();
        return (x, y, z, color, alpha) -> vertices.vertex(pose, x, y, z).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255).endVertex();
    }

    public ZushiVfxRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public void render(ZushiVfxEntity entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        stack.pushPose();
        Entity target = entity.getTarget();
        if ((entity.getMode() == 0 || entity.getMode() == 2) && target != null) {
            stack.translate(MathHelper.lerp((double)partial, (double)target.xOld, (double)target.getX()) - MathHelper.lerp((double)partial, (double)entity.xOld, (double)entity.getX()), MathHelper.lerp((double)partial, (double)target.yOld, (double)target.getY()) - MathHelper.lerp((double)partial, (double)entity.yOld, (double)entity.getY()), MathHelper.lerp((double)partial, (double)target.zOld, (double)target.getZ()) - MathHelper.lerp((double)partial, (double)entity.zOld, (double)entity.getZ()));
        }
        if (entity.getMode() == 0) {
            ZushiVfxMesh.gravityInk(KamaVfxRenderer.slashSink(stack, buffer), (float)entity.tickCount + partial, entity.getSize());
            ZushiVfxMesh.gravityGlow(KokuVfxRenderer.sink(stack, buffer), (float)entity.tickCount + partial, entity.getSize());
        } else if (entity.getMode() == 2) {
            ZushiVfxMesh.graviZoneInk(KamaVfxRenderer.slashSink(stack, buffer), (float)entity.tickCount + partial, entity.getSize());
            ZushiVfxMesh.graviZoneGlow(KokuVfxRenderer.sink(stack, buffer), (float)entity.tickCount + partial, entity.getSize());
        } else {
            ZushiVfxMesh.impact(KokuVfxRenderer.sink(stack, buffer), (float)entity.tickCount + partial, entity.getSize());
        }
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(ZushiVfxEntity entity) {
        return KokuVfxRenderer.UNUSED_TEXTURE;
    }

    private static final class States
    extends RenderState {
        private States() {
            super(null, null, null);
        }

        private static RenderType create() {
            return RenderType.create((String)"kazimod_meteor_rock", (VertexFormat)DefaultVertexFormats.POSITION_COLOR, (int)7, (int)32768, (boolean)false, (boolean)false, (RenderType.State)RenderType.State.builder().setTransparencyState(NO_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).createCompositeState(false));
        }

        static /* synthetic */ RenderType access$000() {
            return States.create();
        }
    }
}
