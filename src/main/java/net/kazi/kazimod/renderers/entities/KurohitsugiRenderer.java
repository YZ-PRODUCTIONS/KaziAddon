package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.KurohitsugiEntity;
import net.kazi.kazimod.models.abilities.KurohitsugiModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KurohitsugiRenderer extends EntityRenderer<KurohitsugiEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/models/kurohitsugi_texture.png");
    private static final float OVERLAY_RED = 0.64F;
    private static final float OVERLAY_GREEN = 0.28F;
    private static final float OVERLAY_BLUE = 1.0F;
    private static final float OUTLINE_GROWTH_HEIGHT = 19.6875F;
    private static final float BOX_SCALE_XZ = 6.9F;
    private static final float BOX_SCALE_Y = 7.734375F;
    private final KurohitsugiModel model = new KurohitsugiModel();

    public KurohitsugiRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(KurohitsugiEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(KurohitsugiEntity entity, float entityYaw, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        matrixStack.pushPose();
        matrixStack.scale(BOX_SCALE_XZ, BOX_SCALE_Y, BOX_SCALE_XZ);
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);
        if (entity.isOutlineOnly()) {
            float progress = MathHelper.clamp(entity.getProgress(), 0.0F, 1.0F);
            float heightScale = 0.05F + progress * 0.95F;
            matrixStack.translate(0.0D, OUTLINE_GROWTH_HEIGHT * (1.0F - heightScale), 0.0D);
            matrixStack.scale(1.08F, heightScale, 1.08F);
            IVertexBuilder outline = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            this.model.renderToBuffer(matrixStack, outline, packedLight, OverlayTexture.NO_OVERLAY, OVERLAY_RED, OVERLAY_GREEN, OVERLAY_BLUE, 0.2F + progress * 0.25F);
        } else {
            IVertexBuilder solid = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            this.model.renderToBuffer(matrixStack, solid, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStack.pushPose();
            matrixStack.scale(1.04F, 1.02F, 1.04F);
            IVertexBuilder glow = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            this.model.renderToBuffer(matrixStack, glow, packedLight, OverlayTexture.NO_OVERLAY, OVERLAY_RED, OVERLAY_GREEN, OVERLAY_BLUE, 0.42F);
            matrixStack.popPose();
        }
        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    public static class Factory implements net.minecraftforge.fml.client.registry.IRenderFactory<KurohitsugiEntity> {
        @Override
        public net.minecraft.client.renderer.entity.EntityRenderer<? super KurohitsugiEntity> createRenderFor(EntityRendererManager manager) {
            return new KurohitsugiRenderer(manager);
        }
    }
}
