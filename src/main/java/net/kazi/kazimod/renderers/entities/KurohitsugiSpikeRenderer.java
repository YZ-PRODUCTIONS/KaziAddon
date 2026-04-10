package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.KurohitsugiSpikeEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.PillarModel;

@OnlyIn(Dist.CLIENT)
public class KurohitsugiSpikeRenderer extends EntityRenderer<KurohitsugiSpikeEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/models/kurohitsugi_spike.png");
    private static final float OVERLAY_RED = 0.64F;
    private static final float OVERLAY_GREEN = 0.28F;
    private static final float OVERLAY_BLUE = 1.0F;
    private static final float PILLAR_TAIL_OFFSET = 23.0F / 16.0F;
    private final PillarModel model = new PillarModel();

    public KurohitsugiSpikeRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(KurohitsugiSpikeEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(KurohitsugiSpikeEntity entity, float entityYaw, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        float yaw = MathHelper.lerp(partialTicks, entity.yRotO, entity.yRot);
        float pitch = MathHelper.lerp(partialTicks, entity.xRotO, entity.xRot);
        float scale = entity.getScale();
        float growth = 0.12F + entity.getGrowthProgress() * 0.88F;
        float zScale = scale * 2.6F * growth;
        float tailOffset = PILLAR_TAIL_OFFSET * zScale;

        matrixStack.pushPose();
        matrixStack.mulPose(net.minecraft.util.math.vector.Vector3f.YP.rotationDegrees(-yaw));
        matrixStack.mulPose(net.minecraft.util.math.vector.Vector3f.XP.rotationDegrees(pitch));
        matrixStack.translate(0.0D, 0.0D, -tailOffset);
        matrixStack.scale(scale * 1.2F, scale * 1.2F, zScale);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);
        IVertexBuilder solid = buffer.getBuffer(RenderType.entitySolid(TEXTURE));
        this.model.renderToBuffer(matrixStack, solid, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.pushPose();
        matrixStack.scale(1.1F, 1.1F, 1.08F);
        IVertexBuilder glow = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(matrixStack, glow, packedLight, OverlayTexture.NO_OVERLAY, OVERLAY_RED, OVERLAY_GREEN, OVERLAY_BLUE, 0.5F);
        matrixStack.popPose();

        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    public static class Factory implements net.minecraftforge.fml.client.registry.IRenderFactory<KurohitsugiSpikeEntity> {
        @Override
        public net.minecraft.client.renderer.entity.EntityRenderer<? super KurohitsugiSpikeEntity> createRenderFor(EntityRendererManager manager) {
            return new KurohitsugiSpikeRenderer(manager);
        }
    }
}
