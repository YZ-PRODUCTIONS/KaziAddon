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
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.HydraModel;

@OnlyIn(Dist.CLIENT)
public class MeraFlameDragonRenderer extends EntityRenderer<GoryutenmetsuDragonEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mineminenomi", "textures/models/projectiles/hydra_full.png");
    private final HydraModel<GoryutenmetsuDragonEntity> model = new HydraModel<>();

    public MeraFlameDragonRenderer(EntityRendererManager manager) {
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
        matrixStack.pushPose();
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(entity.getRenderYaw() - 90.0F));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(entity.getRenderPitch()));
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(entity.getRenderRoll()));
        matrixStack.scale(entity.getScale(), entity.getScale(), entity.getScale());

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount, 0.0F, 0.0F);

        IVertexBuilder core = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(matrixStack, core, packedLight, OverlayTexture.NO_OVERLAY,
                entity.getColorRed(), entity.getColorGreen(), entity.getColorBlue(), entity.getAlpha());
        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    public static class Factory implements net.minecraftforge.fml.client.registry.IRenderFactory<GoryutenmetsuDragonEntity> {
        @Override
        public EntityRenderer<? super GoryutenmetsuDragonEntity> createRenderFor(EntityRendererManager manager) {
            return new MeraFlameDragonRenderer(manager);
        }
    }
}
