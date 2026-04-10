package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.GoryutenmetsuDragonEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.OnibiModel;

@OnlyIn(Dist.CLIENT)
public class GoryutenmetsuDragonRenderer extends EntityRenderer<GoryutenmetsuDragonEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    private final OnibiModel<GoryutenmetsuDragonEntity> model = new OnibiModel<>();

    public GoryutenmetsuDragonRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(GoryutenmetsuDragonEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GoryutenmetsuDragonEntity entity, float entityYaw, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        float yaw = MathHelper.lerp(partialTicks, entity.yRotO, entity.getRenderYaw());
        float pitch = MathHelper.lerp(partialTicks, entity.xRotO, entity.getRenderPitch());
        float roll = entity.getRenderRoll();
        float alpha = entity.getAlpha();
        float scale = entity.getScale();

        matrixStack.pushPose();
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(yaw + 180.0F));
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(pitch));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(roll));
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.translate(0.0D, -1.1D, 0.0D);
        matrixStack.scale(scale, scale, scale);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        IVertexBuilder core = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(matrixStack, core, packedLight, OverlayTexture.NO_OVERLAY,
                entity.getColorRed(), entity.getColorGreen(), entity.getColorBlue(), alpha);

        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    public static class Factory implements net.minecraftforge.fml.client.registry.IRenderFactory<GoryutenmetsuDragonEntity> {
        @Override
        public EntityRenderer<? super GoryutenmetsuDragonEntity> createRenderFor(EntityRendererManager manager) {
            return new GoryutenmetsuDragonRenderer(manager);
        }
    }
}
